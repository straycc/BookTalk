package com.cc.booktalk.application.user.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.booktalk.application.user.service.post.PostHotRefreshService;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import com.cc.booktalk.domain.post.PostHotScoreDomainService;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserBehaviorLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PostHotRefreshServiceImpl implements PostHotRefreshService {

    private static final int WINDOW_DAYS = 30;
    private static final int CANDIDATE_LIMIT = 500;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostHotScoreDomainService postHotScoreDomainService;

    @Override
    public void refreshPostHotScores() {
        List<Long> postIds = userBehaviorLogMapper.getHotPostCandidateIds(WINDOW_DAYS, CANDIDATE_LIMIT);
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long postId : postIds) {
            List<UserBehaviorLog> behaviors = userBehaviorLogMapper.getPostRecentBehaviors(postId, WINDOW_DAYS);
            double hotScore = postHotScoreDomainService.calculateHotScore(behaviors, now);
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .set(Post::getHotScore, hotScore)
                    .set(Post::getHotScoreUpdateTime, now));
        }
        log.info("帖子热度刷新完成: candidateCount={}", postIds.size());
    }
}
