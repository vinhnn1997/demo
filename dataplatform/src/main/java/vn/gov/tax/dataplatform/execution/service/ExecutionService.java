package vn.gov.tax.dataplatform.execution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import vn.gov.tax.dataplatform.compiler.model.ExecutionPlan;
import vn.gov.tax.dataplatform.compiler.model.PlanValidationResult;
import vn.gov.tax.dataplatform.execution.domain.Execution;
import vn.gov.tax.dataplatform.execution.domain.ExecutionStatus;
import vn.gov.tax.dataplatform.execution.dto.ExecutionResponse;
import vn.gov.tax.dataplatform.execution.repository.ExecutionRepository;
import vn.gov.tax.dataplatform.pipeline.service.PipelineService;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.pipeline.domain.Pipeline;

@Service
@RequiredArgsConstructor
public class ExecutionService {
  private static final TypeReference<List<ExecutionResponse.TaskStatus>> TASK_LIST = new TypeReference<>() {};

  private final ExecutionRepository executionRepository;
  private final PipelineService pipelineService;
  private final AirflowClient airflowClient;
  private final ObjectMapper objectMapper;

  public ExecutionResponse trigger(String tenantId, UUID pipelineId) {
    PlanValidationResult compiled = pipelineService.compile(tenantId, pipelineId);
    if (!compiled.valid()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Pipeline is invalid: " + String.join("; ", compiled.errors()));
    }

    String dagRunId = "manual__" + UUID.randomUUID();
    Execution execution = new Execution();
    execution.setTenantId(tenantId);
    execution.setPipelineId(pipelineId);
    execution.setAirflowDagRunId(dagRunId);
    execution.setStatus(ExecutionStatus.SUBMITTING);
    execution.setTasksJson("[]");
    execution = executionRepository.saveAndFlush(execution);

    ExecutionPlan plan = compiled.plan();
    try {
      AirflowClient.AirflowDagRun airflowRun = airflowClient.trigger(dagRunId, execution.getId(), plan);
      if (airflowRun == null) {
        execution.setStatus(ExecutionStatus.SUBMISSION_UNKNOWN);
        execution.setStatusMessage("Airflow returned no run confirmation; status will be reconciled");
      } else {
        applyRunState(execution, airflowRun);
        try {
          execution.setTasksJson(writeTasks(airflowClient.getTaskStates(dagRunId)));
        } catch (RestClientException ignored) {
          // The run state is still useful if task details are temporarily unavailable.
        }
      }
    } catch (RestClientException exception) {
      execution.setStatus(ExecutionStatus.SUBMISSION_UNKNOWN);
      execution.setStatusMessage("Airflow did not confirm the trigger; status will be reconciled");
    }
    return toResponse(executionRepository.save(execution));
  }

  public Page<ExecutionResponse> list(String tenantId, UUID pipelineId, Pageable pageable) {
    pipelineService.findPipeline(tenantId, pipelineId);
    return executionRepository
        .findAllByTenantIdAndPipelineIdOrderByCreatedAtDesc(tenantId, pipelineId, pageable)
        .map(this::toResponse);
  }

  public DatabaseConnectionRequest connectionForWorker(UUID executionId) {
    Execution execution = executionRepository.findById(executionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found"));
    if (isTerminal(execution.getStatus())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Execution has already finished");
    }
    Pipeline pipeline = pipelineService.findPipeline(execution.getTenantId(), execution.getPipelineId());
    return pipelineService.sourceConnectionForWorker(execution.getTenantId(), pipeline.getSourceId());
  }

  public ExecutionResponse get(String tenantId, UUID id) {
    Execution execution = executionRepository.findByTenantIdAndId(tenantId, id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found"));
    if (!isTerminal(execution.getStatus())) {
      try {
        AirflowClient.AirflowDagRun run = airflowClient.getDagRun(execution.getAirflowDagRunId());
        if (run != null) {
          applyRunState(execution, run);
          execution.setTasksJson(writeTasks(airflowClient.getTaskStates(execution.getAirflowDagRunId())));
          execution = executionRepository.save(execution);
        }
      } catch (RestClientException exception) {
        // Keep the last confirmed state when Airflow is temporarily unavailable.
      }
    }
    return toResponse(execution);
  }

  private void applyRunState(Execution execution, AirflowClient.AirflowDagRun run) {
    String state = run.state() == null ? "" : run.state().toLowerCase(java.util.Locale.ROOT);
    switch (state) {
      case "queued" -> execution.setStatus(ExecutionStatus.QUEUED);
      case "running" -> execution.setStatus(ExecutionStatus.RUNNING);
      case "success" -> execution.setStatus(ExecutionStatus.SUCCEEDED);
      case "failed" -> {
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setStatusMessage("Airflow reported execution failure; watermark must remain unchanged");
      }
      default -> {
        if (execution.getStatus() == ExecutionStatus.SUBMITTING) {
          execution.setStatus(ExecutionStatus.SUBMISSION_UNKNOWN);
        }
      }
    }
    if (run.startDate() != null) {
      execution.setStartedAt(run.startDate());
    } else if (execution.getStatus() == ExecutionStatus.RUNNING && execution.getStartedAt() == null) {
      execution.setStartedAt(Instant.now());
    }
    if (run.endDate() != null) {
      execution.setFinishedAt(run.endDate());
    } else if (isTerminal(execution.getStatus()) && execution.getFinishedAt() == null) {
      execution.setFinishedAt(Instant.now());
    }
    if (execution.getStatus() == ExecutionStatus.SUCCEEDED) {
      execution.setStatusMessage(null);
    }
  }

  private ExecutionResponse toResponse(Execution execution) {
    try {
      return new ExecutionResponse(
          execution.getId(), execution.getPipelineId(), execution.getAirflowDagRunId(),
          execution.getStatus(), execution.getCreatedAt(), execution.getStartedAt(),
          execution.getFinishedAt(), execution.getStatusMessage(),
          objectMapper.readValue(execution.getTasksJson(), TASK_LIST));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Saved execution task state is invalid", exception);
    }
  }

  private String writeTasks(List<AirflowClient.AirflowTaskState> tasks) {
    List<ExecutionResponse.TaskStatus> mapped = tasks.stream()
        .map(task -> new ExecutionResponse.TaskStatus(
            task.taskId(), task.state(), task.startDate(), task.endDate()))
        .toList();
    try {
      return objectMapper.writeValueAsString(mapped);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Airflow task state cannot be serialized", exception);
    }
  }

  private static boolean isTerminal(ExecutionStatus status) {
    return status == ExecutionStatus.SUCCEEDED || status == ExecutionStatus.FAILED;
  }
}