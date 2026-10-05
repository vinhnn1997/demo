package vn.gov.tax.dataplatform.source.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;

class DatabaseConnectionTesterTest {
    private static final String PASSWORD = "never-in-url";

    @Test
    void buildsSqlServerUrlWithTlsOptions() {
        MssqlConnectionTester tester = new MssqlConnectionTester();

        String url = tester.jdbcUrl(connection(true, false));

        assertEquals(
                "jdbc:sqlserver://db.example.test:1433;databaseName=reporting;encrypt=true;"
                        + "trustServerCertificate=false;loginTimeout=5;socketTimeout=5000",
                url);
        assertFalse(url.contains(PASSWORD));
    }

    @Test
    void buildsMysqlUrlWithIdentityVerification() {
        MysqlConnectionTester tester = new MysqlConnectionTester();

        String url = tester.jdbcUrl(connection(true, false));

        assertEquals(
                "jdbc:mysql://db.example.test:1433/reporting?sslMode=VERIFY_IDENTITY"
                        + "&connectTimeout=5000&socketTimeout=5000",
                url);
        assertFalse(url.contains(PASSWORD));
    }

    @Test
    void buildsPostgresqlUrlWithFullCertificateVerification() {
        PostgresqlConnectionTester tester = new PostgresqlConnectionTester();

        String url = tester.jdbcUrl(connection(true, false));

        assertEquals(
                "jdbc:postgresql://db.example.test:1433/reporting?sslmode=verify-full"
                        + "&connectTimeout=5&socketTimeout=5",
                url);
        assertFalse(url.contains(PASSWORD));
    }

    @Test
    void buildsOracleTcpsServiceNameUrl() {
        OracleConnectionTester tester = new OracleConnectionTester();

        String url = tester.jdbcUrl(connection(true, false));

        assertTrue(url.contains("(PROTOCOL=TCPS)"));
        assertTrue(url.contains("(SERVICE_NAME=reporting)"));
        assertFalse(url.contains(PASSWORD));
    }

    private static DatabaseConnectionRequest connection(boolean encrypt, boolean trustServerCertificate) {
        return new DatabaseConnectionRequest(
                "db.example.test", 1433, "reporting", "dbo", "reader", PASSWORD,
                encrypt, trustServerCertificate);
    }
}
