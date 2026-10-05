package vn.gov.tax.dataplatform.source.connector;

import org.springframework.stereotype.Component;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;

@Component
public class MssqlConnectionTester extends AbstractJdbcDatabaseConnectionTester {
    @Override
    public DatabaseType type() {
        return DatabaseType.MSSQL;
    }

    @Override
    protected String jdbcUrl(DatabaseConnectionRequest request) {
        return "jdbc:sqlserver://" + safeHost(request) + ":" + request.port()
                + ";databaseName=" + safeDatabase(request)
                + ";encrypt=" + request.useEncryption()
                + ";trustServerCertificate=" + request.useTrustServerCertificate()
                + ";loginTimeout=5;socketTimeout=5000";
    }

    @Override
    protected String validationSql() {
        return "SELECT 1";
    }

    @Override
    protected String schemaLookupSql() {
        return "SELECT SCHEMA_ID(?)";
    }
}
