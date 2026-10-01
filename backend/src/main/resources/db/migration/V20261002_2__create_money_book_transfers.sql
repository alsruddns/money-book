CREATE TABLE money_book_transfers (
    transfer_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    from_account_uid BIGINT NOT NULL,
    to_account_uid BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transfer_date DATE NOT NULL,
    memo VARCHAR(500),
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_book_transfers_book FOREIGN KEY (money_book_uid)
        REFERENCES money_books (money_book_uid),
    CONSTRAINT fk_money_book_transfers_from_book FOREIGN KEY (from_account_uid, money_book_uid)
        REFERENCES money_book_accounts (account_uid, money_book_uid),
    CONSTRAINT fk_money_book_transfers_to_book FOREIGN KEY (to_account_uid, money_book_uid)
        REFERENCES money_book_accounts (account_uid, money_book_uid),
    CONSTRAINT chk_money_book_transfers_amount CHECK (amount > 0),
    CONSTRAINT chk_money_book_transfers_distinct_accounts CHECK (from_account_uid <> to_account_uid)
);

CREATE INDEX idx_money_book_transfers_month
    ON money_book_transfers (money_book_uid, transfer_date DESC, transfer_uid DESC);
CREATE INDEX idx_money_book_transfers_from_account ON money_book_transfers (from_account_uid);
CREATE INDEX idx_money_book_transfers_to_account ON money_book_transfers (to_account_uid);
