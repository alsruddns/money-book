CREATE TABLE holidays (
    holiday_uid BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    holiday_type VARCHAR(30),
    is_holiday BOOLEAN NOT NULL,
    source VARCHAR(30) NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT uq_holidays_date_name UNIQUE (holiday_date, name)
);

CREATE TABLE holiday_sync_status (
    "year" INTEGER PRIMARY KEY,
    last_attempt_at TIMESTAMP NOT NULL,
    last_synced_at TIMESTAMP,
    CONSTRAINT chk_holiday_sync_status_year CHECK ("year" BETWEEN 1 AND 9999)
);
