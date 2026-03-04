-- ========================================
-- 个性化推荐系统数据库表设计（简化版）
-- ========================================

-- 1. 用户行为记录表
-- 用于存储用户的详细行为数据，是推荐系统的基础数据源
CREATE TABLE user_behavior_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_id BIGINT NOT NULL COMMENT '目标对象ID（图书ID、书评ID等）',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型：BOOK, REVIEW, USER等',
    behavior_type VARCHAR(50) NOT NULL COMMENT '行为类型：BOOK_VIEW, BOOK_LIKE, BOOK_COLLECT, BOOK_SCORE, BOOK_REVIEW, REVIEW_LIKE, REVIEW_REPLY等',
    behavior_score DECIMAL(10,4) DEFAULT 1.0 COMMENT '行为分数，根据行为类型设置不同权重',
    extra_data TEXT COMMENT '额外数据（JSON格式），如评分值、评论内容等',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引设计
    INDEX idx_user_behavior_time (user_id, create_time),
    INDEX idx_target_behavior (target_id, target_type, behavior_type),
    INDEX idx_behavior_type_time (behavior_type, create_time),
    INDEX idx_user_target_type (user_id, target_id, target_type)
) COMMENT '用户行为记录表';

-- 2. 用户兴趣标签表
-- 用于存储用户对不同分类、标签、作者等的兴趣分数，是推荐算法计算的核心数据
CREATE TABLE user_interest_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_id BIGINT COMMENT '分类ID（关联book_category表）',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称：可以是分类名称、作者名、主题标签等',
    interest_score DECIMAL(10,4) NOT NULL DEFAULT 0.0 COMMENT '兴趣分数，基于用户行为计算得出',
    behavior_count INT NOT NULL DEFAULT 0 COMMENT '行为次数，该兴趣标签相关的用户行为总次数',
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引设计
    INDEX idx_user_interest (user_id),
    INDEX idx_category_interest (category_id),
    INDEX idx_tag_interest (tag_name),
    INDEX idx_score_interest (interest_score),
    UNIQUE KEY uk_user_category_tag (user_id, category_id, tag_name)
) COMMENT '用户兴趣标签表';

-- ========================================
-- 索引优化建议
-- ========================================

-- 为现有表添加推荐系统相关索引
ALTER TABLE book ADD INDEX idx_book_category_score (category_id, average_score, favorite_count);
ALTER TABLE book ADD INDEX idx_book_score_count (score_count, average_score);
ALTER TABLE book ADD INDEX idx_book_create_time (create_time);

ALTER TABLE book_review ADD INDEX idx_review_user_time (user_id, create_time);
ALTER TABLE book_review ADD INDEX idx_review_book_time (book_id, create_time);
ALTER TABLE book_review ADD INDEX idx_review_score (score, create_time);

ALTER TABLE book_shelf ADD INDEX idx_shelf_user_time (user_id, create_time);
ALTER TABLE book_shelf ADD INDEX idx_shelf_user_status (user_id, status);

ALTER TABLE like_record ADD INDEX idx_like_user_time_target (user_id, create_time, target_type);
ALTER TABLE like_record ADD INDEX idx_like_target_type (target_id, target_type, create_time);

-- ========================================
-- 数据清理策略
-- ========================================

-- 创建数据清理的存储过程
DELIMITER $$

CREATE PROCEDURE CleanExpiredBehaviorLogs(IN retention_days INT)
BEGIN
    DECLARE deleted_count INT DEFAULT 0;

    -- 删除过期的用户行为日志
    DELETE FROM user_behavior_log
    WHERE create_time < DATE_SUB(NOW(), INTERVAL retention_days DAY);

    SET deleted_count = ROW_COUNT();

    SELECT CONCAT('清理完成，删除 ', deleted_count, ' 条过期记录') AS result;
END$$

DELIMITER ;

-- ========================================
-- 推荐结果存储策略
-- ========================================

/*
推荐结果存储方案：使用Redis

1. 推荐列表存储：
   Key: personalized_rec:{userId}
   Value: JSON格式的推荐列表
   TTL: 24小时

2. 用户兴趣缓存：
   Key: user_interest:{userId}
   Value: JSON格式的兴趣标签列表
   TTL: 6小时

3. 热门推荐缓存：
   Key: popular_books:{category}:{period}
   Value: JSON格式的热门图书列表
   TTL: 2小时
*/

-- ========================================
-- 行为类型和分数配置
-- ========================================

/*
行为类型和对应分数配置：

浏览行为：
- BOOK_VIEW: 1.0分 (浏览图书)

互动行为：
- BOOK_LIKE: 3.0分 (点赞图书)
- REVIEW_LIKE: 2.0分 (点赞书评)
- REVIEW_REPLY: 3.0分 (回复书评)

深度行为：
- BOOK_COLLECT: 5.0分 (收藏图书)
- BOOK_REVIEW: 4.0分 (写书评)
- BOOK_SCORE: 实际分值 (评分，1-10分)

时间衰减策略：
- 30天半衰期：score * e^(-天数/30)
- 最小分数阈值：0.1分
*/

-- ========================================
-- 使用说明
-- ========================================

/*
1. 核心表说明：
   - user_behavior_log: 存储所有用户行为，推荐系统的基础数据
   - user_interest_tag: 存储用户兴趣模型，推荐算法的核心数据

2. 数据保留策略：
   - user_behavior_log: 保留90天（定期清理）
   - user_interest_tag: 保留1年（长期兴趣模型）

3. 推荐结果存储：
   - 使用Redis存储推荐列表，性能更好
   - 支持实时更新和定时更新
   - 避免数据库压力

4. 性能优化：
   - 所有关键查询都有索引支持
   - 定期清理过期数据
   - 使用Redis缓存热点数据

5. 扩展策略：
   - 先用2张表实现核心功能
   - 后续根据需要添加推荐记录表（效果追踪）
   - 再根据需要添加统计表（性能优化）

6. 简化优势：
   - 维护成本低
   - 数据结构清晰
   - 开发效率高
   - 便于快速验证推荐效果
*/