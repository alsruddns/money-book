package com.moneybook.backend.recovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationCodeHashMigrationTests {
    @Test
    void migrationConvertsFixedCharToVarcharWithoutChangingHash() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:email_verification_code_hash_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE account_email_verifications (code_hash CHAR(64) NOT NULL)");
                statement.execute("INSERT INTO account_email_verifications(code_hash) VALUES ('0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef')");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261011_2__normalize_email_verification_code_hash.sql"));

            try (var result = connection.createStatement()
                    .executeQuery("SELECT code_hash FROM account_email_verifications")) {
                assertTrue(result.next());
                assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", result.getString(1));
            }
            try (var columns = connection.getMetaData().getColumns(null, null,
                    "ACCOUNT_EMAIL_VERIFICATIONS", "CODE_HASH")) {
                assertTrue(columns.next());
                assertEquals("CHARACTER VARYING", columns.getString("TYPE_NAME"));
                assertEquals(64, columns.getInt("COLUMN_SIZE"));
            }
        }
    }
}
