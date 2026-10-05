package vn.gov.tax.dataplatform.source.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;
import vn.gov.tax.dataplatform.source.domain.Source;

public interface SourceRepository extends JpaRepository<Source, UUID> {
  @EntityGraph(attributePaths = "connection")
  Page<Source> findAllByTenantIdAndStatusOrderByNameAsc(
      String tenantId, Source.Status status, Pageable pageable);

  @EntityGraph(attributePaths = "connection")
  @Query("""
      select source from Source source
      where source.tenantId = :tenantId
        and (:type is null or source.type = :type)
        and (:status is null or source.status = :status)
        and (:name is null or lower(source.name) like lower(concat('%', :name, '%')))
      order by source.name asc
      """)
  Page<Source> searchByTenantAndFilters(
      @Param("tenantId") String tenantId,
      @Param("type") DatabaseType type,
      @Param("status") Source.Status status,
      @Param("name") String name,
      Pageable pageable);

  @EntityGraph(attributePaths = "connection")
  Optional<Source> findByTenantIdAndId(String tenantId, UUID id);

  boolean existsByTenantIdAndName(String tenantId, String name);

  boolean existsByTenantIdAndNameAndIdNot(String tenantId, String name, UUID id);
}
