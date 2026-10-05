package vn.gov.tax.dataplatform.compiler.model;

import java.util.UUID;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineDefinition;

public record ExecutionPlan(
    int schemaVersion,
    UUID pipelineId,
    UUID sourceId,
    PipelineDefinition definition) {}