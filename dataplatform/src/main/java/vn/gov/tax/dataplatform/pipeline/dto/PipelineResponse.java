package vn.gov.tax.dataplatform.pipeline.dto;

import java.time.Instant;
import java.util.UUID;

public record PipelineResponse(
    UUID id,
    String name,
    UUID sourceId,
    PipelineDefinition definition,
    Instant createdAt,
    Instant updatedAt) {}