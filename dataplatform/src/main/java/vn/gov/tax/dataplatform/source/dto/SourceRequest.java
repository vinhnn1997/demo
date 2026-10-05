package vn.gov.tax.dataplatform.source.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SourceRequest(
        @NotNull @Valid SourceMetadataRequest metadata,
        @NotNull @Valid DatabaseConnectionRequest connection) {
}
