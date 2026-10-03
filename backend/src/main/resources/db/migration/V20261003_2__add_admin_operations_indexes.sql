-- User activity timelines span money books, so actor UID must lead this index.
CREATE INDEX idx_money_book_activities_actor_latest
    ON money_book_activities (actor_user_uid, occurred_at DESC, activity_uid DESC);

-- Target-user audit filtering combines the target discriminator and UID before date ordering.
CREATE INDEX idx_system_admin_audit_target_user_latest
    ON system_admin_audit_logs (target_type, target_uid, occurred_at DESC, admin_audit_log_uid DESC);

-- The overview counts active sessions globally, while the existing index is user-leading.
-- Keep this as a portable full index; revoked rows are filtered by the query.
CREATE INDEX idx_refresh_token_sessions_active_expiry
    ON refresh_token_sessions (expires_at);
