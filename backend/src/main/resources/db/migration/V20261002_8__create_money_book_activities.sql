CREATE TABLE money_book_activities (
    activity_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    actor_user_uid BIGINT NOT NULL,
    actor_nickname VARCHAR(50) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_uid BIGINT,
    summary VARCHAR(200) NOT NULL,
    metadata_json VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_book_activities_book FOREIGN KEY (money_book_uid)
        REFERENCES money_books (money_book_uid) ON DELETE CASCADE,
    CONSTRAINT fk_money_book_activities_actor FOREIGN KEY (actor_user_uid)
        REFERENCES users (user_uid)
);

CREATE INDEX idx_money_book_activities_latest
    ON money_book_activities (money_book_uid, occurred_at DESC, activity_uid DESC);
CREATE INDEX idx_money_book_activities_actor
    ON money_book_activities (money_book_uid, actor_user_uid, occurred_at DESC);
CREATE INDEX idx_money_book_activities_type
    ON money_book_activities (money_book_uid, activity_type, occurred_at DESC);
CREATE INDEX idx_money_book_activities_target
    ON money_book_activities (money_book_uid, target_type, occurred_at DESC);
