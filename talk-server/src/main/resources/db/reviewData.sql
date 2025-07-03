

#书评基本表
CREATE TABLE book_review (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
                             book_id BIGINT NOT NULL COMMENT '图书ID',
                             user_id BIGINT NOT NULL COMMENT '评论用户ID',
                             type TINYINT NOT NULL DEFAULT 0 COMMENT '评论类型：0-短评，1-长评',
                             title VARCHAR(100) DEFAULT NULL COMMENT '长评标题（短评可为空）',
                             content TEXT NOT NULL COMMENT '评论内容（支持 markdown）',
                             score TINYINT DEFAULT NULL COMMENT '评分（1-10），可选',
                             like_count INT DEFAULT 0 COMMENT '点赞数',
                             reply_count INT DEFAULT 0 COMMENT '回复数',
                             status TINYINT DEFAULT 1 COMMENT '状态：1-待审核，2-已通过，0-驳回/屏蔽',
                             audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
                             create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                             update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             INDEX idx_book_id (book_id),
                             INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评论表';


#书评评论表
CREATE TABLE book_review_reply (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '回复ID',
                                   review_id BIGINT NOT NULL COMMENT '主评论ID',
                                   user_id BIGINT NOT NULL COMMENT '回复人ID',
                                   content TEXT NOT NULL COMMENT '回复内容',
                                   create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书评回复表';


#书评点赞表
CREATE TABLE book_review_like (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                                  review_id BIGINT NOT NULL COMMENT '评论ID',
                                  user_id BIGINT NOT NULL COMMENT '用户ID',
                                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  UNIQUE KEY uk_user_review (user_id, review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书评点赞表';


#书评审核表
CREATE TABLE book_review_audit_log (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                                       review_id BIGINT NOT NULL COMMENT '评论ID',
                                       admin_id BIGINT NOT NULL COMMENT '审核管理员ID',
                                       result TINYINT NOT NULL COMMENT '审核结果：0-驳回，1-通过',
                                       remark VARCHAR(255) COMMENT '审核备注',
                                       audit_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论审核日志表';
