ALTER TABLE money_book_categories
    ADD COLUMN category_level VARCHAR(10) NOT NULL DEFAULT 'LARGE',
    ADD COLUMN parent_category_uid BIGINT,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE money_book_categories
    ADD CONSTRAINT chk_money_book_categories_level
        CHECK (category_level IN ('LARGE', 'MEDIUM', 'SMALL')),
    ADD CONSTRAINT chk_money_book_categories_parent_level
        CHECK ((category_level = 'LARGE' AND parent_category_uid IS NULL)
            OR (category_level IN ('MEDIUM', 'SMALL') AND parent_category_uid IS NOT NULL)),
    ADD CONSTRAINT fk_money_book_categories_parent
        FOREIGN KEY (parent_category_uid, money_book_uid, transaction_type)
        REFERENCES money_book_categories (category_uid, money_book_uid, transaction_type);

CREATE INDEX idx_money_book_categories_parent
    ON money_book_categories (parent_category_uid, sort_order, category_uid);
