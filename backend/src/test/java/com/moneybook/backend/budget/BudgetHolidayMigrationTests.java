package com.moneybook.backend.budget;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetHolidayMigrationTests {
    @Test
    void budgetAndHolidayMigrationsEnforceKeysChecksAndAuditingColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:budget_holiday_migration;MODE=PostgreSQL", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO users (user_uid) VALUES (1)");
            }
            for (String migration : new String[] {
                    "V20261001_2__create_money_books.sql",
                    "V20261002_1__create_ledger_tables.sql",
                    "V20261002_4__create_money_book_budgets.sql",
                    "V20261002_5__create_holiday_cache.sql"}) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/" + migration));
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO money_books (money_book_uid, name, owner_user_uid, reg_time, mod_time)
                        VALUES (1, 'home', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute("""
                        INSERT INTO money_book_categories
                            (category_uid, money_book_uid, name, transaction_type, sort_order, reg_time, mod_time)
                        VALUES (1, 1, 'food', 'EXPENSE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute(budgetSql("10", "10"));
                assertThrows(SQLException.class, () -> statement.execute(budgetSql("10", "10")));
                assertThrows(SQLException.class, () -> statement.execute(budgetSql("13", "10")));
                assertThrows(SQLException.class, () -> statement.execute(budgetSql("11", "-1")));
                statement.execute(categorySql("1", "0"));
                assertThrows(SQLException.class, () -> statement.execute(categorySql("1", "0")));
                assertThrows(SQLException.class, () -> statement.execute(categorySql("999", "1")));
                assertThrows(SQLException.class, () -> statement.execute(categorySql("1", "-1")));
                statement.execute(holidaySql("개천절"));
                statement.execute(holidaySql("다른 특일"));
                assertThrows(SQLException.class, () -> statement.execute(holidaySql("개천절")));
                statement.execute("""
                        INSERT INTO holiday_sync_status ("year", last_attempt_at, last_synced_at)
                        VALUES (2026, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
            }
        }
    }

    private String budgetSql(String month, String amount) {
        return "INSERT INTO money_book_budgets (money_book_uid, \"year\", \"month\", total_budget, reg_time, mod_time) "
                + "VALUES (1, 2026, " + month + ", " + amount + ", CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }

    private String categorySql(String categoryUid, String amount) {
        return "INSERT INTO money_book_category_budgets (budget_uid, category_uid, amount, reg_time, mod_time) "
                + "VALUES (1, " + categoryUid + ", " + amount + ", CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }

    private String holidaySql(String name) {
        return "INSERT INTO holidays (holiday_date, name, is_holiday, source, reg_time, mod_time) "
                + "VALUES (DATE '2026-10-03', '" + name + "', TRUE, 'KASI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }
}
