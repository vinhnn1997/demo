package vn.gov.tax.taxpayer.entity;
import jakarta.persistence.*;
@Entity public class Taxpayer {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true) private String taxCode;
 @Column(nullable=false) private String name;
 private String address; private String provinceCode; private String phone;
 public Long getId(){return id;} public String getTaxCode(){return taxCode;} public void setTaxCode(String v){taxCode=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;} public String getProvinceCode(){return provinceCode;} public void setProvinceCode(String v){provinceCode=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
}
