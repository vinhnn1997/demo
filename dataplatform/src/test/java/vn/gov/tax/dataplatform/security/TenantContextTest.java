package vn.gov.tax.dataplatform.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantContextTest {
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usesSubjectWhenTenantIdClaimIsAbsent() {
    setAuthentication(Jwt.withTokenValue("token").header("alg", "none").subject("user-123").build());

    assertEquals("user-123", TenantContext.require());
  }

  @Test
  void prefersTenantIdClaimWhenPresent() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user-123")
        .claim("tenant_id", "tenant-456")
        .build();
    setAuthentication(jwt);

    assertEquals("tenant-456", TenantContext.require());
  }

  @Test
  void deniesTokensWithoutTenantIdOrSubject() {
    setAuthentication(Jwt.withTokenValue("token").header("alg", "none").build());

    assertThrows(AccessDeniedException.class, TenantContext::require);
  }

  private void setAuthentication(Jwt jwt) {
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
