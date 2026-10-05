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
        String tenantId = jwt.getClaimAsString("tenant_id");
        return tenantId == null || tenantId.isBlank() ? jwt.getSubject() : tenantId;
    }

    public static String require() {
        String tenantId = get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AccessDeniedException("The access token must identify the acting user");
        }
        return tenantId;
    }

    public static String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)
                || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new AccessDeniedException("The access token must identify the acting user");
        }
        return jwt.getSubject();
    }
}
