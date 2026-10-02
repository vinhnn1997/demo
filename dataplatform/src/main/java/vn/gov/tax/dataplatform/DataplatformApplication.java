package vn.gov.tax.dataplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vn.gov.tax.common.security.CommonSecurityConfig;

@SpringBootApplication
@Import(CommonSecurityConfig.class)
@EntityScan("vn.gov.tax")
@EnableJpaRepositories("vn.gov.tax")
public class DataplatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataplatformApplication.class, args);
    }
}
