package com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.entity.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.entity.vo.PersonalizedRecVO;
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

}