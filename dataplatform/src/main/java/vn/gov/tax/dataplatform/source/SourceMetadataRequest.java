package vn.gov.tax.dataplatform.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SourceMetadataRequest(
        @NotNull DatabaseType type,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description) {
}
