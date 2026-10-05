package vn.gov.tax.dataplatform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

class CorsConfigTest {
  @Test
  void allowsConfiguredOriginPreflightRequests() throws Exception {
    FilterRegistrationBean<CorsFilter> registration =
        new CorsConfig().dataplatformCorsFilter("http://localhost:3000,http://localhost:8088");
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/sources");
    request.addHeader("Origin", "http://localhost:8088");
    request.addHeader("Access-Control-Request-Method", "GET");
    request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (servletRequest, servletResponse) -> {};

    registration.getFilter().doFilter(request, response, chain);

    assertEquals("http://localhost:8088", response.getHeader("Access-Control-Allow-Origin"));
    assertEquals(200, response.getStatus());
  }

  @Test
  void rejectsOriginsNotInConfiguration() throws Exception {
    FilterRegistrationBean<CorsFilter> registration =
        new CorsConfig().dataplatformCorsFilter("http://localhost:3000,http://localhost:8088");
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/sources");
    request.addHeader("Origin", "http://malicious.example");
    request.addHeader("Access-Control-Request-Method", "GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (servletRequest, servletResponse) -> {};

    registration.getFilter().doFilter(request, response, chain);

    assertEquals(403, response.getStatus());
    assertFalse(response.containsHeader("Access-Control-Allow-Origin"));
  }
}
