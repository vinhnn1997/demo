package vn.gov.tax.dataplatform.source;

import java.time.Instant;
import java.util.UUID;

public record SourceResponse(
        UUID id,
        Metadata metadata,
        Connection connection,
        Source.Status status,
        Instant createdAt,
        Instant updatedAt) {
        public record Metadata(String name, DatabaseType type, String description) {
    }

    public record Connection(
            String host,
            int port,
            String databaseName,
            String schemaName,
            String username,
            boolean encrypt,
            boolean trustServerCertificate) {
    }
}
