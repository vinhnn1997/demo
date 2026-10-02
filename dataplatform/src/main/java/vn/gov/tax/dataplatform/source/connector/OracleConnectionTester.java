package vn.gov.tax.dataplatform.source.connector;

import org.springframework.stereotype.Component;
import vn.gov.tax.dataplatform.source.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.DatabaseType;

@Component
public class OracleConnectionTester extends AbstractJdbcDatabaseConnectionTester {
    @Override
    public DatabaseType type() {
        return DatabaseType.ORACLE;
    }

    @Override
    protected String jdbcUrl(DatabaseConnectionRequest request) {
        if (request.useTrustServerCertificate()) {
            throw new IllegalArgumentException(
                    "Oracle certificate validation must be configured with the JVM truststore");
        }
        String protocol = request.useEncryption() ? "TCPS" : "TCP";
        return "jdbc:oracle:thin:@(DESCRIPTION=(CONNECT_TIMEOUT=5)(RETRY_COUNT=0)"
                + "(ADDRESS=(PROTOCOL=" + protocol + ")(HOST=" + safeHost(request) + ")(PORT=" + request.port() + "))"
                + "(CONNECT_DATA=(SERVICE_NAME=" + safeDatabase(request) + ")))";
    }

    @Override
    protected String validationSql() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    protected String schemaLookupSql() {
        return "SELECT username FROM all_users WHERE username = UPPER(?)";
    }
}
