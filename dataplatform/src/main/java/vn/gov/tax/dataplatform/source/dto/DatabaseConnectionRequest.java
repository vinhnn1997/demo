package vn.gov.tax.dataplatform.source.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatabaseConnectionRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._:-]+$") @Size(max = 255) String host,
        @Min(1) @Max(65535) int port,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_$-]+$") @Size(max = 128) String databaseName,
        @Pattern(regexp = "^(?:[A-Za-z0-9_$-]+)?$") @Size(max = 128) String schemaName,
        @NotBlank @Size(max = 256) String username,
        @Size(max = 500) String password,
        Boolean encrypt,
        Boolean trustServerCertificate) {
    public boolean useEncryption() {
        return encrypt == null || encrypt;
    }

    public boolean useTrustServerCertificate() {
        return trustServerCertificate != null && trustServerCertificate;
    }
}
