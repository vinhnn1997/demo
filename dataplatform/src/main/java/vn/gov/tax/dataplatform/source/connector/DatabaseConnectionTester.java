package vn.gov.tax.dataplatform.source.connector;

import vn.gov.tax.dataplatform.source.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.DatabaseType;

public interface DatabaseConnectionTester {
    DatabaseType type();

    ConnectionTestResult test(DatabaseConnectionRequest request);

    record ConnectionTestResult(boolean success, String message, long durationMs) {
    }
}
