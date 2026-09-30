package vn.gov.tax.taxpayer.entity;

import vn.gov.tax.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Taxpayer extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String taxCode;
    @Column(nullable = false)
    private String name;
    private String address;
    private String provinceCode;
    private String phone;

}
