package vn.gov.tax.dataplatform.source.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;
import jakarta.validation.constraints.Size;

public record SourceMetadataRequest(
        @NotNull DatabaseType type,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description) {
}
