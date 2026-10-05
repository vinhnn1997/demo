package vn.gov.tax.dataplatform.source.connector;

import org.springframework.stereotype.Component;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;

@Component
public class MysqlConnectionTester extends AbstractJdbcDatabaseConnectionTester {
    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    @Override
    protected String jdbcUrl(DatabaseConnectionRequest request) {
        String sslMode = !request.useEncryption()
                ? "DISABLED"
                : request.useTrustServerCertificate() ? "REQUIRED" : "VERIFY_IDENTITY";
        return "jdbc:mysql://" + safeHost(request) + ":" + request.port() + "/" + safeDatabase(request)
                + "?sslMode=" + sslMode + "&connectTimeout=5000&socketTimeout=5000";
    }

    @Override
    protected String validationSql() {
        return "SELECT 1";
    }

    @Override
    protected String schemaLookupSql() {
        return "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";
    }
}
