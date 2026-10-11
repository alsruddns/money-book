package com.moneybook.backend.recovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationGrantHashMigrationTests {
    @Test
    void migrationPreservesGrantHashAndNullWhileConvertingToVarchar() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:email_verification_grant_hash_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE account_email_verifications (grant_hash CHAR(64))");
                statement.execute("CREATE INDEX idx_email_verification_grant " +
                        "ON account_email_verifications(grant_hash)");
                statement.execute("INSERT INTO account_email_verifications(grant_hash) " +
                        "VALUES ('0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'), (NULL)");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261011_3__normalize_email_verification_grant_hash.sql"));

            try (var result = connection.createStatement()
                    .executeQuery("SELECT grant_hash FROM account_email_verifications ORDER BY grant_hash NULLS LAST")) {
                assertTrue(result.next());
                assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", result.getString(1));
                assertTrue(result.next());
                assertNull(result.getString(1));
            }
            try (var columns = connection.getMetaData().getColumns(null, null,
                    "ACCOUNT_EMAIL_VERIFICATIONS", "GRANT_HASH")) {
                assertTrue(columns.next());
                assertEquals("CHARACTER VARYING", columns.getString("TYPE_NAME"));
                assertEquals(64, columns.getInt("COLUMN_SIZE"));
            }
            try (var indexes = connection.getMetaData().getIndexInfo(null, null,
                    "ACCOUNT_EMAIL_VERIFICATIONS", false, false)) {
                assertTrue(indexes.next());
                assertEquals("IDX_EMAIL_VERIFICATION_GRANT", indexes.getString("INDEX_NAME"));
            }
        }
    }
}
