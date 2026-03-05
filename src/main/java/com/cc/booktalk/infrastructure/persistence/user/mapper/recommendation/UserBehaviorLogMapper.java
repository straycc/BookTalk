package com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户行为记录Mapper接口
 *
 * @author cc
 * @since 2024-01-15
 */
@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {

    /**
     * 获取用户高分书籍（基于行为日志）
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 高分书籍列表
     */
    List<PersonalizedRecVO> getHighScoreBooks(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 获取热门书籍
     *
     * @param limit 限制数量
     * @return 热门书籍列表
     */
    List<PersonalizedRecVO> getHotBooks(@Param("limit") Integer limit);

    /**
     * 获取活跃用户列表
     *
     * @param days 最近天数
     * @param minActions 最小行为次数
     * @return 活跃用户ID列表
     */
    List<Long> getActiveUsers(@Param("days") Integer days, @Param("minActions") Integer minActions);

    /**
     * 获取热门书籍候选ID（按行为活跃度初筛）
     *
     * @param days 最近天数
     * @param limit 候选数量
     * @return 书籍ID列表
     */
    List<Long> getHotBookCandidateIds(@Param("days") Integer days, @Param("limit") Integer limit);

    /**
     * 获取单本书的近期行为记录
     *
     * @param bookId 书籍ID
     * @param days 最近天数
     * @return 行为记录
     */
    List<UserBehaviorLog> getBookRecentBehaviors(@Param("bookId") Long bookId, @Param("days") Integer days);

    /**
     * 获取热门书评候选ID（按行为活跃度初筛）
     *
     * @param days 最近天数
     * @param limit 候选数量
     * @return 书评ID列表
     */
    List<Long> getHotReviewCandidateIds(@Param("days") Integer days, @Param("limit") Integer limit);

    /**
     * 获取单条书评的近期行为记录
     *
     * @param reviewId 书评ID
     * @param days 最近天数
     * @return 行为记录
     */
    List<UserBehaviorLog> getReviewRecentBehaviors(@Param("reviewId") Long reviewId, @Param("days") Integer days);

}
