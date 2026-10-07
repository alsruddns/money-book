package com.moneybook.backend.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class SystemRoleMigrationTests {
    @Test void existingUsersDefaultToUserAndDatabaseRejectsUnknownRoles() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:system_role_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY, status VARCHAR(30) NOT NULL)");
                statement.execute("INSERT INTO users VALUES (1,'ACTIVE'),(2,'BLOCKED')");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261002_9__add_system_role_to_users.sql"));
            try (var rows = connection.createStatement().executeQuery("SELECT system_role FROM users ORDER BY user_uid")) {
                assertTrue(rows.next()); assertEquals("USER", rows.getString(1));
                assertTrue(rows.next()); assertEquals("USER", rows.getString(1));
            }
            assertThrows(java.sql.SQLException.class, () -> connection.createStatement().executeUpdate(
                    "UPDATE users SET system_role='ROOT' WHERE user_uid=1"));
        }
    }
}
