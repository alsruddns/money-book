package com.moneybook.backend.moneybook;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class MoneyBookSettingMigrationTests {
    @Test void createsSettingsAndBackfillsExistingBooksWithSunday() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:setting_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE money_books (money_book_uid BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO money_books VALUES (1),(2)");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261002_7__create_money_book_settings.sql"));
            try (var rows = connection.createStatement().executeQuery(
                    "SELECT money_book_uid, week_start_day FROM money_book_settings ORDER BY money_book_uid")) {
                assertTrue(rows.next()); assertEquals(1L, rows.getLong(1)); assertEquals("SUNDAY", rows.getString(2));
                assertTrue(rows.next()); assertEquals(2L, rows.getLong(1)); assertEquals("SUNDAY", rows.getString(2));
                assertFalse(rows.next());
            }
            assertThrows(java.sql.SQLException.class, () -> connection.createStatement().executeUpdate(
                    "INSERT INTO money_book_settings (money_book_uid,week_start_day,reg_time,mod_time) VALUES (1,'FRIDAY',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
        }
    }
}
