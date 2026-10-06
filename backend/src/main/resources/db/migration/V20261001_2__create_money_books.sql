CREATE TABLE money_books (
    money_book_uid BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_user_uid BIGINT NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_books_owner FOREIGN KEY (owner_user_uid) REFERENCES users (user_uid)
);

CREATE INDEX idx_money_books_owner_user_uid ON money_books (owner_user_uid);

CREATE TABLE money_book_users (
    money_book_user_uid BIGSERIAL PRIMARY KEY,
    money_book_uid BIGINT NOT NULL,
    user_uid BIGINT NOT NULL,
    is_admin BOOLEAN NOT NULL,
    can_create BOOLEAN NOT NULL,
    can_read BOOLEAN NOT NULL,
    can_update BOOLEAN NOT NULL,
    can_delete BOOLEAN NOT NULL,
    invitation_status VARCHAR(30) NOT NULL,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_money_book_users_book FOREIGN KEY (money_book_uid) REFERENCES money_books (money_book_uid),
    CONSTRAINT fk_money_book_users_user FOREIGN KEY (user_uid) REFERENCES users (user_uid),
    CONSTRAINT uq_money_book_users_book_user UNIQUE (money_book_uid, user_uid),
    CONSTRAINT chk_money_book_users_invitation_status
        CHECK (invitation_status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE INDEX idx_money_book_users_access
    ON money_book_users (user_uid, invitation_status, can_read, money_book_uid DESC);
