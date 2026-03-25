package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.application.user.service.recommendation.model.UserProfileSnapshot;

/**
 * 用户画像服务
 */
public interface UserProfileService {

    /**
     * 构建用户当前推荐画像快照
     *
     * @param userId 用户ID
     * @return 用户画像快照
     */
    UserProfileSnapshot buildSnapshot(Long userId);
}
