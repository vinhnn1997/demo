package vn.gov.tax.fine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.fine.entity.FineDecision;
public interface FineDecisionRepository extends JpaRepository<FineDecision, Long> { }
