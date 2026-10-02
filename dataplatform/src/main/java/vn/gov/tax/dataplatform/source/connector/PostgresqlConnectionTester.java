package vn.gov.tax.dataplatform.source.connector;

import org.springframework.stereotype.Component;
import vn.gov.tax.dataplatform.source.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.DatabaseType;

@Component
public class PostgresqlConnectionTester extends AbstractJdbcDatabaseConnectionTester {
    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    protected String jdbcUrl(DatabaseConnectionRequest request) {
        String sslMode = !request.useEncryption()
                ? "disable"
                : request.useTrustServerCertificate() ? "require" : "verify-full";
        return "jdbc:postgresql://" + safeHost(request) + ":" + request.port() + "/" + safeDatabase(request)
                + "?sslmode=" + sslMode + "&connectTimeout=5&socketTimeout=5";
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
