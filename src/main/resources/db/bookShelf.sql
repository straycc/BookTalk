#书架数据库表
CREATE TABLE book_shelf (
                            id BIGINT PRIMARY KEY,
                            user_id BIGINT NOT NULL COMMENT '用户ID',
                            book_id BIGINT NOT NULL COMMENT '书籍ID',
                            status VARCHAR(20) NOT NULL DEFAULT 'WANT_TO_READ' COMMENT '状态: WANT_TO_READ-想读, READING-在读, READ-读完',
                            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            INDEX idx_user_id (user_id),
                            INDEX idx_book_id (book_id),
                            INDEX idx_status (status),
                            UNIQUE KEY uk_user_book (user_id, book_id)
);