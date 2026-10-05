package vn.gov.tax.dataplatform.compiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineDefinition;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;

class ExecutionPlanCompilerTest {
  private final ExecutionPlanCompiler compiler = new ExecutionPlanCompiler();

  @Test
  void compilesVersionOnePipelineDefinition() {
    var result = compiler.compile(
      UUID.randomUUID(), UUID.randomUUID(), DatabaseType.MSSQL,
      definition(1, "orders"), sourceColumns());

    assertTrue(result.valid());
    assertTrue(result.errors().isEmpty());
    assertTrue(result.plan() != null);
    assertTrue(result.plan().sourceType() == DatabaseType.MSSQL);
  }

  @Test
  void rejectsUnsupportedPlanVersionAndUnsafeIdentifiers() {
    var result = compiler.compile(
      UUID.randomUUID(), UUID.randomUUID(), DatabaseType.MSSQL,
      definition(2, "orders;DROP TABLE users"), sourceColumns());

    assertFalse(result.valid());
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("schema version")));
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("sourceTable")));
  }

  @Test
  void rejectsColumnsMissingFromTheSourceSchema() {
    var result = compiler.compile(
        UUID.randomUUID(), UUID.randomUUID(), DatabaseType.MSSQL,
        definition(1, "dbo.orders"), Set.of("order_id"));

    assertFalse(result.valid());
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("watermarkColumn")));
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("cleaningRules.columns")));
  }

  private static Set<String> sourceColumns() {
    return Set.of("updated_at", "order_id", "customer_name");
  }

  private static PipelineDefinition definition(int version, String sourceTable) {
    return new PipelineDefinition(
        version,
        sourceTable,
        "updated_at",
        List.of("order_id"),
        "bronze.orders",
        "silver.orders",
        "gold.orders",
        "analytics.orders",
        List.of(new PipelineDefinition.CleaningRule(
            PipelineDefinition.CleaningOperation.TRIM, List.of("customer_name"))),
        List.of(new PipelineDefinition.DqRule(
            "order-id-required", "order_id", PipelineDefinition.DqRuleType.NOT_NULL, null)));
  }
}