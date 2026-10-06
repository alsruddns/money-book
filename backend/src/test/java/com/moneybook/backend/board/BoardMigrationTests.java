package com.moneybook.backend.board;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardMigrationTests {
    @Test
    void boardMigrationCreatesSeedCategoriesAndEnforcesForeignKeys() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:board_migration;MODE=PostgreSQL", "sa", "")) {
            connection.createStatement().execute("CREATE TABLE users (user_uid BIGINT PRIMARY KEY)");
            connection.createStatement().execute("INSERT INTO users(user_uid) VALUES (1)");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261004_1__create_board_tables.sql"));
            try (ResultSet result = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM board_categories WHERE is_active=TRUE")) {
                result.next();
                assertEquals(5, result.getInt(1));
            }
            connection.createStatement().execute("""
                    INSERT INTO board_posts(category_uid, author_user_uid, title, content)
                    VALUES (1, 1, 'title', 'body')
                    """);
            assertThrows(Exception.class, () -> connection.createStatement().execute("""
                    INSERT INTO board_posts(category_uid, author_user_uid, title, content)
                    VALUES (999, 1, 'invalid category', 'body')
                    """));
        }
    }
}
