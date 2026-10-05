package vn.gov.tax.dataplatform.source.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.gov.tax.dataplatform.execution.domain.ExecutionStatus;
import vn.gov.tax.dataplatform.execution.repository.ExecutionRepository;
import vn.gov.tax.dataplatform.pipeline.domain.Pipeline;
import vn.gov.tax.dataplatform.pipeline.repository.PipelineRepository;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTesterRegistry;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;
import vn.gov.tax.dataplatform.source.domain.Source;
import vn.gov.tax.dataplatform.source.domain.SourceConnection;
import vn.gov.tax.dataplatform.source.domain.SourceCredential;
import vn.gov.tax.dataplatform.source.mapper.SourceMapper;
import vn.gov.tax.dataplatform.source.repository.SourceCredentialRepository;
import vn.gov.tax.dataplatform.source.repository.SourceRepository;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {
  private static final String TENANT_ID = "tenant-1";
  private static final String ACTOR_ID = "user-1";

  @Mock private SourceRepository sourceRepository;
  @Mock private SourceCredentialRepository credentialRepository;
  @Mock private CredentialEncryptionService encryptionService;
  @Mock private DatabaseConnectionTesterRegistry connectionTesters;
  @Mock private PipelineRepository pipelineRepository;
  @Mock private ExecutionRepository executionRepository;

  private SourceService service;

  @BeforeEach
  void setUp() {
    service = new SourceService(
        sourceRepository,
        new SourceMapper(),
        credentialRepository,
        encryptionService,
        connectionTesters,
        pipelineRepository,
        executionRepository);
  }

  @Test
  void refusesToDisableSourceWhilePipelineExecutionIsActive() {
    Source source = source();
    Pipeline pipeline = pipeline(source.getId());
    when(sourceRepository.findByTenantIdAndId(TENANT_ID, source.getId())).thenReturn(Optional.of(source));
    when(pipelineRepository.findAllByTenantIdAndSourceId(TENANT_ID, source.getId()))
        .thenReturn(List.of(pipeline));
    when(executionRepository.existsByTenantIdAndPipelineIdInAndStatusIn(
        TENANT_ID, List.of(pipeline.getId()), List.of(
            ExecutionStatus.SUBMITTING,
            ExecutionStatus.SUBMISSION_UNKNOWN,
            ExecutionStatus.QUEUED,
            ExecutionStatus.RUNNING)))
        .thenReturn(true);

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> service.delete(TENANT_ID, ACTOR_ID, source.getId()));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals(Source.Status.ACTIVE, source.getStatus());
    verify(sourceRepository, never()).save(any());
    verify(credentialRepository, never()).delete(any(SourceCredential.class));
  }

  @Test
  void softDisablesSourceAndRevokesCredentialAfterExecutionsFinish() {
    Source source = source();
    Pipeline pipeline = pipeline(source.getId());
    SourceCredential credential = new SourceCredential("secret-1", TENANT_ID, "encrypted");
    when(sourceRepository.findByTenantIdAndId(TENANT_ID, source.getId())).thenReturn(Optional.of(source));
    when(pipelineRepository.findAllByTenantIdAndSourceId(TENANT_ID, source.getId()))
        .thenReturn(List.of(pipeline));
    when(executionRepository.existsByTenantIdAndPipelineIdInAndStatusIn(
      TENANT_ID,
      List.of(pipeline.getId()),
      List.of(ExecutionStatus.SUBMITTING, ExecutionStatus.SUBMISSION_UNKNOWN,
        ExecutionStatus.QUEUED, ExecutionStatus.RUNNING)))
        .thenReturn(false);
    when(sourceRepository.save(any(Source.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(credentialRepository.findBySecretRefAndTenantId("secret-1", TENANT_ID))
        .thenReturn(Optional.of(credential));

    var response = service.delete(TENANT_ID, ACTOR_ID, source.getId());

    assertEquals(Source.Status.DISABLED, response.status());
    assertEquals(ACTOR_ID, response.deletedBy());
    assertNotNull(response.deletedAt());
    assertEquals(ACTOR_ID, response.updatedBy());
    verify(credentialRepository).delete(credential);
  }

  private static Source source() {
    Source source = new Source();
    source.setId(UUID.randomUUID());
    source.setTenantId(TENANT_ID);
    source.setName("legacy-ins");
    source.setType(DatabaseType.MSSQL);
    source.setCreatedBy(ACTOR_ID);
    source.setUpdatedBy(ACTOR_ID);

    SourceConnection connection = new SourceConnection();
    connection.setSource(source);
    connection.setSecretRef("secret-1");
    connection.setHost("sql.example.internal");
    connection.setPort(1433);
    connection.setDatabaseName("tax");
    connection.setUsername("reader");
    connection.setEncrypt(true);
    source.setConnection(connection);
    return source;
  }

  private static Pipeline pipeline(UUID sourceId) {
    Pipeline pipeline = new Pipeline();
    pipeline.setId(UUID.randomUUID());
    pipeline.setTenantId(TENANT_ID);
    pipeline.setSourceId(sourceId);
    return pipeline;
  }
}