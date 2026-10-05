package vn.gov.tax.dataplatform.source.service;

import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;
import vn.gov.tax.dataplatform.source.domain.Source;
import vn.gov.tax.dataplatform.source.domain.SourceConnection;
import vn.gov.tax.dataplatform.source.domain.SourceCredential;
import vn.gov.tax.dataplatform.source.dto.ConnectionTestRequest;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.dto.SourceMetadataRequest;
import vn.gov.tax.dataplatform.source.dto.SourceRequest;
import vn.gov.tax.dataplatform.source.dto.SourceResponse;
import vn.gov.tax.dataplatform.source.mapper.SourceMapper;
import vn.gov.tax.dataplatform.source.repository.SourceCredentialRepository;
import vn.gov.tax.dataplatform.source.repository.SourceRepository;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTester;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTesterRegistry;
import vn.gov.tax.dataplatform.pipeline.repository.PipelineRepository;
import vn.gov.tax.dataplatform.execution.domain.ExecutionStatus;
import vn.gov.tax.dataplatform.execution.repository.ExecutionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SourceService {
  private final SourceRepository repository;
  private final SourceMapper mapper;
  private final SourceCredentialRepository credentialRepository;
  private final CredentialEncryptionService encryptionService;
  private final DatabaseConnectionTesterRegistry connectionTesters;
  private final PipelineRepository pipelineRepository;
  private final ExecutionRepository executionRepository;

  public Page<SourceResponse> list(
      String tenant, String name, DatabaseType type, Source.Status status, Pageable pageable) {
    String searchName = name == null || name.isBlank() ? null : name.trim();
    return repository
        .searchByTenantAndFilters(
            tenant,
            type,
            status == null ? Source.Status.ACTIVE : status,
            searchName,
            pageable)
        .map(mapper::toResponse);
  }

  public SourceResponse get(String tenant, UUID id) {
    return mapper.toResponse(findSourceById(tenant, id));
  }

  public DatabaseConnectionTester.ConnectionTestResult testConnection(ConnectionTestRequest request) {
    requirePassword(request.connection().password());
    return connectionTesters.test(request.type(), request.connection());
  }

  public List<DatabaseConnectionTester.DiscoveredTable> discoverTables(String tenant, UUID id) {
    Source source = findActiveSource(tenant, id);
    return connectionTesters.discoverTables(source.getType(), connectionRequest(source, tenant));
  }

  public List<DatabaseConnectionTester.DiscoveredColumn> discoverColumns(
      String tenant, UUID id, String tableName) {
    Source source = findActiveSource(tenant, id);
    return connectionTesters.discoverColumns(
        source.getType(), connectionRequest(source, tenant), tableName);
  }

  public DatabaseConnectionRequest connectionForWorker(String tenant, UUID id) {
    Source source = findActiveSource(tenant, id);
    return connectionRequest(source, tenant);
  }

  private DatabaseConnectionRequest connectionRequest(Source source, String tenant) {
    SourceConnection profile = source.getConnection();
    String password = encryptionService.decrypt(findCredential(source, tenant).getEncryptedPassword());
    return new DatabaseConnectionRequest(
        profile.getHost(), profile.getPort(), profile.getDatabaseName(), profile.getSchemaName(),
        profile.getUsername(), password, profile.isEncrypt(), profile.isTrustServerCertificate());
  }

  @Transactional
  public SourceResponse create(String tenant, String actor, SourceRequest request) {
    SourceMetadataRequest metadata = request.metadata();
    DatabaseConnectionRequest connection = request.connection();
    if (repository.existsByTenantIdAndName(tenant, metadata.name())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Source name already exists");
    }
    requirePassword(connection.password());
    requireConnection(metadata.type(), connection, connection.password());

    Source source = mapper.toEntity(tenant, request);
    source.setCreatedBy(actor);
    source.setUpdatedBy(actor);
    source = repository.save(source);
    credentialRepository.save(new SourceCredential(
        source.getConnection().getSecretRef(), tenant, encryptionService.encrypt(connection.password())));
    return mapper.toResponse(source);
  }

  @Transactional
  public SourceResponse update(String tenant, String actor, UUID id, SourceRequest request) {
    Source source = findActiveSource(tenant, id);
    SourceMetadataRequest metadata = request.metadata();
    DatabaseConnectionRequest connection = request.connection();
    if (repository.existsByTenantIdAndNameAndIdNot(tenant, metadata.name(), id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Source name already exists");
    }

    boolean passwordChanged = hasText(connection.password());
    boolean databaseTypeChanged = source.getType() != metadata.type();
    boolean connectionChanged = connectionChanged(source.getConnection(), connection);
    String password = passwordChanged
        ? connection.password()
        : connectionChanged ? loadPassword(source, tenant) : null;
    if (passwordChanged || connectionChanged || databaseTypeChanged) {
      requireConnection(metadata.type(), connection, password);
    }

    mapper.update(source, request);
    source.setUpdatedBy(actor);
    Source updated = repository.save(source);
    if (passwordChanged) {
      SourceCredential credential = findCredential(source, tenant);
      credential.setEncryptedPassword(encryptionService.encrypt(password));
      credentialRepository.save(credential);
    }
    return mapper.toResponse(updated);
  }

  @Transactional
  public SourceResponse delete(String tenant, String actor, UUID id) {
    Source source = findActiveSource(tenant, id);
    List<UUID> pipelineIds = pipelineRepository.findAllByTenantIdAndSourceId(tenant, id).stream()
        .map(vn.gov.tax.dataplatform.pipeline.domain.Pipeline::getId)
        .toList();
    if (!pipelineIds.isEmpty() && executionRepository.existsByTenantIdAndPipelineIdInAndStatusIn(
        tenant,
        pipelineIds,
        List.of(ExecutionStatus.SUBMITTING, ExecutionStatus.SUBMISSION_UNKNOWN,
            ExecutionStatus.QUEUED, ExecutionStatus.RUNNING))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Source is used by an active execution and cannot be disabled yet");
    }

    source.setStatus(Source.Status.DISABLED);
    source.setDeletedAt(java.time.Instant.now());
    source.setDeletedBy(actor);
    source.setUpdatedBy(actor);
    Source disabled = repository.save(source);
    credentialRepository.findBySecretRefAndTenantId(source.getConnection().getSecretRef(), tenant)
        .ifPresent(credentialRepository::delete);
    return mapper.toResponse(disabled);
  }

  private Source findSourceById(String tenant, UUID id) {
    return repository.findByTenantIdAndId(tenant, id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found"));
  }

  private Source findActiveSource(String tenant, UUID id) {
    Source source = findSourceById(tenant, id);
    if (source.getStatus() != Source.Status.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found");
    }
    return source;
  }

  private String loadPassword(Source source, String tenant) {
    return encryptionService.decrypt(findCredential(source, tenant).getEncryptedPassword());
  }

  private SourceCredential findCredential(Source source, String tenant) {
    return credentialRepository.findBySecretRefAndTenantId(source.getConnection().getSecretRef(), tenant)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.CONFLICT, "Source credentials are unavailable; enter the password again"));
  }

  private void requireConnection(DatabaseType type, DatabaseConnectionRequest request, String password) {
    DatabaseConnectionRequest requestWithPassword = new DatabaseConnectionRequest(
        request.host(), request.port(), request.databaseName(), request.schemaName(), request.username(), password,
        request.encrypt(), request.trustServerCertificate());
    DatabaseConnectionTester.ConnectionTestResult result = connectionTesters.test(type, requestWithPassword);
    if (!result.success()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, result.message());
    }
  }

  private static boolean connectionChanged(SourceConnection current, DatabaseConnectionRequest request) {
    return !current.getHost().equals(request.host())
        || current.getPort() != request.port()
        || !current.getDatabaseName().equals(request.databaseName())
        || !java.util.Objects.equals(current.getSchemaName(), request.schemaName())
        || !current.getUsername().equals(request.username())
        || current.isEncrypt() != request.useEncryption()
        || current.isTrustServerCertificate() != request.useTrustServerCertificate();
  }

  private static void requirePassword(String password) {
    if (!hasText(password)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for connection testing");
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

}
