CREATE TABLE notification (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                              user_id BIGINT NOT NULL COMMENT '接收用户ID',
                              type VARCHAR(20) NOT NULL COMMENT '通知类型: LIKE, COMMENT, REPLY, FOLLOW, SYSTEM',
                              title VARCHAR(100) NOT NULL COMMENT '通知标题',
                              content TEXT COMMENT '通知内容',
                              target_id BIGINT COMMENT '目标ID (书评/评论ID)',
                              target_type VARCHAR(20) COMMENT '目标类型: BOOK_REVIEW, COMMENT, USER',
                              sender_id BIGINT COMMENT '发送者用户ID',
                              sender_name VARCHAR(50) COMMENT '发送者用户名',
                              sender_avatar VARCHAR(255) COMMENT '发送者头像',
                              is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
                              is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
                              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                              INDEX idx_user_id (user_id),
                              INDEX idx_type (type),
                              INDEX idx_create_time (create_time),
                              INDEX idx_is_read (is_read),
                              INDEX idx_user_read (user_id, is_read)
) COMMENT='通知表';