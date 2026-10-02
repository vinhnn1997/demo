package vn.gov.tax.dataplatform.source.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;
import vn.gov.tax.dataplatform.source.DatabaseConnectionRequest;

abstract class AbstractJdbcDatabaseConnectionTester implements DatabaseConnectionTester {
    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9._:-]+$");
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9_$-]+$");

    protected abstract String jdbcUrl(DatabaseConnectionRequest request);

    protected abstract String validationSql();

    protected abstract String schemaLookupSql();

    @Override
    public ConnectionTestResult test(DatabaseConnectionRequest request) {
        long startedAt = System.nanoTime();
        validateUrlParts(request);
        Properties properties = new Properties();
        properties.setProperty("user", request.username());
        properties.setProperty("password", request.password());

        try (Connection connection = DriverManager.getConnection(jdbcUrl(request), properties);
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            statement.execute(validationSql());
            if (request.schemaName() != null && !request.schemaName().isBlank()) {
                try (PreparedStatement schemaQuery = connection.prepareStatement(schemaLookupSql())) {
                    schemaQuery.setQueryTimeout(5);
                    schemaQuery.setString(1, request.schemaName());
                    try (ResultSet result = schemaQuery.executeQuery()) {
                        if (!result.next() || result.getObject(1) == null) {
                            return result(false, "Connected, but the requested schema was not found", startedAt);
                        }
                    }
                }
            }
            return result(true, "Connection successful", startedAt);
        } catch (SQLException exception) {
            return result(false, "Connection failed; check the server, database, credentials, and TLS settings", startedAt);
        }
    }

    protected static String safeHost(DatabaseConnectionRequest request) {
        return request.host();
    }

    protected static String safeDatabase(DatabaseConnectionRequest request) {
        return request.databaseName();
    }

    private static void validateUrlParts(DatabaseConnectionRequest request) {
        if (request.host() == null || !HOST.matcher(request.host()).matches()) {
            throw new IllegalArgumentException("Host contains unsupported characters");
        }
        if (request.databaseName() == null || !IDENTIFIER.matcher(request.databaseName()).matches()) {
            throw new IllegalArgumentException("Database or service name contains unsupported characters");
        }
        if (request.port() < 1 || request.port() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
    }

    private static ConnectionTestResult result(boolean success, String message, long startedAt) {
        return new ConnectionTestResult(success, message, (System.nanoTime() - startedAt) / 1_000_000);
    }
}
