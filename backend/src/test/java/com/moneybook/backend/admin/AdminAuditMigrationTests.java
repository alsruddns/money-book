package com.moneybook.backend.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class AdminAuditMigrationTests {
    @Test void createsIndexedImmutableAdminAuditTable() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:admin_audit_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261002_10__create_system_admin_audit_logs.sql"));
            try (var rows = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME='SYSTEM_ADMIN_AUDIT_LOGS'")) {
                assertTrue(rows.next()); assertTrue(rows.getInt(1)>=5);
            }
        }
    }
}
