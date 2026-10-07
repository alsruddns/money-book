CREATE TABLE board_categories (
    category_uid BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(300),
    display_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE board_posts (
    post_uid BIGSERIAL PRIMARY KEY,
    category_uid BIGINT NOT NULL REFERENCES board_categories(category_uid),
    author_user_uid BIGINT NOT NULL REFERENCES users(user_uid),
    title VARCHAR(200) NOT NULL,
    content VARCHAR(20000) NOT NULL,
    is_secret BOOLEAN NOT NULL DEFAULT FALSE,
    is_notice BOOLEAN NOT NULL DEFAULT FALSE,
    view_count BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_board_posts_category_latest ON board_posts(category_uid, is_deleted, reg_time DESC, post_uid DESC);
CREATE INDEX idx_board_posts_notice_latest ON board_posts(is_notice DESC, is_deleted, reg_time DESC, post_uid DESC);
CREATE INDEX idx_board_posts_author ON board_posts(author_user_uid, reg_time DESC);

CREATE TABLE board_comments (
    comment_uid BIGSERIAL PRIMARY KEY,
    post_uid BIGINT NOT NULL REFERENCES board_posts(post_uid),
    author_user_uid BIGINT NOT NULL REFERENCES users(user_uid),
    parent_comment_uid BIGINT REFERENCES board_comments(comment_uid),
    content VARCHAR(5000) NOT NULL,
    is_secret BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    reg_r_id BIGINT,
    reg_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_r_id BIGINT,
    mod_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_board_comments_post_parent_time ON board_comments(post_uid, parent_comment_uid, reg_time, comment_uid);
CREATE INDEX idx_board_comments_author ON board_comments(author_user_uid, reg_time DESC);

INSERT INTO board_categories(name, description, display_order) VALUES
('자유', NULL, 1), ('기능 요청', NULL, 2), ('개선 제안', NULL, 3), ('버그 신고', NULL, 4), ('기타', NULL, 5);
