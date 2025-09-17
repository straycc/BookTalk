package com.cc.talkserver.user.mapper;

import com.cc.talkpojo.entity.LikeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-09-10
 */
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {


    /**
     * 根据用户id和目标类型/Id删除点赞记录
     *
     * @param userId
     * @param likeTargetType
     * @param targetId
     */
    int deleteByUserAndTarget(Long userId, String likeTargetType, Long targetId);


    /**
     * 根据用户id和目标类型/Id查询点赞记录
     * @param userId
     * @param targetType
     * @param targetId
     * @return
     */
    Long selectByUserTaeget(Long userId, String targetType, Long targetId);
}
