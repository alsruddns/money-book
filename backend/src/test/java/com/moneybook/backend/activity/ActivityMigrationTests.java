package com.moneybook.backend.activity;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class ActivityMigrationTests {
    @Test
    void createsImmutableActivityTableAndLookupIndexes() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:activity_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
                statement.execute("CREATE TABLE money_books (money_book_uid BIGINT PRIMARY KEY)");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261002_8__create_money_book_activities.sql"));
            try (var rows = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME='MONEY_BOOK_ACTIVITIES'")) {
                assertTrue(rows.next());
                assertTrue(rows.getInt(1) >= 5);
            }
        }
    }
}
