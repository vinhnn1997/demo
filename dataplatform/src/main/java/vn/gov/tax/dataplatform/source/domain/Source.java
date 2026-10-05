package vn.gov.tax.dataplatform.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gov.tax.dataplatform.persistence.DataPlatformEntity;

@Entity
@Table(
    name = "data_source",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_source_tenant_name",
            columnNames = {"tenant_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Source extends DataPlatformEntity {
  @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
  private String tenantId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 32)
  private DatabaseType type;

  @Column(length = 500)
  private String description;

  @Column(name = "created_by", nullable = false, updatable = false, length = 255)
  private String createdBy;

  @Column(name = "updated_by", nullable = false, length = 255)
  private String updatedBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "deleted_by", length = 255)
  private String deletedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status = Status.ACTIVE;

  @OneToOne(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private SourceConnection connection;

  public enum Status {
    ACTIVE,
    DISABLED
  }

}
