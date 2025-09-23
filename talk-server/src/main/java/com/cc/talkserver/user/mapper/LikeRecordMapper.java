package com.cc.talkserver.user.mapper;

import com.cc.talkpojo.entity.LikeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

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
    int deleteByUserAndTarget(@Param("userId") Long userId, @Param("likeTargetType") String likeTargetType, @Param("targetId") Long targetId);


    /**
     * 根据用户id和目标类型/Id查询点赞记录
     * @param userId
     * @param targetType
     * @param targetId
     * @return
     */
    Long selectByUserTaeget(@Param("userId")Long userId,
                            @Param("targetType") String targetType,
                            @Param("targetId") Long targetId);
}
