package com.moneybook.backend.recovery;

import org.junit.jupiter.api.Test;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordRecoveryHashMigrationTests {
    @Test
    void migrationPreservesCodeHashAndUniqueConstraintWhileConvertingToVarchar() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:password_recovery_hash_migration;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE password_recovery_codes (" +
                        "recovery_code_uid BIGSERIAL PRIMARY KEY, " +
                        "user_uid BIGINT NOT NULL, " +
                        "code_hash CHAR(64) NOT NULL UNIQUE, " +
                        "used_at TIMESTAMP, revoked_at TIMESTAMP, reg_r_id BIGINT, " +
                        "reg_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, mod_r_id BIGINT, " +
                        "mod_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                statement.execute("INSERT INTO password_recovery_codes(user_uid, code_hash) " +
                        "VALUES (1, '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef')");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261011_4__normalize_password_recovery_hash_columns.sql"));

            try (var result = connection.createStatement()
                    .executeQuery("SELECT code_hash FROM password_recovery_codes")) {
                assertTrue(result.next());
                assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", result.getString(1));
            }
            try (var columns = connection.getMetaData().getColumns(null, null,
                    "PASSWORD_RECOVERY_CODES", "CODE_HASH")) {
                assertTrue(columns.next());
                assertEquals("CHARACTER VARYING", columns.getString("TYPE_NAME"));
                assertEquals(64, columns.getInt("COLUMN_SIZE"));
            }
            assertThrows(Exception.class, () -> connection.createStatement().executeUpdate(
                    "INSERT INTO password_recovery_codes(code_hash) " +
                            "VALUES ('0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef')"));

            validatesEntitySchema();
        }
    }

    private void validatesEntitySchema() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url",
                        "jdbc:h2:mem:password_recovery_hash_migration;MODE=PostgreSQL")
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.hbm2ddl.auto", "validate")
                .build();
        try (var sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(com.moneybook.backend.entity.PasswordRecoveryCode.class)
                .buildMetadata()
                .buildSessionFactory()) {
            assertTrue(sessionFactory.isOpen());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
