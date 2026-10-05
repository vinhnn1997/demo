package vn.gov.tax.eureka.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EurekaOpenApiConfiguration {
    @Bean
    OpenAPI eurekaOpenAPI() {
        return new OpenAPI().info(new Info().title("Eureka Server API").version("v1"));
    }
}