package vn.gov.tax.dataplatform.compiler.model;

import java.util.UUID;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineDefinition;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;

public record ExecutionPlan(
    int schemaVersion,
    UUID pipelineId,
    UUID sourceId,
    DatabaseType sourceType,
    PipelineDefinition definition) {}