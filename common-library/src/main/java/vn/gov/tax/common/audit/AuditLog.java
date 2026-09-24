package vn.gov.tax.common.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_logs_username", columnList = "username")
})
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "service_name", nullable = false, length = 120) private String serviceName;
    @Column(nullable = false, length = 120) private String action;
    @Column(name = "request_uri", length = 500) private String requestUri;
    @Column(length = 150) private String username;
    @Column(name = "http_status") private Integer httpStatus;
    @Column(name = "duration_ms") private Long durationMs;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public Long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String value) { serviceName = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String value) { requestUri = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer value) { httpStatus = value; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long value) { durationMs = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}