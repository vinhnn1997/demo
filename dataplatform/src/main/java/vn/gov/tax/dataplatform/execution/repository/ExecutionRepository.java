package vn.gov.tax.dataplatform.execution.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.dataplatform.execution.domain.Execution;
import vn.gov.tax.dataplatform.execution.domain.ExecutionStatus;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {
  Optional<Execution> findByTenantIdAndId(String tenantId, UUID id);

  Page<Execution> findAllByTenantIdAndPipelineIdOrderByCreatedAtDesc(
      String tenantId, UUID pipelineId, Pageable pageable);

    boolean existsByTenantIdAndPipelineIdInAndStatusIn(
      String tenantId, Collection<UUID> pipelineIds, Collection<ExecutionStatus> statuses);
}