package vn.gov.tax.fine.entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
@Entity public class FineDecision {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true) private String decisionNumber;
 @Column(nullable=false) private String taxpayerCode;
 private String violation; private String provinceCode; private BigDecimal amount; private LocalDate issuedDate;
 @Enumerated(EnumType.STRING) private FineStatus status = FineStatus.ISSUED;
 public Long getId(){return id;} public String getDecisionNumber(){return decisionNumber;} public void setDecisionNumber(String v){decisionNumber=v;} public String getTaxpayerCode(){return taxpayerCode;} public void setTaxpayerCode(String v){taxpayerCode=v;} public String getViolation(){return violation;} public void setViolation(String v){violation=v;} public String getProvinceCode(){return provinceCode;} public void setProvinceCode(String v){provinceCode=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getIssuedDate(){return issuedDate;} public void setIssuedDate(LocalDate v){issuedDate=v;} public FineStatus getStatus(){return status;} public void setStatus(FineStatus v){status=v;}
}
