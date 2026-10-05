package vn.gov.tax.dataplatform.pipeline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PipelineRequest(
    @NotBlank @Size(max = 160) String name,
    @NotNull UUID sourceId,
    @NotNull @Valid PipelineDefinition definition) {}