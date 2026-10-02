package vn.gov.tax.dataplatform.source.connector;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.gov.tax.dataplatform.source.DatabaseType;

@Component
public class DatabaseConnectionTesterRegistry {
    private final Map<DatabaseType, DatabaseConnectionTester> testers;

    public DatabaseConnectionTesterRegistry(List<DatabaseConnectionTester> testers) {
        EnumMap<DatabaseType, DatabaseConnectionTester> byType = new EnumMap<>(DatabaseType.class);
        for (DatabaseConnectionTester tester : testers) {
            if (byType.put(tester.type(), tester) != null) {
                throw new IllegalStateException("Duplicate database tester for " + tester.type());
            }
        }
        for (DatabaseType type : DatabaseType.values()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException("Missing database tester for " + type);
            }
        }
        this.testers = Map.copyOf(byType);
    }

    public DatabaseConnectionTester.ConnectionTestResult test(
            DatabaseType type, vn.gov.tax.dataplatform.source.DatabaseConnectionRequest request) {
        DatabaseConnectionTester tester = testers.get(type);
        if (tester == null) {
            throw new IllegalArgumentException("Unsupported database type: " + type);
        }
        return tester.test(request);
    }
}
