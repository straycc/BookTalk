CREATE TABLE like_record (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                             target_id BIGINT NOT NULL COMMENT '被点赞的对象ID',
                             target_type ENUM('BOOK', 'REVIEW', 'BOOKLIST', 'NOTE', 'USER') NOT NULL COMMENT '点赞对象类型',
                             user_id BIGINT NOT NULL COMMENT '点赞用户ID',
                             create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                             UNIQUE KEY uniq_like (target_id, target_type, user_id) -- 防止同一用户对同一对象重复点赞
);
