package vn.gov.tax.dataplatform.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class TenantContext {
    private TenantContext() {
    }

    public static String get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return jwt.getClaimAsString("tenant_id");
    }

    public static String require() {
        String tenantId = get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AccessDeniedException("The access token must contain a tenant_id claim");
        }
        return tenantId;
    }
}
