-- BookTalk P0 完整初始化脚本（MySQL 8.0+）
-- 默认管理员仅用于本地演示：admin / BookTalk@123，首次登录后应立即修改。

CREATE DATABASE IF NOT EXISTS book_talk
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE book_talk;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone (phone),
    KEY idx_user_status_created (status, create_time),
    CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_user_role CHECK (role IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号';

CREATE TABLE IF NOT EXISTS user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(50) NULL,
    avatar_url VARCHAR(500) NULL,
    background VARCHAR(500) NULL,
    gender CHAR(1) NOT NULL DEFAULT 'O',
    birthday DATE NULL,
    region VARCHAR(100) NULL,
    signature VARCHAR(255) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_info_user (user_id),
    CONSTRAINT fk_user_info_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_info_gender CHECK (gender IN ('M', 'F', 'O'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料';

CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类';

CREATE TABLE IF NOT EXISTS book (
    id BIGINT NOT NULL,
    isbn VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    sub_title VARCHAR(255) NULL,
    original_title VARCHAR(255) NULL,
    description TEXT NULL,
    author VARCHAR(255) NULL,
    author_country VARCHAR(100) NULL,
    translator VARCHAR(255) NULL,
    series VARCHAR(255) NULL,
    publisher VARCHAR(255) NULL,
    producer VARCHAR(255) NULL,
    publish_date DATE NULL,
    price DECIMAL(10,2) NULL,
    cover_url VARCHAR(500) NULL,
    page_count INT NULL,
    binding_type VARCHAR(100) NULL,
    category_id BIGINT NULL,
    average_score DECIMAL(4,2) NOT NULL DEFAULT 0,
    stars5_top DECIMAL(5,2) NOT NULL DEFAULT 0,
    stars4_top DECIMAL(5,2) NOT NULL DEFAULT 0,
    stars3_top DECIMAL(5,2) NOT NULL DEFAULT 0,
    stars2_top DECIMAL(5,2) NOT NULL DEFAULT 0,
    stars1_top DECIMAL(5,2) NOT NULL DEFAULT 0,
    score_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    hot_score DECIMAL(12,4) NOT NULL DEFAULT 0,
    hot_score_update_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn),
    KEY idx_book_category_score (category_id, average_score, favorite_count),
    KEY idx_book_hot_created (hot_score, create_time),
    KEY idx_book_author (author),
    KEY idx_book_created (create_time),
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL,
    CONSTRAINT chk_book_score CHECK (average_score BETWEEN 0 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书';

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    name VARCHAR(50) NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    description TEXT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name),
    KEY idx_tag_category_usage (category_id, usage_count),
    CONSTRAINT fk_tag_creator FOREIGN KEY (creator_id) REFERENCES `user` (id),
    CONSTRAINT fk_tag_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共标签';

CREATE TABLE IF NOT EXISTS book_tag_relation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_tag (book_id, tag_id),
    KEY idx_book_tag_tag (tag_id, book_id),
    CONSTRAINT fk_book_tag_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
    CONSTRAINT fk_book_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书标签关系';

CREATE TABLE IF NOT EXISTS book_review (
    id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL DEFAULT 0,
    title VARCHAR(100) NULL,
    content TEXT NOT NULL,
    score TINYINT NULL,
    like_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    hot_score DECIMAL(12,4) NOT NULL DEFAULT 0,
    hot_score_update_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_review_book_time (book_id, create_time),
    KEY idx_review_book_score (book_id, score),
    KEY idx_review_user_time (user_id, create_time),
    KEY idx_review_hot (hot_score, create_time),
    CONSTRAINT fk_review_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT chk_review_type CHECK (type IN (0, 1)),
    CONSTRAINT chk_review_score CHECK (score IS NULL OR score BETWEEN 1 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书评';

CREATE TABLE IF NOT EXISTS post (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    summary VARCHAR(255) NULL,
    related_book_id BIGINT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    hot_score DECIMAL(12,4) NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    last_active_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    hot_score_update_time DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_post_user_status_time (user_id, status, create_time),
    KEY idx_post_status_hot (status, hot_score, last_active_time),
    KEY idx_post_book_time (related_book_id, create_time),
    CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_book FOREIGN KEY (related_book_id) REFERENCES book (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='讨论帖';

CREATE TABLE IF NOT EXISTS book_shelf (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WANT_TO_READ',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_shelf_user_book (user_id, book_id),
    KEY idx_shelf_user_status_time (user_id, status, update_time),
    KEY idx_shelf_book_status (book_id, status),
    CONSTRAINT fk_shelf_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_shelf_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
    CONSTRAINT chk_shelf_status CHECK (status IN ('WANT_TO_READ', 'READING', 'READ'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人书架';

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT NOT NULL,
    root_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    parent_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_comment_target_time (target_type, root_id, create_time),
    KEY idx_comment_parent_time (parent_id, create_time),
    KEY idx_comment_user_time (user_id, create_time),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (id) ON DELETE CASCADE,
    CONSTRAINT chk_comment_target CHECK (target_type IN ('BOOK', 'REVIEW', 'POST', 'COMMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一评论';

CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_like_user_target (user_id, target_type, target_id),
    KEY idx_like_target_time (target_type, target_id, create_time),
    KEY idx_like_user_time (user_id, create_time),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT chk_like_target CHECK (target_type IN ('REVIEW', 'POST', 'COMMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录';

CREATE TABLE IF NOT EXISTS content_tag_relation (
    id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_tag (content_type, content_id, tag_id),
    KEY idx_content_tag_target (content_type, content_id),
    KEY idx_content_tag_tag (tag_id, content_type),
    CONSTRAINT fk_content_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
    CONSTRAINT chk_content_tag_type CHECK (content_type IN ('REVIEW', 'POST'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容标签关系';

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NULL,
    target_id BIGINT NULL,
    target_type VARCHAR(20) NULL,
    sender_id BIGINT NULL,
    sender_name VARCHAR(50) NULL,
    sender_avatar VARCHAR(500) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notification_user_read_time (user_id, is_deleted, is_read, create_time),
    KEY idx_notification_user_time (user_id, create_time),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES `user` (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知';

CREATE TABLE IF NOT EXISTS user_behavior_log (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    behavior_type VARCHAR(50) NOT NULL,
    behavior_score DECIMAL(10,4) NOT NULL DEFAULT 1,
    extra_data JSON NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_behavior_user_time (user_id, create_time),
    KEY idx_behavior_target_time (target_type, target_id, create_time),
    KEY idx_behavior_type_time (behavior_type, create_time),
    CONSTRAINT fk_behavior_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为日志';

CREATE TABLE IF NOT EXISTS user_interest_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    interest_type VARCHAR(32) NOT NULL,
    interest_key VARCHAR(255) NOT NULL,
    interest_score DECIMAL(10,4) NOT NULL DEFAULT 0,
    behavior_count INT NOT NULL DEFAULT 0,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_interest (user_id, interest_type, interest_key),
    KEY idx_interest_user_score (user_id, interest_score),
    KEY idx_interest_type_key (interest_type, interest_key),
    CONSTRAINT fk_interest_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT chk_interest_type CHECK (interest_type IN ('TAG', 'CATEGORY', 'AUTHOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣画像';

INSERT IGNORE INTO `user` (id, username, password, email, status, role)
VALUES (1, 'admin', '$2a$10$2HVhKJhJVbO5Jar1x0qy9.rH51DfoF3yCFb2Eu7wFIT2YdWHrbOQC', 'admin@booktalk.local', 1, 'admin');

INSERT INTO user_info (user_id, nickname, gender, signature)
VALUES (1, 'BookTalk Admin', 'O', 'Local demo administrator')
ON DUPLICATE KEY UPDATE
    nickname = 'BookTalk Admin',
    gender = 'O',
    signature = 'Local demo administrator';

INSERT IGNORE INTO category (id, name, description) VALUES
    (1, '文学', '小说、诗歌、散文等文学作品'),
    (2, '科技', '计算机、工程与自然科学'),
    (3, '人文社科', '历史、哲学、社会科学'),
    (4, '生活', '生活方式、心理与成长'),
    (5, '少儿', '儿童文学与科普读物');
