CREATE TABLE system_admin_audit_logs (
    admin_audit_log_uid BIGSERIAL PRIMARY KEY,
    actor_user_uid BIGINT NOT NULL,
    actor_nickname VARCHAR(50) NOT NULL,
    actor_system_role VARCHAR(30) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_uid BIGINT,
    summary VARCHAR(200) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_system_admin_audit_actor FOREIGN KEY (actor_user_uid)
        REFERENCES users (user_uid),
    CONSTRAINT chk_system_admin_audit_actor_role
        CHECK (actor_system_role IN ('SYSTEM_ADMIN', 'SUPER_ADMIN'))
);

CREATE INDEX idx_system_admin_audit_latest
    ON system_admin_audit_logs (occurred_at DESC, admin_audit_log_uid DESC);
CREATE INDEX idx_system_admin_audit_actor
    ON system_admin_audit_logs (actor_user_uid, occurred_at DESC);
CREATE INDEX idx_system_admin_audit_action
    ON system_admin_audit_logs (action_type, occurred_at DESC);
CREATE INDEX idx_system_admin_audit_target
    ON system_admin_audit_logs (target_type, occurred_at DESC);
