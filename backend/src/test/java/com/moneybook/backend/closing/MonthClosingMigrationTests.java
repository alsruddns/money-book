package com.moneybook.backend.closing;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MonthClosingMigrationTests {
    @Test
    void closingMigrationEnforcesBookMonthUniquenessAndChecks() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:month_closing_migration;MODE=PostgreSQL", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO users (user_uid) VALUES (1)");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261001_2__create_money_books.sql"));
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261002_6__create_money_book_month_closings.sql"));
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO money_books (money_book_uid, name, owner_user_uid, reg_time, mod_time)
                        VALUES (1, 'home', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute(closingSql(10, "100"));
                assertThrows(SQLException.class, () -> statement.execute(closingSql(10, "100")));
                assertThrows(SQLException.class, () -> statement.execute(closingSql(13, "100")));
                assertThrows(SQLException.class, () -> statement.execute(closingSql(11, "-1")));
            }
        }
    }

    private String closingSql(int month, String income) {
        return "INSERT INTO money_book_month_closings (money_book_uid, \"year\", \"month\", income, expense, "
                + "transaction_count, previous_income, previous_expense, budget_configured, closed_by_user_uid, "
                + "closed_at, reg_time, mod_time) VALUES (1, 2026, " + month + ", " + income
                + ", 0, 0, 0, 0, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }
}
