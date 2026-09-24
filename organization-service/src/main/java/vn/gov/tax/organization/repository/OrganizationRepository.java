package vn.gov.tax.organization.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.organization.entity.Organization;
public interface OrganizationRepository extends JpaRepository<Organization, Long> { }
