package vn.gov.tax.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Configuration
public class AuditLogAspect {
    @Value("${spring.application.name:unknown-service}")
    private String serviceName;
    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) { this.auditLogService = auditLogService; }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.currentTimeMillis();
        Throwable failure = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            recordSafely(joinPoint, startedAt, failure);
        }
    }

    private void recordSafely(ProceedingJoinPoint joinPoint, long startedAt, Throwable failure) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes == null ? null : attributes.getRequest();
            HttpServletResponse response = attributes == null ? null : attributes.getResponse();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication == null ? "anonymous" : authentication.getName();
            int status = response == null ? (failure == null ? 200 : 500) : response.getStatus();
            String uri = request == null ? null : request.getRequestURI();
            String error = failure == null ? null : failure.getClass().getSimpleName() + ": " + failure.getMessage();
            auditLogService.record(serviceName, joinPoint.getSignature().toShortString(), uri, username,
                status, System.currentTimeMillis() - startedAt, error);
        } catch (Exception ignored) {
            // Audit failure must never break the business request.
        }
    }
}