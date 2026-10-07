CREATE TABLE money_book_recurring_transactions (
    recurring_transaction_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    category_uid BIGINT NOT NULL,
    account_uid BIGINT NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    day_of_month INTEGER,
    day_of_week INTEGER,
    start_date DATE NOT NULL,
    end_date DATE,
    memo VARCHAR(500),
    is_active BOOLEAN NOT NULL,
    last_generated_date DATE,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_recurring_book FOREIGN KEY (money_book_uid) REFERENCES money_books (money_book_uid),
    CONSTRAINT fk_recurring_category_book_type
        FOREIGN KEY (category_uid, money_book_uid, transaction_type)
        REFERENCES money_book_categories (category_uid, money_book_uid, transaction_type),
    CONSTRAINT fk_recurring_account_book
        FOREIGN KEY (account_uid, money_book_uid)
        REFERENCES money_book_accounts (account_uid, money_book_uid),
    CONSTRAINT chk_recurring_transaction_type CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_recurring_amount CHECK (amount > 0),
    CONSTRAINT chk_recurring_date_range CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_recurring_frequency_days CHECK (
        (frequency = 'MONTHLY' AND day_of_month IS NOT NULL
            AND day_of_month BETWEEN 1 AND 31 AND day_of_week IS NULL)
        OR (frequency = 'WEEKLY' AND day_of_week IS NOT NULL
            AND day_of_week BETWEEN 1 AND 7 AND day_of_month IS NULL)
    )
);

-- Generated transactions retain their source UID after a rule is deleted.
-- A foreign key to the rule is intentionally omitted so historical transactions remain intact.
ALTER TABLE money_book_transactions ADD COLUMN recurring_transaction_uid BIGINT;
ALTER TABLE money_book_transactions ADD COLUMN scheduled_date DATE;
ALTER TABLE money_book_transactions
    ADD CONSTRAINT chk_transaction_occurrence_pair CHECK (
        (recurring_transaction_uid IS NULL AND scheduled_date IS NULL)
        OR (recurring_transaction_uid IS NOT NULL AND scheduled_date IS NOT NULL)
    );
ALTER TABLE money_book_transactions
    ADD CONSTRAINT uq_transaction_recurring_occurrence
        UNIQUE (recurring_transaction_uid, scheduled_date);

CREATE INDEX idx_recurring_book_active
    ON money_book_recurring_transactions (money_book_uid, is_active);
CREATE INDEX idx_recurring_category ON money_book_recurring_transactions (category_uid);
CREATE INDEX idx_recurring_account ON money_book_recurring_transactions (account_uid);
