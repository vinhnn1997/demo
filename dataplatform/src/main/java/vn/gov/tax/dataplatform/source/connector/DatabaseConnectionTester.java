package vn.gov.tax.dataplatform.source.connector;

import java.util.List;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;
import vn.gov.tax.dataplatform.source.domain.DatabaseType;

public interface DatabaseConnectionTester {
    DatabaseType type();

    ConnectionTestResult test(DatabaseConnectionRequest request);

    List<DiscoveredTable> discoverTables(DatabaseConnectionRequest request);

    List<DiscoveredColumn> discoverColumns(DatabaseConnectionRequest request, String tableName);

    record DiscoveredTable(String catalog, String schema, String name, String type) {
    }

    record DiscoveredColumn(String name, String type, int jdbcType, boolean nullable) {
    }

    record ConnectionTestResult(boolean success, String message, long durationMs) {
    }
}
