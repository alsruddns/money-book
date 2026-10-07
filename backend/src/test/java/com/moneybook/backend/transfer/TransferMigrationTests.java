package com.moneybook.backend.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferMigrationTests {
    @Test
    void databaseRejectsSameAccountNonPositiveAmountAndForeignBookAccount() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:transfer_migration;MODE=PostgreSQL", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO users (user_uid) VALUES (1)");
            }
            for (String migration : new String[] {
                    "V20261001_2__create_money_books.sql",
                    "V20261002_1__create_ledger_tables.sql",
                    "V20261002_2__create_money_book_transfers.sql"}) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/" + migration));
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO money_books (money_book_uid, name, owner_user_uid, reg_time, mod_time)
                        VALUES (1, 'first', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                               (2, 'second', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute("""
                        INSERT INTO money_book_accounts
                            (account_uid, money_book_uid, name, account_type, sort_order, reg_time, mod_time)
                        VALUES (1, 1, 'cash', 'CASH', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                               (2, 1, 'bank', 'BANK', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                               (3, 2, 'foreign', 'BANK', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute(sql(1, 2, 10));
                assertThrows(SQLException.class, () -> statement.execute(sql(1, 1, 10)));
                assertThrows(SQLException.class, () -> statement.execute(sql(1, 2, 0)));
                assertThrows(SQLException.class, () -> statement.execute(sql(1, 3, 10)));
                assertThrows(SQLException.class, () -> statement.execute(sql(3, 2, 10)));
            }
        }
    }

    private String sql(long from, long to, int amount) {
        return "INSERT INTO money_book_transfers "
                + "(money_book_uid, from_account_uid, to_account_uid, amount, transfer_date, reg_time, mod_time) "
                + "VALUES (1, " + from + ", " + to + ", " + amount
                + ", DATE '2026-10-02', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    }
}
