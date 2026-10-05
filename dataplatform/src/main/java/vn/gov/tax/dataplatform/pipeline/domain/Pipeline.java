package vn.gov.tax.dataplatform.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gov.tax.dataplatform.persistence.DataPlatformEntity;

@Entity
@Table(
    name = "data_pipeline",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pipeline_tenant_name",
        columnNames = {"tenant_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Pipeline extends DataPlatformEntity {
  @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
  private String tenantId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(name = "source_id", nullable = false)
  private UUID sourceId;

  @Column(name = "definition_json", nullable = false, columnDefinition = "nvarchar(max)")
  private String definitionJson;
}