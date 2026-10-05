package vn.gov.tax.dataplatform.source.dto;

import java.time.Instant;
import java.util.UUID;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;
import vn.gov.tax.dataplatform.source.domain.Source;

public record SourceResponse(
        UUID id,
        Metadata metadata,
        Connection connection,
        Source.Status status,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        String deletedBy) {
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
