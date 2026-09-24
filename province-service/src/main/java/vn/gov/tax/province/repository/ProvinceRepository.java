package vn.gov.tax.province.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.tax.province.entity.Province;
public interface ProvinceRepository extends JpaRepository<Province, Long> { }
