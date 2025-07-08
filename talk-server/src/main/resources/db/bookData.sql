
#书籍基本信息表
CREATE TABLE book (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图书ID',
                      title VARCHAR(255) NOT NULL COMMENT '书名',
                      original_title VARCHAR(255) DEFAULT NULL COMMENT '原作名称（外文原名）',
                      author VARCHAR(100) DEFAULT NULL COMMENT '作者',
                      author_country VARCHAR(100) DEFAULT NULL COMMENT '作者国籍',
                      translator VARCHAR(100) DEFAULT NULL COMMENT '译者',
                      publisher VARCHAR(255) DEFAULT NULL COMMENT '出版社',
                      producer VARCHAR(255) DEFAULT NULL COMMENT '出品方/品牌方',
                      publish_date DATE DEFAULT NULL COMMENT '出版时间',
                      price DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
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



#书籍评分表
CREATE TABLE book_rating (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评分ID',
                             user_id BIGINT NOT NULL COMMENT '用户ID',
                             book_id BIGINT NOT NULL COMMENT '图书ID',
                             score TINYINT NOT NULL CHECK (score BETWEEN 1 AND 10) COMMENT '评分，1-10分',
                             book_favorite BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否收藏',
                             comment TEXT COMMENT '评论内容（可选）',
                             create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
                             UNIQUE KEY uk_user_book (user_id, book_id),
                             INDEX idx_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评分表';


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


#书籍表签表
CREATE TABLE book_tag (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
                          name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
                          description TEXT COMMENT '标签描述',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书标签表';

#图书-标签关联表
CREATE TABLE book_tag_relation (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
                                   book_id BIGINT NOT NULL COMMENT '图书ID',
                                   tag_id BIGINT NOT NULL COMMENT '标签ID',
                                   create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   update_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                   UNIQUE KEY uniq_book_tag (book_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书标签表';