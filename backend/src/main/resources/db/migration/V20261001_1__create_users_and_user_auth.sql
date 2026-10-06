CREATE TABLE users (
    user_uid BIGSERIAL PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    profile_image_url VARCHAR(500),
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'WITHDRAWN', 'BLOCKED'))
);

CREATE TABLE user_auth (
    user_auth_uid BIGSERIAL PRIMARY KEY,
    user_uid BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    login_id VARCHAR(100),
    password_hash VARCHAR(255),
    provider_user_id VARCHAR(255),
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_auth_user FOREIGN KEY (user_uid) REFERENCES users (user_uid),
    CONSTRAINT chk_user_auth_credentials CHECK (
        (provider = 'LOCAL' AND login_id IS NOT NULL AND password_hash IS NOT NULL AND provider_user_id IS NULL)
        OR (provider IN ('GOOGLE', 'KAKAO', 'NAVER')
            AND provider_user_id IS NOT NULL AND login_id IS NULL AND password_hash IS NULL)
    )
);

CREATE UNIQUE INDEX uq_user_auth_local_login_id ON user_auth (login_id) WHERE provider = 'LOCAL';
CREATE UNIQUE INDEX uq_user_auth_oauth_identity ON user_auth (provider, provider_user_id)
    WHERE provider IN ('GOOGLE', 'KAKAO', 'NAVER');

-- Supports linked-auth lookups and FK checks when deleting a user.
CREATE INDEX idx_user_auth_user_uid ON user_auth (user_uid);
