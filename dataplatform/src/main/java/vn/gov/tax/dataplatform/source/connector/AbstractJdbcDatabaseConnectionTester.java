package vn.gov.tax.dataplatform.source.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;

abstract class AbstractJdbcDatabaseConnectionTester implements DatabaseConnectionTester {
    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9._:-]+$");
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9_$-]+$");
        private static final Pattern QUALIFIED_IDENTIFIER =
            Pattern.compile("^[A-Za-z0-9_$-]+(\\.[A-Za-z0-9_$-]+)?$");

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

    @Override
    public List<DiscoveredTable> discoverTables(DatabaseConnectionRequest request) {
        validateUrlParts(request);
        Properties properties = new Properties();
        properties.setProperty("user", request.username());
        properties.setProperty("password", request.password());
        try (Connection connection = DriverManager.getConnection(jdbcUrl(request), properties)) {
            DatabaseMetaData metadata = connection.getMetaData();
            String schemaPattern = request.schemaName() == null || request.schemaName().isBlank()
                    ? null
                    : request.schemaName();
            List<DiscoveredTable> tables = new ArrayList<>();
            try (ResultSet result = metadata.getTables(
                    connection.getCatalog(), schemaPattern, "%", new String[] {"TABLE", "VIEW"})) {
                while (result.next()) {
                    tables.add(new DiscoveredTable(
                            result.getString("TABLE_CAT"),
                            result.getString("TABLE_SCHEM"),
                            result.getString("TABLE_NAME"),
                            result.getString("TABLE_TYPE")));
                }
            }
            return List.copyOf(tables);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to discover source tables; check the connection and schema", exception);
        }
    }

    @Override
    public List<DiscoveredColumn> discoverColumns(DatabaseConnectionRequest request, String tableName) {
        validateUrlParts(request);
        if (tableName == null || !QUALIFIED_IDENTIFIER.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Table name contains unsupported characters");
        }
        String[] parts = tableName.split("\\.", 2);
        String schemaPattern = parts.length == 2 ? parts[0] : request.schemaName();
        String tablePattern = parts.length == 2 ? parts[1] : parts[0];
        Properties properties = new Properties();
        properties.setProperty("user", request.username());
        properties.setProperty("password", request.password());
        try (Connection connection = DriverManager.getConnection(jdbcUrl(request), properties)) {
            List<DiscoveredColumn> columns = new ArrayList<>();
            try (ResultSet result = connection.getMetaData().getColumns(
                    connection.getCatalog(), schemaPattern, tablePattern, "%")) {
                while (result.next()) {
                    if (!tablePattern.equalsIgnoreCase(result.getString("TABLE_NAME"))) {
                        continue;
                    }
                    String resultSchema = result.getString("TABLE_SCHEM");
                    if (schemaPattern != null && !schemaPattern.equalsIgnoreCase(resultSchema)) {
                        continue;
                    }
                    columns.add(new DiscoveredColumn(
                            result.getString("COLUMN_NAME"),
                            result.getString("TYPE_NAME"),
                            result.getInt("DATA_TYPE"),
                            result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
                }
            }
            return List.copyOf(columns);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to inspect source table schema", exception);
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
