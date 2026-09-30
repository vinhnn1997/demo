package vn.gov.tax.fine.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import vn.gov.tax.common.entity.BaseEntity;

@Entity
@Getter
@Setter
public class FineDecision extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String decisionNumber;

    @Column(nullable = false)
    private String taxpayerCode;

    private String violation;
    private String provinceCode;
    private BigDecimal amount;
    private LocalDate issuedDate;

    @Enumerated(EnumType.STRING)
    private FineStatus status = FineStatus.ISSUED;

}
