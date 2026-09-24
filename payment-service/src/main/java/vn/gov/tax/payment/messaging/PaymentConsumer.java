package vn.gov.tax.payment.messaging;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.gov.tax.common.messaging.KafkaTopics;
import vn.gov.tax.common.messaging.event.FineCreatedEvent;
import vn.gov.tax.payment.entity.Payment;
import vn.gov.tax.payment.repository.PaymentRepository;
@Component public class PaymentConsumer {
 private final PaymentRepository repository; public PaymentConsumer(PaymentRepository repository){this.repository=repository;}
 @KafkaListener(topics=KafkaTopics.FINE_CREATED, groupId="payment-service")
 public void onFineCreated(FineCreatedEvent event){
  if(repository.findByDecisionNumber(event.decisionNumber()).isPresent()) return;
  Payment payment=new Payment(); payment.setDecisionNumber(event.decisionNumber()); payment.setTaxpayerCode(event.taxpayerCode()); payment.setAmount(event.amount()); repository.save(payment);
 }
}
