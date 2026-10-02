package vn.gov.tax.dataplatform.source;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ConnectionTestRequest(
        @NotNull DatabaseType type,
        @NotNull @Valid DatabaseConnectionRequest connection) {
}
