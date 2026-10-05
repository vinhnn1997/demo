package vn.gov.tax.dataplatform.compiler.model;

import java.util.List;

public record PlanValidationResult(boolean valid, List<String> errors, ExecutionPlan plan) {
  public PlanValidationResult {
    errors = List.copyOf(errors);
  }
}