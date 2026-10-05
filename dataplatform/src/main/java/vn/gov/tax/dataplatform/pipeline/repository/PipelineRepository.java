package vn.gov.tax.dataplatform.pipeline.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.dataplatform.pipeline.domain.Pipeline;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {
  Page<Pipeline> findAllByTenantIdOrderByNameAsc(String tenantId, Pageable pageable);

  Optional<Pipeline> findByTenantIdAndId(String tenantId, UUID id);

  boolean existsByTenantIdAndName(String tenantId, String name);

  boolean existsByTenantIdAndNameAndIdNot(String tenantId, String name, UUID id);

  List<Pipeline> findAllByTenantIdAndSourceId(String tenantId, UUID sourceId);
}