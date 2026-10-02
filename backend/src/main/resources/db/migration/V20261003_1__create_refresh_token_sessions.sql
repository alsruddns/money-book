CREATE TABLE refresh_token_sessions (
    refresh_session_uid BIGSERIAL PRIMARY KEY,
    user_uid BIGINT NOT NULL,
    session_key VARCHAR(36) NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    user_agent VARCHAR(500),
    ip_address VARCHAR(100),
    last_used_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoke_reason VARCHAR(30),
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_refresh_token_sessions_user FOREIGN KEY (user_uid) REFERENCES users (user_uid),
    CONSTRAINT uq_refresh_token_sessions_key UNIQUE (session_key)
);

CREATE INDEX idx_refresh_token_sessions_user_active
    ON refresh_token_sessions (user_uid, revoked_at, expires_at);
