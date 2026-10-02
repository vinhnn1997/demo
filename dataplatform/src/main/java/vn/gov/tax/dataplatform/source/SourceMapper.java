package vn.gov.tax.dataplatform.source;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SourceMapper {
  public Source toEntity(String tenantId, SourceRequest request) {
    SourceMetadataRequest metadata = request.metadata();
    DatabaseConnectionRequest connectionRequest = request.connection();
    Source source = new Source();
    source.setTenantId(tenantId);
    source.setName(metadata.name());
    source.setDescription(metadata.description());
    source.setType(metadata.type());
    source.setConnection(toConnection(source, connectionRequest));
    return source;
  }

  public void update(Source source, SourceRequest request) {
    SourceMetadataRequest metadata = request.metadata();
    source.setName(metadata.name());
    source.setType(metadata.type());
    source.setDescription(metadata.description());
    updateConnection(source.getConnection(), request.connection());
  }

  public SourceResponse toResponse(Source source) {
    SourceConnection connection = source.getConnection();
    return new SourceResponse(
        source.getId(),
        new SourceResponse.Metadata(source.getName(), source.getType(), source.getDescription()),
        new SourceResponse.Connection(
            connection.getHost(),
            connection.getPort(),
            connection.getDatabaseName(),
            connection.getSchemaName(),
            connection.getUsername(),
            connection.isEncrypt(),
            connection.isTrustServerCertificate()),
        source.getStatus(),
        source.getCreatedAt(),
        source.getUpdatedAt());
  }

  private SourceConnection toConnection(Source source, DatabaseConnectionRequest request) {
    SourceConnection connection = new SourceConnection();
    connection.setSource(source);
    connection.setSecretRef(UUID.randomUUID().toString());
    updateConnection(connection, request);
    return connection;
  }

  private void updateConnection(SourceConnection connection, DatabaseConnectionRequest request) {
    connection.setHost(request.host());
    connection.setPort(request.port());
    connection.setDatabaseName(request.databaseName());
    connection.setSchemaName(request.schemaName());
    connection.setUsername(request.username());
    connection.setEncrypt(request.useEncryption());
    connection.setTrustServerCertificate(request.useTrustServerCertificate());
  }
}
