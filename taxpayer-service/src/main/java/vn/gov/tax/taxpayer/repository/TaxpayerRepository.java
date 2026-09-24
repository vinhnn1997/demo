package vn.gov.tax.taxpayer.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.taxpayer.entity.Taxpayer;
public interface TaxpayerRepository extends JpaRepository<Taxpayer, Long> { }
