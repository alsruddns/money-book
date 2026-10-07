ALTER TABLE users
    ADD COLUMN system_role VARCHAR(30) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_system_role
        CHECK (system_role IN ('USER', 'SYSTEM_ADMIN', 'SUPER_ADMIN'));

CREATE INDEX idx_users_system_role
    ON users (system_role);
