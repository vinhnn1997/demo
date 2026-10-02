package vn.gov.tax.dataplatform.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gov.tax.dataplatform.persistence.DataPlatformEntity;

@Entity
@Table(
    name = "dataset",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_dataset_tenant_name",
            columnNames = {"tenant_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Dataset extends DataPlatformEntity {
  @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
  private String tenantId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, length = 500)
  private String storageUri;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private State state = State.DRAFT;

  public enum State {
    DRAFT,
    ACTIVE,
    DEPRECATED
  }
}
