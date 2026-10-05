package vn.gov.tax.dataplatform.execution.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gov.tax.dataplatform.persistence.DataPlatformEntity;

@Entity
@Table(name = "data_execution")
@Getter
@Setter
@NoArgsConstructor
public class Execution extends DataPlatformEntity {
  @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
  private String tenantId;

  @Column(name = "pipeline_id", nullable = false)
  private UUID pipelineId;

  @Column(name = "airflow_dag_run_id", nullable = false, length = 160, unique = true)
  private String airflowDagRunId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ExecutionStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "status_message", length = 500)
  private String statusMessage;

  @Column(name = "tasks_json", nullable = false, columnDefinition = "nvarchar(max)")
  private String tasksJson = "[]";
}