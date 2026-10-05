package vn.gov.tax.dataplatform.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
  @Bean
  FilterRegistrationBean<CorsFilter> dataplatformCorsFilter(
      @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8088}") String origins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList());
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
    configuration.setExposedHeaders(List.of("Location"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new CorsFilter(source));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
