package vn.gov.tax.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import vn.gov.tax.common.exception.GlobalExceptionHandler;
import vn.gov.tax.common.audit.AuditLogAspect;
import vn.gov.tax.common.audit.AuditLogService;
import vn.gov.tax.common.messaging.CommonKafkaConfig;

@Configuration
@EnableMethodSecurity
@Import({GlobalExceptionHandler.class, AuditLogAspect.class, AuditLogService.class, CommonKafkaConfig.class,
        TokenRevocationConfiguration.class, CommonJwtConfiguration.class})
public class CommonSecurityConfig {
    @Bean
    SecurityFilterChain commonSecurityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/internal/**", "/api/*/internal/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("supervisor")
                        .requestMatchers("/api/**").hasAnyRole("tax-officer", "supervisor")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
