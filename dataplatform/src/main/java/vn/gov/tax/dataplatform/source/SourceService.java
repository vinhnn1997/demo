package vn.gov.tax.dataplatform.source;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTester;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTesterRegistry;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SourceService {
  private final SourceRepository repository;
  private final SourceMapper mapper;
  private final SourceCredentialRepository credentialRepository;
  private final CredentialEncryptionService encryptionService;
  private final DatabaseConnectionTesterRegistry connectionTesters;

  public Page<SourceResponse> list(String tenant, Pageable pageable) {
    return repository
        .findAllByTenantIdAndStatusOrderByNameAsc(tenant, Source.Status.ACTIVE, pageable)
        .map(mapper::toResponse);
  }

  public SourceResponse get(String tenant, UUID id) {
    return mapper.toResponse(findSource(tenant, id));
  }

  public DatabaseConnectionTester.ConnectionTestResult testConnection(ConnectionTestRequest request) {
    requirePassword(request.connection().password());
    return connectionTesters.test(request.type(), request.connection());
  }

  @Transactional
  public SourceResponse create(String tenant, SourceRequest request) {
    SourceMetadataRequest metadata = request.metadata();
    DatabaseConnectionRequest connection = request.connection();
    if (repository.existsByTenantIdAndName(tenant, metadata.name())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Source name already exists");
    }
    requirePassword(connection.password());
    requireConnection(metadata.type(), connection, connection.password());

    Source source = repository.save(mapper.toEntity(tenant, request));
    credentialRepository.save(new SourceCredential(
        source.getConnection().getSecretRef(), tenant, encryptionService.encrypt(connection.password())));
    return mapper.toResponse(source);
  }

  @Transactional
  public SourceResponse update(String tenant, UUID id, SourceRequest request) {
    Source source = findSource(tenant, id);
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
    Source updated = repository.save(source);
    if (passwordChanged) {
      SourceCredential credential = findCredential(source, tenant);
      credential.setEncryptedPassword(encryptionService.encrypt(password));
      credentialRepository.save(credential);
    }
    return mapper.toResponse(updated);
  }

  @Transactional
  public void delete(String tenant, UUID id) {
    Source source = findSource(tenant, id);
    credentialRepository.delete(findCredential(source, tenant));
    repository.delete(source);
  }

  private Source findSource(String tenant, UUID id) {
    return repository.findByTenantIdAndId(tenant, id)
        .filter(source -> source.getStatus() == Source.Status.ACTIVE)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found"));
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
