
#书籍基本信息表
CREATE TABLE book (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图书ID',
                      title VARCHAR(255) NOT NULL COMMENT '书名',
                      author VARCHAR(100) DEFAULT NULL COMMENT '作者',
                      isbn VARCHAR(50) UNIQUE COMMENT 'ISBN编号',
                      cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
                      description TEXT COMMENT '图书简介',
                      category_id BIGINT DEFAULT NULL COMMENT '分类ID（单分类）',
                      average_score DECIMAL(3,1) DEFAULT 0 COMMENT '平均评分',
                      score_count INT DEFAULT 0 COMMENT '评分人数',
                      favorite_count INT DEFAULT 0 COMMENT '收藏人数',
                      create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书主表';


#书籍分类表
CREATE TABLE book_category (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
                               name VARCHAR(100) NOT NULL UNIQUE COMMENT '分类名称',
                               description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';


#书籍收藏表
CREATE TABLE book_favorite (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
                               user_id BIGINT NOT NULL COMMENT '用户ID',
                               book_id BIGINT NOT NULL COMMENT '图书ID',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                               UNIQUE KEY uk_user_book (user_id, book_id),
                               INDEX idx_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书收藏表';


#书籍评分表
CREATE TABLE book_list (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '书单ID',
                           user_id BIGINT NOT NULL COMMENT '创建者ID',
                           title VARCHAR(100) NOT NULL COMMENT '书单标题',
                           description TEXT COMMENT '书单简介',
                           cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图（可选）',
                           visibility TINYINT DEFAULT 1 COMMENT '是否公开（1公开/0私密）',
                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户书单表';


#书单表
CREATE TABLE book_list (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '书单ID',
                           user_id BIGINT NOT NULL COMMENT '创建者ID',
                           title VARCHAR(100) NOT NULL COMMENT '书单标题',
                           description TEXT COMMENT '书单简介',
                           cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图（可选）',
                           visibility TINYINT DEFAULT 1 COMMENT '是否公开（1公开/0私密）',
                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户书单表';


#书记书单关联表
CREATE TABLE book_list_item (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                                book_list_id BIGINT NOT NULL COMMENT '书单ID',
                                book_id BIGINT NOT NULL COMMENT '图书ID',
                                sort_order INT DEFAULT 0 COMMENT '排序值',
                                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
                                UNIQUE KEY uk_book_list_book (book_list_id, book_id),
                                INDEX idx_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书单与图书关联表';
