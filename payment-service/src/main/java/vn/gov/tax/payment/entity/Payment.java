package vn.gov.tax.payment.entity;

import java.math.BigDecimal;

import vn.gov.tax.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Payment extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String decisionNumber;
    private String taxpayerCode;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

}
