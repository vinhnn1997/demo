package vn.gov.tax.dataplatform.pipeline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PipelineDefinition(
    @Min(1) int schemaVersion,
    @NotBlank @Size(max = 256) String sourceTable,
    @NotBlank @Size(max = 128) String watermarkColumn,
    @NotEmpty List<@NotBlank @Size(max = 128) String> keyColumns,
    @NotBlank @Size(max = 256) String bronzeTable,
    @NotBlank @Size(max = 256) String silverTable,
    @NotBlank @Size(max = 256) String goldTable,
    @NotBlank @Size(max = 256) String clickHouseTable,
    @NotNull List<@Valid CleaningRule> cleaningRules,
    @NotEmpty List<@Valid DqRule> dqRules) {
  public enum CleaningOperation {
    TRIM,
    NORMALIZE_WHITESPACE,
    LOWERCASE,
    UPPERCASE
  }

  public enum DqRuleType {
    NOT_NULL,
    UNIQUE,
    REGEX,
    MIN,
    MAX
  }

  public record CleaningRule(
      @NotNull CleaningOperation operation,
      @NotEmpty List<@NotBlank @Size(max = 128) String> columns) {}

  public record DqRule(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Size(max = 128) String column,
      @NotNull DqRuleType type,
      @Size(max = 500) String value) {}
}