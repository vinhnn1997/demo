package vn.gov.tax.dataplatform.execution.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.gov.tax.dataplatform.execution.domain.ExecutionStatus;

public record ExecutionResponse(
    UUID id,
    UUID pipelineId,
    String airflowDagRunId,
    ExecutionStatus status,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    String statusMessage,
    List<TaskStatus> tasks) {
  public ExecutionResponse {
    tasks = List.copyOf(tasks);
  }

  public record TaskStatus(String taskId, String state, Instant startedAt, Instant finishedAt) {}
}