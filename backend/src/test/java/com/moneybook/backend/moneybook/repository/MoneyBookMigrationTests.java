package com.moneybook.backend.moneybook.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyBookMigrationTests {

    @Test
    void migrationCreatesAuditedTablesAndUniqueMembership() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:money_book_migration;MODE=PostgreSQL", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO users (user_uid) VALUES (42)");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261001_2__create_money_books.sql"));

            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO money_books
                            (money_book_uid, name, owner_user_uid, reg_time, mod_time)
                        VALUES (7, 'home', 42, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                statement.execute("""
                        INSERT INTO money_book_users
                            (money_book_uid, user_uid, is_admin, can_create, can_read,
                             can_update, can_delete, invitation_status, reg_time, mod_time)
                        VALUES (7, 42, TRUE, TRUE, TRUE, TRUE, TRUE, 'ACCEPTED',
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """);
                try (ResultSet result = statement.executeQuery("""
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME IN ('MONEY_BOOKS', 'MONEY_BOOK_USERS')
                          AND COLUMN_NAME IN ('REG_R_ID', 'REG_TIME', 'MOD_R_ID', 'MOD_TIME')
                        """)) {
                    result.next();
                    assertEquals(8, result.getInt(1));
                }
                assertThrows(SQLException.class, () -> statement.execute("""
                        INSERT INTO money_book_users
                            (money_book_uid, user_uid, is_admin, can_create, can_read,
                             can_update, can_delete, invitation_status, reg_time, mod_time)
                        VALUES (7, 42, TRUE, TRUE, TRUE, TRUE, TRUE, 'ACCEPTED',
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """));
            }
        }
    }
}
