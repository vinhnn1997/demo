package vn.gov.tax.organization.entity;
import jakarta.persistence.*;
@Entity public class Organization {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true) private String code;
 @Column(nullable=false) private String name;
 private String provinceCode; private String type;
 public Long getId(){return id;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getProvinceCode(){return provinceCode;} public void setProvinceCode(String v){provinceCode=v;} public String getType(){return type;} public void setType(String v){type=v;}
}
