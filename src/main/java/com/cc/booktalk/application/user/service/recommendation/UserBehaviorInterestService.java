package com.cc.booktalk.application.user.service.recommendation;

import com.cc.booktalk.entity.dto.behavior.UserBehaviorDTO;
import com.cc.booktalk.entity.entity.book.Book;
import com.cc.booktalk.entity.entity.review.BookReview;
import com.cc.booktalk.entity.entity.tag.Tag;
import com.cc.booktalk.application.user.service.book.BookUserService;
import com.cc.booktalk.application.user.service.review.ReviewUserService;
import com.cc.booktalk.application.user.service.tag.TagUserService;
import com.cc.booktalk.domain.recommendation.UserInterestDomainService;
import com.cc.booktalk.infrastructure.persistence.user.mapper.tag.BookTagUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserBehaviorInterestService {


    @Resource
    private UserInterestService userInterestService;


    @Resource
    private ReviewUserService reviewUserService;

    @Resource
    private BookUserService bookUserService;

    @Resource
    private TagUserService tagUserService;

    @Resource
    private BookTagUserMapper bookTagUserMapper;

    @Resource
    private UserInterestDomainService userInterestDomainService;

    /**
     * 根据用户行为更新兴趣分数
     */
    public void updateUserInterest(UserBehaviorDTO behaviorDTO) {
        try {
            Long userId = behaviorDTO.getUserId();
            Long targetId = behaviorDTO.getTargetId();
            Double behaviorScore = behaviorDTO.getBehaviorScore();
            String behaviorType = behaviorDTO.getBehaviorType();

            if (!userInterestDomainService.isValidBehavior(userId, targetId, behaviorScore, behaviorType)) {
                log.warn("用户行为数据不完整，跳过兴趣更新: userId={}, behaviorType={}, targetId={}, behaviorScore={}",
                        userId, behaviorType, targetId, behaviorScore);
                return;
            }

            Long reviewBookId = null;
            if (behaviorType.startsWith("REVIEW_")) {
                reviewBookId = getBookIdFromReview(targetId);
            }
            Long bookId = userInterestDomainService.resolveBookIdByBehavior(behaviorType, targetId, reviewBookId);

            if (bookId == null) {
                log.warn("不支持的用户行为类型，跳过兴趣更新: userId={}, behaviorType={}", userId, behaviorType);
                return;
            }

            updateBookInterest(userId, bookId, behaviorScore);

        } catch (Exception e) {
            log.error("更新用户兴趣分数失败", e);
        }
    }

    /**
     * 更新书籍相关的兴趣分数
     */
    public void updateBookInterest(Long userId, Long bookId, Double behaviorScore) {
        if (bookId == null) {
            log.warn("书籍ID为空，跳过兴趣更新: userId={}", userId);
            return;
        }

        // 获取书籍信息、分类、书籍标签
        Book book = bookUserService.getById(bookId);
        if (book == null) {
            log.warn("书籍不存在，跳过兴趣更新: userId={}, bookId={}", userId, bookId);
            return;
        }

        List<String> tags = userInterestDomainService.resolveInterestTags(getBookTags(bookId));
        Long categoryId = book.getCategoryId();

        // 更新具体标签兴趣
        for (String tag : tags) {
            userInterestService.updateInterestScore(userId,tag, behaviorScore);
        }
        log.debug("书籍兴趣分数更新完成: userId={}, bookId={}, categoryId={}, tags={}, score={}",
                userId, bookId, categoryId, tags, behaviorScore);
    }

    /**
     * 根据书籍id获取书籍tag
     * @param bookId
     * @return
     */
    private List<String> getBookTags(Long bookId) {
        if (bookId == null) {
            return new ArrayList<>();
        }

        // 查询 book_tag_relation 表获取标签ID
        List<Long> tagIds = bookTagUserMapper.selectTagIdsByBookId(bookId);

        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 根据标签ID获取标签名称
        List<String> tagNames = new ArrayList<>();
        for (Long tagId : tagIds) {
            Tag tag = tagUserService.getById(tagId);
            if (tag != null && tag.getName() != null) {
                tagNames.add(tag.getName());
            }
        }

        log.debug("获取书籍标签: bookId={}, tagNames={}", bookId, tagNames);
        return tagNames;
    }

    /**
     * 根据书评ID获取书籍ID
     */
    private Long getBookIdFromReview(Long reviewId) {
        try {
            BookReview review = reviewUserService.getById(reviewId);
            return review.getBookId();
        } catch (Exception e) {
            log.error("根据评论ID获取书籍ID失败: reviewId={}", reviewId, e);
            return null;
        }
    }


}
