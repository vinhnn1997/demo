package vn.gov.tax.dataplatform.execution.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import vn.gov.tax.dataplatform.compiler.model.ExecutionPlan;

@Service
public class AirflowClient {
  private final RestClient restClient;
  private final String apiPrefix;
  private final String dagId;

  public AirflowClient(
      RestClient.Builder builder,
      @Value("${dataplatform.airflow.base-url}") String baseUrl,
      @Value("${dataplatform.airflow.api-prefix:/api/v1}") String apiPrefix,
      @Value("${dataplatform.airflow.dag-id:tax_data_pipeline}") String dagId,
      @Value("${dataplatform.airflow.username:}") String username,
            @Value("${dataplatform.airflow.password:}") String password,
            @Value("${dataplatform.airflow.api-token:}") String apiToken,
            @Value("${dataplatform.airflow.connect-timeout:3s}") Duration connectTimeout,
            @Value("${dataplatform.airflow.read-timeout:10s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (apiToken != null && !apiToken.isBlank()) {
                        headers.setBearerAuth(apiToken);
                    } else if (username != null && !username.isBlank()) {
                        headers.setBasicAuth(username, password);
                    }
                })
        .build();
    this.apiPrefix = apiPrefix.endsWith("/")
        ? apiPrefix.substring(0, apiPrefix.length() - 1)
        : apiPrefix;
    this.dagId = dagId;
  }

  public AirflowDagRun trigger(String dagRunId, UUID executionId, ExecutionPlan plan) {
    Map<String, Object> request = Map.of(
        "dag_run_id", dagRunId,
        "conf", Map.of("execution_id", executionId.toString(), "plan", plan));
    return restClient.post()
        .uri(apiPrefix + "/dags/{dagId}/dagRuns", dagId)
        .body(request)
        .retrieve()
        .body(AirflowDagRun.class);
  }

  public AirflowDagRun getDagRun(String dagRunId) {
    return restClient.get()
        .uri(apiPrefix + "/dags/{dagId}/dagRuns/{dagRunId}", dagId, dagRunId)
        .retrieve()
        .body(AirflowDagRun.class);
  }

  public List<AirflowTaskState> getTaskStates(String dagRunId) {
    AirflowTaskResponse response = restClient.get()
        .uri(apiPrefix + "/dags/{dagId}/dagRuns/{dagRunId}/taskInstances", dagId, dagRunId)
        .retrieve()
        .body(AirflowTaskResponse.class);
    return response == null || response.taskInstances() == null ? List.of() : response.taskInstances();
  }

  public record AirflowDagRun(
      @JsonProperty("dag_run_id") String dagRunId,
      String state,
      @JsonProperty("start_date") Instant startDate,
      @JsonProperty("end_date") Instant endDate) {}

  public record AirflowTaskResponse(
      @JsonProperty("task_instances") List<AirflowTaskState> taskInstances) {}

  public record AirflowTaskState(
      @JsonProperty("task_id") String taskId,
      String state,
      @JsonProperty("start_date") Instant startDate,
      @JsonProperty("end_date") Instant endDate) {}
}