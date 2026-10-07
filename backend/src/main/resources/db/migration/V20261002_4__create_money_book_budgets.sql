CREATE TABLE money_book_budgets (
    budget_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL REFERENCES money_books (money_book_uid),
    "year" INTEGER NOT NULL,
    "month" INTEGER NOT NULL,
    total_budget NUMERIC(19, 2),
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT uq_money_book_budgets_book_month UNIQUE (money_book_uid, "year", "month"),
    CONSTRAINT chk_money_book_budgets_year CHECK ("year" BETWEEN 1 AND 9999),
    CONSTRAINT chk_money_book_budgets_month CHECK ("month" BETWEEN 1 AND 12),
    CONSTRAINT chk_money_book_budgets_total CHECK (total_budget IS NULL OR total_budget >= 0)
);

CREATE TABLE money_book_category_budgets (
    category_budget_uid BIGSERIAL PRIMARY KEY,
    budget_uid BIGINT NOT NULL REFERENCES money_book_budgets (budget_uid) ON DELETE CASCADE,
    category_uid BIGINT NOT NULL REFERENCES money_book_categories (category_uid),
    amount NUMERIC(19, 2) NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT uq_money_book_category_budgets_budget_category UNIQUE (budget_uid, category_uid),
    CONSTRAINT chk_money_book_category_budgets_amount CHECK (amount >= 0)
);

CREATE INDEX idx_money_book_category_budgets_category
    ON money_book_category_budgets (category_uid);
