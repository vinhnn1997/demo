package vn.gov.tax.province;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cloud.openfeign.EnableFeignClients;
import vn.gov.tax.common.security.CommonSecurityConfig;

@SpringBootApplication
@Import(CommonSecurityConfig.class)
@EntityScan("vn.gov.tax")
@EnableJpaRepositories("vn.gov.tax")
@EnableFeignClients(basePackages = "vn.gov.tax")
public class ProvinceServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ProvinceServiceApplication.class, args); }
}
