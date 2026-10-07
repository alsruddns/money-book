CREATE TABLE money_book_settings (
    money_book_setting_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL UNIQUE REFERENCES money_books (money_book_uid) ON DELETE CASCADE,
    week_start_day VARCHAR(10) NOT NULL DEFAULT 'SUNDAY',
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT chk_money_book_settings_week_start CHECK (week_start_day IN ('SUNDAY', 'MONDAY'))
);

INSERT INTO money_book_settings (money_book_uid, week_start_day, reg_time, mod_time)
SELECT money_book_uid, 'SUNDAY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM money_books;
