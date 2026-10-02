package vn.gov.tax.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(TokenRevocationProperties.class)
public class TokenRevocationConfiguration {
    @Bean
    TokenRevocationService tokenRevocationService(
            StringRedisTemplate stringRedisTemplate, TokenRevocationProperties properties) {
        return new TokenRevocationService(stringRedisTemplate, properties);
    }

    @Bean
    JwtRevocationValidator jwtRevocationValidator(TokenRevocationService tokenRevocationService) {
        return new JwtRevocationValidator(tokenRevocationService);
    }
}
