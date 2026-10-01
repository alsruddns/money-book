CREATE TABLE money_book_categories (
    category_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    sort_order INTEGER NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_book_categories_book FOREIGN KEY (money_book_uid) REFERENCES money_books (money_book_uid),
    CONSTRAINT uq_money_book_categories_book_type_name UNIQUE (money_book_uid, transaction_type, name),
    CONSTRAINT uq_money_book_categories_uid_book_type UNIQUE (category_uid, money_book_uid, transaction_type),
    CONSTRAINT chk_money_book_categories_type CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_money_book_categories_sort CHECK (sort_order >= 0)
);

CREATE TABLE money_book_accounts (
    account_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    sort_order INTEGER NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_book_accounts_book FOREIGN KEY (money_book_uid) REFERENCES money_books (money_book_uid),
    CONSTRAINT uq_money_book_accounts_book_name UNIQUE (money_book_uid, name),
    CONSTRAINT uq_money_book_accounts_uid_book UNIQUE (account_uid, money_book_uid),
    CONSTRAINT chk_money_book_accounts_type CHECK (account_type IN ('CASH', 'BANK', 'CARD', 'ETC')),
    CONSTRAINT chk_money_book_accounts_sort CHECK (sort_order >= 0)
);

CREATE TABLE money_book_transactions (
    transaction_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    category_uid BIGINT NOT NULL,
    account_uid BIGINT NOT NULL,
    memo VARCHAR(500),
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_book_transactions_book FOREIGN KEY (money_book_uid) REFERENCES money_books (money_book_uid),
    CONSTRAINT fk_money_book_transactions_category_book_type
        FOREIGN KEY (category_uid, money_book_uid, transaction_type)
        REFERENCES money_book_categories (category_uid, money_book_uid, transaction_type),
    CONSTRAINT fk_money_book_transactions_account_book FOREIGN KEY (account_uid, money_book_uid)
        REFERENCES money_book_accounts (account_uid, money_book_uid),
    CONSTRAINT chk_money_book_transactions_type CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_money_book_transactions_amount CHECK (amount > 0)
);

CREATE INDEX idx_money_book_transactions_month
    ON money_book_transactions (money_book_uid, transaction_date DESC, transaction_uid DESC);

CREATE INDEX idx_money_book_transactions_category ON money_book_transactions (category_uid);
CREATE INDEX idx_money_book_transactions_account ON money_book_transactions (account_uid);
