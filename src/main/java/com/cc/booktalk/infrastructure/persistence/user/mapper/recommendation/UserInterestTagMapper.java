package com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.entity.entity.recommendation.UserInterestTag;
import com.cc.booktalk.entity.vo.PersonalizedRecVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户兴趣标签Mapper接口
 *
 * @author cc
 * @since 2024-01-15
 */
@Mapper
public interface UserInterestTagMapper extends BaseMapper<UserInterestTag> {

    /**
     * 获取用户的所有兴趣标签
     *
     * @param userId 用户ID
     * @return 兴趣标签列表
     */
    List<UserInterestTag> getUserInterests(@Param("userId") Long userId);

    /**
     * 根据用户ID查询兴趣标签（简化方法）
     *
     * @param userId 用户ID
     * @return 兴趣标签列表
     */
    List<UserInterestTag> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和标签名查询兴趣标签
     *
     * @param userId 用户ID
     * @param tagName 标签名称
     * @return 兴趣标签
     */
    UserInterestTag selectByUserAndTag(@Param("userId") Long userId, @Param("tagName") String tagName);

    /**
     * 根据标签名获取推荐书籍
     *
     * @param tagName 标签名称
     * @param limit 推荐数量限制
     * @return 推荐书籍列表
     */
    List<PersonalizedRecVO> getBooksByTagName(@Param("tagName") String tagName,
                                              @Param("limit") Integer limit);


    /**
     * 获取用户兴趣分数最高的标签
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 兴趣标签列表
     */
    List<UserInterestTag> getTopUserInterests(@Param("userId") Long userId,
                                             @Param("limit") Integer limit);

    /**
     * 更新用户兴趣标签
     * 如果存在则更新，不存在则插入
     *
     * @param userInterest 用户兴趣标签
     * @return 影响的行数
     */
    int upsertUserInterest(@Param("userInterest") UserInterestTag userInterest);

    /**
     * 批量更新用户兴趣标签
     *
     * @param userInterests 兴趣标签列表
     * @return 影响的行数
     */
    int batchUpsertInterests(@Param("userInterests") List<UserInterestTag> userInterests);

    /**
     * 应用时间衰减到用户兴趣分数
     *
     * @param userId 用户ID
     * @param decayFactor 衰减因子
     * @return 影响的行数
     */
    int applyTimeDecay(@Param("userId") Long userId,
                      @Param("decayFactor") Double decayFactor);

    /**
     * 删除低分数的兴趣标签
     *
     * @param userId 用户ID
     * @param minScore 最小分数阈值
     * @return 删除的行数
     */
    int deleteLowScoreInterests(@Param("userId") Long userId,
                               @Param("minScore") Double minScore);

    /**
     * 获取用户兴趣统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    java.util.Map<String, Object> getUserInterestStats(@Param("userId") Long userId);

    /**
     * 获取兴趣标签相似的用户
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 相似用户ID列表
     */
    List<Long> getSimilarUsersByInterests(@Param("userId") Long userId,
                                         @Param("limit") Integer limit);

    /**
     * 清理用户的过期兴趣标签
     *
     * @param userId 用户ID
     * @param days 天数阈值
     * @return 删除的行数
     */
    int cleanExpiredInterests(@Param("userId") Long userId,
                              @Param("days") Integer days);

    /**
     * 获取所有有兴趣标签的用户ID
     *
     * @return 用户ID列表
     */
    List<Long> getAllUsersWithInterests();

    /**
     * 根据标签名获取用户ID列表
     *
     * @param tagName 标签名称
     * @param limit 限制数量
     * @return 用户ID列表
     */
    List<Long> getUsersByTag(@Param("tagName") String tagName, @Param("limit") Integer limit);
}