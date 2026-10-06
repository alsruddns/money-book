ALTER TABLE user_auth ADD COLUMN security_question_code VARCHAR(40);
ALTER TABLE user_auth ADD COLUMN security_answer_hash VARCHAR(100);
ALTER TABLE user_auth ADD COLUMN verified_email VARCHAR(254);
ALTER TABLE user_auth ADD COLUMN email_verified_at TIMESTAMP;
ALTER TABLE user_auth ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uq_user_auth_verified_email ON user_auth (verified_email);

CREATE TABLE password_recovery_codes (
    recovery_code_uid BIGSERIAL PRIMARY KEY,
    user_uid BIGINT NOT NULL REFERENCES users(user_uid),
    code_hash CHAR(64) NOT NULL UNIQUE,
    used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_recovery_user_active ON password_recovery_codes(user_uid, used_at, revoked_at);

CREATE TABLE account_email_verifications (
    email_verification_uid BIGSERIAL PRIMARY KEY,
    user_uid BIGINT REFERENCES users(user_uid),
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    grant_hash CHAR(64),
    grant_expires_at TIMESTAMP,
    grant_consumed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    resend_after TIMESTAMP NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    consumed_at TIMESTAMP,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_email_verification_purpose CHECK (purpose IN ('SIGNUP', 'ACCOUNT_EMAIL', 'PASSWORD_RESET')),
    CONSTRAINT chk_email_verification_attempts CHECK (attempt_count BETWEEN 0 AND 5)
);
CREATE INDEX idx_email_verification_lookup ON account_email_verifications(email, purpose, reg_time DESC);
CREATE INDEX idx_email_verification_grant ON account_email_verifications(grant_hash, purpose);
