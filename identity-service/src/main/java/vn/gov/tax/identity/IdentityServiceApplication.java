package vn.gov.tax.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vn.gov.tax.common.audit.AuditLogAspect;
import vn.gov.tax.common.audit.AuditLogService;
import vn.gov.tax.common.exception.GlobalExceptionHandler;
import vn.gov.tax.common.security.CommonJwtConfiguration;
import vn.gov.tax.common.security.TokenRevocationConfiguration;
import vn.gov.tax.identity.config.IdentitySecurityConfig;
import vn.gov.tax.identity.config.KeycloakAdminProperties;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakAdminProperties.class)
@Import({
        GlobalExceptionHandler.class,
        AuditLogAspect.class,
        AuditLogService.class,
        TokenRevocationConfiguration.class,
        CommonJwtConfiguration.class,
        IdentitySecurityConfig.class
})
@EntityScan("vn.gov.tax")
@EnableJpaRepositories("vn.gov.tax")
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
