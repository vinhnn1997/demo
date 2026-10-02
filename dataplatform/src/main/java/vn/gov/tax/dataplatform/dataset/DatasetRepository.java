package vn.gov.tax.dataplatform.dataset;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {
  Page<Dataset> findAllByTenantId(String tenant, Pageable p);
}
