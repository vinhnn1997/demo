package vn.gov.tax.dataplatform.source.repository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.dataplatform.source.domain.Source;

public interface SourceRepository extends JpaRepository<Source, UUID> {
  @EntityGraph(attributePaths = "connection")
  Page<Source> findAllByTenantIdAndStatusOrderByNameAsc(
      String tenantId, Source.Status status, Pageable pageable);

  @EntityGraph(attributePaths = "connection")
  Optional<Source> findByTenantIdAndId(String tenantId, UUID id);

  boolean existsByTenantIdAndName(String tenantId, String name);

  boolean existsByTenantIdAndNameAndIdNot(String tenantId, String name, UUID id);
}
