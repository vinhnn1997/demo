package vn.gov.tax.common.security;

import java.time.Instant;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class JwtRevocationValidator implements OAuth2TokenValidator<Jwt> {
    private static final Logger log = LoggerFactory.getLogger(JwtRevocationValidator.class);

    private final TokenRevocationService tokenRevocationService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Instant issuedAt = token.getIssuedAt();
        if (issuedAt == null) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token issued-at claim is missing", null));
        }

        try {
            Long cutoff = tokenRevocationService.findCutoffEpochSeconds(token.getSubject());
            if (cutoff != null && issuedAt.getEpochSecond() <= cutoff) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token has been revoked due to a role change", null));
            }
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException ex) {
            log.error("Token revocation check failed closed for subject={}", token.getSubject(), ex);
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("server_error", "Token revocation check unavailable", null));
        }
    }
}
