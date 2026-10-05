package vn.gov.tax.dataplatform.source.repository;

import java.util.Optional;
import vn.gov.tax.dataplatform.source.domain.SourceCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceCredentialRepository extends JpaRepository<SourceCredential, String> {
    Optional<SourceCredential> findBySecretRefAndTenantId(String secretRef, String tenantId);
}
