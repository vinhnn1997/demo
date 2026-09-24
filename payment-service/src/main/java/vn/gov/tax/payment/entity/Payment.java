package vn.gov.tax.payment.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
@Entity public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true) private String decisionNumber;
 private String taxpayerCode; private BigDecimal amount;
 @Enumerated(EnumType.STRING) private PaymentStatus status = PaymentStatus.PENDING;
 public Long getId(){return id;} public String getDecisionNumber(){return decisionNumber;} public void setDecisionNumber(String v){decisionNumber=v;} public String getTaxpayerCode(){return taxpayerCode;} public void setTaxpayerCode(String v){taxpayerCode=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public PaymentStatus getStatus(){return status;} public void setStatus(PaymentStatus v){status=v;}
}
