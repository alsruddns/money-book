CREATE TABLE money_book_month_closings (
    closing_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL REFERENCES money_books (money_book_uid),
    "year" INTEGER NOT NULL,
    "month" INTEGER NOT NULL,
    income NUMERIC(19, 2) NOT NULL,
    expense NUMERIC(19, 2) NOT NULL,
    transaction_count BIGINT NOT NULL,
    previous_income NUMERIC(19, 2) NOT NULL,
    previous_expense NUMERIC(19, 2) NOT NULL,
    budget_configured BOOLEAN NOT NULL,
    total_budget NUMERIC(19, 2),
    closed_by_user_uid BIGINT NOT NULL REFERENCES users (user_uid),
    closed_at TIMESTAMP NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT uq_money_book_month_closings_book_month UNIQUE (money_book_uid, "year", "month"),
    CONSTRAINT chk_money_book_month_closings_year CHECK ("year" BETWEEN 1 AND 9999),
    CONSTRAINT chk_money_book_month_closings_month CHECK ("month" BETWEEN 1 AND 12),
    CONSTRAINT chk_money_book_month_closings_totals CHECK (
        income >= 0 AND expense >= 0 AND transaction_count >= 0
        AND previous_income >= 0 AND previous_expense >= 0
        AND (total_budget IS NULL OR total_budget >= 0))
);
