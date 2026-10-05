package vn.gov.tax.dataplatform.execution.domain;

public enum ExecutionStatus {
  SUBMITTING,
  SUBMISSION_UNKNOWN,
  QUEUED,
  RUNNING,
  SUCCEEDED,
  FAILED
}