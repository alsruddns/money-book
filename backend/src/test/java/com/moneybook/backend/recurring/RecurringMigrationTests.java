package com.moneybook.backend.recurring;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RecurringMigrationTests {
    @Test
    void scheduleChecksAndOccurrenceUniqueConstraintApply() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:recurring_migration;MODE=PostgreSQL", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO users (user_uid) VALUES (1)");
            }
            for (String migration : new String[] {
                    "V20261001_2__create_money_books.sql",
                    "V20261002_1__create_ledger_tables.sql",
                    "V20261002_2__create_money_book_transfers.sql",
                    "V20261002_3__create_recurring_transactions.sql"}) {
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
                statement.execute("""
                        INSERT INTO money_book_accounts
                            (account_uid, money_book_uid, name, account_type, sort_order, reg_time, mod_time)
                        VALUES (1, 1, 'cash', 'CASH', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute(ruleSql("MONTHLY", "31", "NULL", "2026-01-01", "NULL"));
                assertThrows(SQLException.class,
                        () -> statement.execute(ruleSql("MONTHLY", "NULL", "NULL", "2026-01-01", "NULL")));
                assertThrows(SQLException.class,
                        () -> statement.execute(ruleSql("WEEKLY", "NULL", "8", "2026-01-01", "NULL")));
                assertThrows(SQLException.class,
                        () -> statement.execute(ruleSql("MONTHLY", "31", "NULL", "2026-02-01", "2026-01-01")));
                statement.execute(transactionSql("1", "DATE '2026-02-28'"));
                assertThrows(SQLException.class,
                        () -> statement.execute(transactionSql("1", "DATE '2026-02-28'")));
                assertThrows(SQLException.class,
                        () -> statement.execute(transactionSql("NULL", "DATE '2026-02-28'")));
                statement.execute(transactionSql("NULL", "NULL"));
                statement.execute(transactionSql("NULL", "NULL"));
            }
        }
    }

    private String ruleSql(String frequency, String dayOfMonth, String dayOfWeek,
                           String startDate, String endDate) {
        return "INSERT INTO money_book_recurring_transactions "
                + "(money_book_uid, transaction_type, amount, category_uid, account_uid, frequency, "
                + "day_of_month, day_of_week, start_date, end_date, is_active, reg_time, mod_time) VALUES "
                + "(1, 'EXPENSE', 10, 1, 1, '" + frequency + "', " + dayOfMonth + ", " + dayOfWeek
                + ", DATE '" + startDate + "', " + (endDate.equals("NULL") ? "NULL" : "DATE '" + endDate + "'")
                + ", TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }

    private String transactionSql(String ruleUid, String scheduledDate) {
        return "INSERT INTO money_book_transactions "
                + "(money_book_uid, transaction_type, amount, transaction_date, category_uid, account_uid, "
                + "recurring_transaction_uid, scheduled_date, reg_time, mod_time) VALUES "
                + "(1, 'EXPENSE', 10, DATE '2026-02-28', 1, 1, " + ruleUid + ", " + scheduledDate
                + ", CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }
}
