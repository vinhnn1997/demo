package vn.gov.tax.payment.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.payment.entity.Payment;
public interface PaymentRepository extends JpaRepository<Payment, Long> { Optional<Payment> findByDecisionNumber(String decisionNumber); }
