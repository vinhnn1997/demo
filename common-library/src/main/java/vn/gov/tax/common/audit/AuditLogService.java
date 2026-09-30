package vn.gov.tax.common.audit;

import java.time.Instant;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String serviceName, String action, String requestUri, String username,
                       int httpStatus, long durationMs, String errorMessage) {
        AuditLog log = new AuditLog();
        log.setServiceName(serviceName);
        log.setAction(action);
        log.setRequestUri(requestUri);
        log.setUsername(username);
        log.setHttpStatus(httpStatus);
        log.setDurationMs(durationMs);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(Instant.now());
        repository.save(log);
    }
}
