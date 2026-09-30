package vn.gov.tax.common.audit;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_logs_username", columnList = "username")
})
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;
    @Column(nullable = false, length = 120)
    private String action;
    @Column(name = "request_uri", length = 500)
    private String requestUri;
    @Column(length = 150)
    private String username;
    @Column(name = "http_status")
    private Integer httpStatus;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
