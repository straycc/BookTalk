package com.cc.booktalk.application.user.service.square.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.booktalk.application.user.service.content.ContentTagService;
import com.cc.booktalk.application.user.service.square.SquareFeedService;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.common.utils.CheckPageParam;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.entity.post.Post;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.domain.entity.user.UserInfo;
import com.cc.booktalk.interfaces.dto.user.square.SquareFeedQueryDTO;
import com.cc.booktalk.interfaces.vo.user.square.SquareFeedVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.post.PostMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation.UserInfoUserMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.review.ReviewUserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SquareFeedServiceImpl implements SquareFeedService {

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private ContentTagService contentTagService;

    @Override
    public PageResult<SquareFeedVO> getFeed(SquareFeedQueryDTO queryDTO) {
        CheckPageParam.checkPageDTO(queryDTO);
        String type = normalizeType(queryDTO.getType());
        String sort = normalizeSort(queryDTO.getSort());
        List<SquareFeedVO> feed;
        switch (type) {
            case BusinessConstant.SQUARE_TYPE_REVIEW:
                feed = buildReviewFeed(sort);
                break;
            case BusinessConstant.SQUARE_TYPE_DISCUSSION:
                feed = buildPostFeed(sort);
                break;
            case BusinessConstant.SQUARE_TYPE_ALL:
            default:
                feed = buildMixedFeed(sort);
                break;
        }
        if (queryDTO.getTagId() != null) {
            feed = feed.stream()
                    .filter(item -> item.getTags() != null && item.getTags().stream().anyMatch(tag -> queryDTO.getTagId().equals(tag.getId())))
                    .collect(Collectors.toList());
        }
        return paginate(feed, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    private List<SquareFeedVO> buildMixedFeed(String sort) {
        List<SquareFeedVO> reviewFeed = buildReviewFeed(sort);
        List<SquareFeedVO> postFeed = buildPostFeed(sort);
        int totalSize = Math.max(15, reviewFeed.size() + postFeed.size());
        int reviewQuota = Math.max(1, totalSize * 60 / 100);
        int postQuota = Math.max(1, totalSize - reviewQuota);

        List<SquareFeedVO> mixed = new ArrayList<>();
        mixed.addAll(reviewFeed.stream().limit(reviewQuota).collect(Collectors.toList()));
        mixed.addAll(postFeed.stream().limit(postQuota).collect(Collectors.toList()));

        List<SquareFeedVO> leftovers = new ArrayList<>();
        leftovers.addAll(reviewFeed.stream().skip(reviewQuota).collect(Collectors.toList()));
        leftovers.addAll(postFeed.stream().skip(postQuota).collect(Collectors.toList()));
        if (mixed.size() < totalSize) {
            mixed.addAll(leftovers.stream().limit(totalSize - mixed.size()).collect(Collectors.toList()));
        }
        sortFeed(mixed, sort);
        return mixed;
    }

    private List<SquareFeedVO> buildReviewFeed(String sort) {
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        sortReviews(wrapper, sort);
        List<BookReview> reviews = reviewUserMapper.selectList(wrapper);
        Map<Long, UserInfo> userMap = loadUsers(reviews.stream().map(BookReview::getUserId).collect(Collectors.toList()));
        Map<Long, Book> bookMap = loadBooks(reviews.stream().map(BookReview::getBookId).collect(Collectors.toList()));
        return reviews.stream().map(review -> {
            SquareFeedVO vo = new SquareFeedVO();
            vo.setId(review.getId());
            vo.setType(BusinessConstant.CONTENT_TYPE_REVIEW);
            vo.setTitle(review.getTitle() == null || review.getTitle().isBlank() ? "书评" : review.getTitle());
            vo.setContentSummary(buildSummary(review.getContent()));
            vo.setAuthorId(review.getUserId());
            vo.setLikeCount(defaultInt(review.getLikeCount()));
            vo.setCommentCount(defaultInt(review.getReplyCount()));
            vo.setViewCount(0);
            vo.setHotScore(review.getHotScore());
            vo.setLastActiveTime(review.getUpdateTime() == null ? review.getCreateTime() : review.getUpdateTime());
            vo.setCreateTime(review.getCreateTime());
            vo.setTags(contentTagService.getTagsByContent(review.getId(), BusinessConstant.CONTENT_TYPE_REVIEW));
            UserInfo user = userMap.get(review.getUserId());
            if (user != null) {
                vo.setAuthorName(user.getNickname());
                vo.setAuthorAvatar(user.getAvatarUrl());
            }
            Book book = bookMap.get(review.getBookId());
            if (book != null) {
                vo.setCover(book.getCoverUrl());
                vo.setRelatedBookId(book.getId());
                vo.setRelatedBookName(book.getTitle());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<SquareFeedVO> buildPostFeed(String sort) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);
        if (BusinessConstant.POST_SORT_HOT.equals(sort)) {
            wrapper.orderByDesc(Post::getHotScore).orderByDesc(Post::getLastActiveTime).orderByDesc(Post::getCreateTime);
        } else {
            wrapper.orderByDesc(Post::getLastActiveTime).orderByDesc(Post::getCreateTime);
        }
        List<Post> posts = postMapper.selectList(wrapper);
        Map<Long, UserInfo> userMap = loadUsers(posts.stream().map(Post::getUserId).collect(Collectors.toList()));
        Map<Long, Book> bookMap = loadBooks(posts.stream().map(Post::getRelatedBookId).filter(id -> id != null).collect(Collectors.toList()));
        return posts.stream().map(post -> {
            SquareFeedVO vo = new SquareFeedVO();
            vo.setId(post.getId());
            vo.setType(BusinessConstant.CONTENT_TYPE_POST);
            vo.setTitle(post.getTitle());
            vo.setContentSummary(post.getSummary());
            vo.setAuthorId(post.getUserId());
            vo.setLikeCount(defaultInt(post.getLikeCount()));
            vo.setCommentCount(defaultInt(post.getCommentCount()));
            vo.setViewCount(defaultInt(post.getViewCount()));
            vo.setHotScore(post.getHotScore() == null ? 0.0 : post.getHotScore());
            vo.setLastActiveTime(post.getLastActiveTime());
            vo.setCreateTime(post.getCreateTime());
            vo.setTags(contentTagService.getTagsByContent(post.getId(), BusinessConstant.CONTENT_TYPE_POST));
            UserInfo user = userMap.get(post.getUserId());
            if (user != null) {
                vo.setAuthorName(user.getNickname());
                vo.setAuthorAvatar(user.getAvatarUrl());
            }
            if (post.getRelatedBookId() != null) {
                Book book = bookMap.get(post.getRelatedBookId());
                if (book != null) {
                    vo.setCover(book.getCoverUrl());
                    vo.setRelatedBookId(book.getId());
                    vo.setRelatedBookName(book.getTitle());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private void sortReviews(LambdaQueryWrapper<BookReview> wrapper, String sort) {
        if (BusinessConstant.POST_SORT_HOT.equals(sort)) {
            wrapper.orderByDesc(BookReview::getHotScore).orderByDesc(BookReview::getUpdateTime).orderByDesc(BookReview::getCreateTime);
        } else {
            wrapper.orderByDesc(BookReview::getUpdateTime).orderByDesc(BookReview::getCreateTime);
        }
    }

    private void sortFeed(List<SquareFeedVO> feed, String sort) {
        Comparator<SquareFeedVO> comparator;
        if (BusinessConstant.POST_SORT_HOT.equals(sort)) {
            comparator = Comparator.comparing(SquareFeedVO::getHotScore, Comparator.nullsLast(Double::compareTo)).reversed()
                    .thenComparing(SquareFeedVO::getLastActiveTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
        } else {
            comparator = Comparator.comparing(SquareFeedVO::getLastActiveTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed()
                    .thenComparing(SquareFeedVO::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
        }
        feed.sort(comparator);
    }

    private Map<Long, UserInfo> loadUsers(List<Long> userIds) {
        Set<Long> uniqueIds = userIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return userInfoUserMapper.selectList(new LambdaQueryWrapper<UserInfo>().in(UserInfo::getUserId, uniqueIds))
                .stream()
                .collect(Collectors.toMap(UserInfo::getUserId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Book> loadBooks(List<Long> bookIds) {
        Set<Long> uniqueIds = bookIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return bookUserMapper.selectBatchIds(uniqueIds)
                .stream()
                .collect(Collectors.toMap(Book::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return BusinessConstant.SQUARE_TYPE_ALL;
        }
        String normalized = type.trim().toLowerCase();
        if (BusinessConstant.SQUARE_TYPE_REVIEW.equals(normalized)
                || BusinessConstant.SQUARE_TYPE_DISCUSSION.equals(normalized)) {
            return normalized;
        }
        return BusinessConstant.SQUARE_TYPE_ALL;
    }

    private String normalizeSort(String sort) {
        return BusinessConstant.POST_SORT_HOT.equalsIgnoreCase(sort) ? BusinessConstant.POST_SORT_HOT : BusinessConstant.POST_SORT_LATEST;
    }

    private String buildSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80) + "...";
    }

    private <T> PageResult<T> paginate(List<T> items, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        return new PageResult<>(items.size(), items.subList(fromIndex, toIndex));
    }
}
