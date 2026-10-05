package vn.gov.tax.dataplatform.source.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;

public record ConnectionTestRequest(
        @NotNull DatabaseType type,
        @NotNull @Valid DatabaseConnectionRequest connection) {
}
