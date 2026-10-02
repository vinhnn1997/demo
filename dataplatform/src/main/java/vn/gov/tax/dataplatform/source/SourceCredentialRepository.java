package vn.gov.tax.dataplatform.source;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceCredentialRepository extends JpaRepository<SourceCredential, String> {
    Optional<SourceCredential> findBySecretRefAndTenantId(String secretRef, String tenantId);
}
