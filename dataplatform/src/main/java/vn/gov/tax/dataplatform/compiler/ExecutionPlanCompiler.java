package vn.gov.tax.dataplatform.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import vn.gov.tax.dataplatform.compiler.model.ExecutionPlan;
import vn.gov.tax.dataplatform.compiler.model.PlanValidationResult;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineDefinition;

@Component
public class ExecutionPlanCompiler {
  private static final int SUPPORTED_SCHEMA_VERSION = 1;
  private static final Pattern IDENTIFIER =
      Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*)?");

  public PlanValidationResult compile(
      UUID pipelineId, UUID sourceId, PipelineDefinition definition, Set<String> sourceColumns) {
    List<String> errors = new ArrayList<>();
    if (definition.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
      errors.add("Unsupported execution-plan schema version; expected 1");
    }
    requireIdentifier("sourceTable", definition.sourceTable(), errors);
    requireIdentifier("watermarkColumn", definition.watermarkColumn(), errors);
    requireIdentifier("bronzeTable", definition.bronzeTable(), errors);
    requireIdentifier("silverTable", definition.silverTable(), errors);
    requireIdentifier("goldTable", definition.goldTable(), errors);
    requireIdentifier("clickHouseTable", definition.clickHouseTable(), errors);
    if (sourceColumns.isEmpty()) {
      errors.add("Source table was not found or has no readable columns");
    }
    requireSourceColumn("watermarkColumn", definition.watermarkColumn(), sourceColumns, errors);
    for (String keyColumn : definition.keyColumns()) {
      requireIdentifier("keyColumns", keyColumn, errors);
      requireSourceColumn("keyColumns", keyColumn, sourceColumns, errors);
    }
    for (PipelineDefinition.CleaningRule rule : definition.cleaningRules()) {
      for (String column : rule.columns()) {
        requireIdentifier("cleaningRules.columns", column, errors);
        requireSourceColumn("cleaningRules.columns", column, sourceColumns, errors);
      }
    }
    for (PipelineDefinition.DqRule rule : definition.dqRules()) {
      requireIdentifier("dqRules.column", rule.column(), errors);
      requireSourceColumn("dqRules.column", rule.column(), sourceColumns, errors);
      if (requiresValue(rule.type()) && (rule.value() == null || rule.value().isBlank())) {
        errors.add("DQ rule '" + rule.name() + "' requires a value");
      }
      if (rule.type() == PipelineDefinition.DqRuleType.REGEX && hasText(rule.value())) {
        try {
          Pattern.compile(rule.value());
        } catch (java.util.regex.PatternSyntaxException exception) {
          errors.add("DQ rule '" + rule.name() + "' contains an invalid regular expression");
        }
      }
      if ((rule.type() == PipelineDefinition.DqRuleType.MIN
              || rule.type() == PipelineDefinition.DqRuleType.MAX)
          && hasText(rule.value())) {
        try {
          new java.math.BigDecimal(rule.value());
        } catch (NumberFormatException exception) {
          errors.add("DQ rule '" + rule.name() + "' requires a numeric value");
        }
      }
    }
    if (!errors.isEmpty()) {
      return new PlanValidationResult(false, errors, null);
    }
    ExecutionPlan plan = new ExecutionPlan(SUPPORTED_SCHEMA_VERSION, pipelineId, sourceId, definition);
    return new PlanValidationResult(true, List.of(), plan);
  }

  private static void requireIdentifier(String field, String value, List<String> errors) {
    if (!hasText(value) || !IDENTIFIER.matcher(value).matches()) {
      errors.add(field + " contains an invalid identifier");
    }
  }

  private static void requireSourceColumn(
      String field, String value, Set<String> sourceColumns, List<String> errors) {
    if (hasText(value) && sourceColumns.stream().noneMatch(value::equalsIgnoreCase)) {
      errors.add(field + " refers to a column that does not exist in the source table");
    }
  }

  private static boolean requiresValue(PipelineDefinition.DqRuleType type) {
    return type == PipelineDefinition.DqRuleType.REGEX
        || type == PipelineDefinition.DqRuleType.MIN
        || type == PipelineDefinition.DqRuleType.MAX;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}