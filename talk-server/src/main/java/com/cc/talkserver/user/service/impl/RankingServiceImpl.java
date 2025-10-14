package com.cc.talkserver.user.service.impl;

import com.cc.talkcommon.constant.RedisCacheConstant;
import com.cc.talkpojo.dto.RankingQueryDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.entity.BookReview;
import com.cc.talkpojo.entity.UserInfo;
import com.cc.talkpojo.enums.RankingPeriod;
import com.cc.talkpojo.enums.RankingType;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookRankingVO;
import com.cc.talkpojo.vo.HotReviewVO;
import com.cc.talkserver.user.mapper.BookUserMapper;
import com.cc.talkserver.user.mapper.CategoryUserMapper;
import com.cc.talkserver.user.mapper.ReviewUserMapper;
import com.cc.talkserver.user.mapper.UserInfoUserMapper;
import com.cc.talkserver.user.service.RankingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 榜单推荐服务实现类
 *
 * @author cc
 * @since 2025-10-13
 */
@Slf4j
@Service
public class RankingServiceImpl implements RankingService {

    @Resource
    private ReviewUserMapper reviewUserMapper;

    @Resource
    private BookUserMapper bookUserMapper;

    @Resource
    private UserInfoUserMapper userInfoUserMapper;

    @Resource
    private RedisTemplate<String, Object> customObjectRedisTemplate;


    @Resource
    private CategoryUserMapper categoryUserMapper;

    /**
     * 获取热门评论
     * @param queryDTO 查询参数
     * @return
     */
    @Override
    public PageResult<HotReviewVO> getHotReviews(RankingQueryDTO queryDTO) {
        String period = queryDTO.getRankingPeriod() != null ?
                queryDTO.getRankingPeriod() : RankingPeriod.WEEKLY.getCode();

        // 先尝试从Redis获取
        String redisKey = RedisCacheConstant.RANKING_HOT_REVIEWS + ":" + period;
        List<HotReviewVO> cachedList = getCachedHotReviews(redisKey, queryDTO.getPage(), queryDTO.getSize());

        if (!cachedList.isEmpty()) {
            return new PageResult<>(getTotalHotReviews(redisKey), cachedList);
        }

        // Redis中没有，从数据库查询
        return getHotReviewsFromDB(queryDTO, period);
    }

    /**
     * 获取书籍榜单
     * @param queryDTO 查询参数
     * @return
     */
    @Override
    public PageResult<BookRankingVO> getBookRankings(RankingQueryDTO queryDTO) {
        String rankingType = queryDTO.getRankingType() != null ?
                queryDTO.getRankingType() : RankingType.BOOK_RATING.getCode();
        String period = queryDTO.getRankingPeriod() != null ?
                queryDTO.getRankingPeriod() : RankingPeriod.MONTHLY.getCode();

        // 先尝试从Redis获取
        String redisKey = RedisCacheConstant.RANKING_BOOKS + ":" + rankingType + ":" + period;
        List<BookRankingVO> cachedList = getCachedBookRankings(redisKey, queryDTO.getPage(), queryDTO.getSize());

        if (!cachedList.isEmpty()) {
            return new PageResult<>(getTotalBookRankings(redisKey), cachedList);
        }

        // Redis中没有，从数据库查询
        return getBookRankingsFromDB(queryDTO, rankingType, period);
    }


    /**
     * 获取推荐书籍
     * @param bookId 书籍ID
     * @param limit 推荐数量
     * @return
     */
    @Override
    public List<BookRankingVO> getSimilarBooks(Long bookId, Integer limit) {
        // 简单实现：基于同一类别和评分相近的书籍推荐
        Book currentBook = bookUserMapper.selectById(bookId);
        if (currentBook == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getCategoryId, currentBook.getCategoryId())
                   .ne(Book::getId, bookId)
                   .orderByDesc(Book::getAverageScore)
                   .last("LIMIT " + (limit != null ? limit : 5));

        List<Book> similarBooks = bookUserMapper.selectList(queryWrapper);

        return similarBooks.stream().map(book -> {
            BookRankingVO vo = new BookRankingVO();
            BeanUtils.copyProperties(book, vo);
            vo.setBookId(book.getId());
            vo.setBookName(book.getTitle());
            vo.setAuthor(book.getAuthor());
            vo.setAvgRating(book.getAverageScore());
            vo.setBookCover(book.getCoverUrl());
            vo.setCategory(getCategoryName(book.getCategoryId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新热门书评榜单
     * @param period 时间周期
     */
    @Override
    public void updateHotReviewsRanking(String period) {
        log.info("开始更新热门书评榜单: period={}", period);

        LocalDateTime startTime = getStartTimeByPeriod(period);

        // 查询指定时间范围内的书评，计算热度
        LambdaQueryWrapper<BookReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(BookReview::getCreateTime, startTime)
                   .orderByDesc(BookReview::getLikeCount)
                   .last("LIMIT 100"); // 只取前100名

        List<BookReview> reviews = reviewUserMapper.selectList(queryWrapper);

        List<HotReviewVO> hotReviews = reviews.stream()
                .map(this::convertToHotReviewVO)
                .collect(Collectors.toList());

        // 保存到Redis
        String redisKey = RedisCacheConstant.RANKING_HOT_REVIEWS + ":" + period;
        saveHotReviewsToRedis(redisKey, hotReviews);

        log.info("热门书评榜单更新完成: period={}, count={}", period, hotReviews.size());
    }

    /**
     * 更新书籍榜单
     * @param rankingType 榜单类型
     * @param period 时间周期
     */
    @Override
    public void updateBookRanking(String rankingType, String period) {
        log.info("开始更新书籍榜单: rankingType={}, period={}", rankingType, period);

        List<BookRankingVO> rankings = new ArrayList<>();

        switch (rankingType) {
            case "book_rating":
                rankings = getBookRatingRanking();
                break;
            case "hot_discussion":
                rankings = getHotDiscussionRanking();
                break;
            case "new_books":
                rankings = getNewBooksRanking();
                break;
            default:
                log.warn("未知的榜单类型: {}", rankingType);
                return;
        }

        // 保存到Redis
        String redisKey = RedisCacheConstant.RANKING_BOOKS + ":" + rankingType + ":" + period;
        saveBookRankingsToRedis(redisKey, rankings);

        log.info("书籍榜单更新完成: rankingType={}, period={}, count={}",
                rankingType, period, rankings.size());
    }

    /**
     * 获取热门书评Top N
     * @param period 时间周期
     * @param limit 数量限制
     * @return
     */
    @Override
    public List<HotReviewVO> getHotReviewsTopN(String period, Integer limit) {
        String redisKey = RedisCacheConstant.RANKING_HOT_REVIEWS + ":" + period;
        return getCachedHotReviews(redisKey, 1, limit != null ? limit : 10);
    }

    /**
     * 获取书籍榜单Top N
     * @param rankingType 榜单类型
     * @param period 时间周期
     * @param limit 数量限制
     * @return
     */
    @Override
    public List<BookRankingVO> getBookRankingTopN(String rankingType, String period, Integer limit) {
        String redisKey = RedisCacheConstant.RANKING_BOOKS + ":" + rankingType + ":" + period;
        return getCachedBookRankings(redisKey, 1, limit != null ? limit : 10);
    }

    // ==================== 私有方法 ====================

    private PageResult<HotReviewVO> getHotReviewsFromDB(RankingQueryDTO queryDTO, String period) {
        LocalDateTime startTime = getStartTimeByPeriod(period);

        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<BookReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(BookReview::getCreateTime, startTime)
                   .orderByDesc(BookReview::getLikeCount)
                   .orderByDesc(BookReview::getReplyCount);

        List<BookReview> reviews = reviewUserMapper.selectList(queryWrapper);
        PageInfo<BookReview> pageInfo = new PageInfo<>(reviews);

        List<HotReviewVO> hotReviews = reviews.stream()
                .map(this::convertToHotReviewVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageInfo.getTotal(), hotReviews);
    }

    private PageResult<BookRankingVO> getBookRankingsFromDB(RankingQueryDTO queryDTO, String rankingType, String period) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());

        List<Book> books = new ArrayList<>();

        switch (rankingType) {
            case "book_rating":
                LambdaQueryWrapper<Book> ratingWrapper = new LambdaQueryWrapper<>();
                ratingWrapper.orderByDesc(Book::getAverageScore)
                            .orderByDesc(Book::getScoreCount);
                books = bookUserMapper.selectList(ratingWrapper);
                break;
            case "hot_discussion":
                // 这里使用收藏数量作为讨论热度的指标
                LambdaQueryWrapper<Book> discussionWrapper = new LambdaQueryWrapper<>();
                discussionWrapper.orderByDesc(Book::getFavoriteCount);
                books = bookUserMapper.selectList(discussionWrapper);
                break;
            default:
                books = bookUserMapper.selectList(new LambdaQueryWrapper<>());
        }

        PageInfo<Book> pageInfo = new PageInfo<>(books);

        List<BookRankingVO> rankings = books.stream()
                .map(this::convertToBookRankingVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageInfo.getTotal(), rankings);
    }

    private HotReviewVO convertToHotReviewVO(BookReview review) {
        HotReviewVO vo = new HotReviewVO();
        BeanUtils.copyProperties(review, vo);
        vo.setReviewId(review.getId());

        // 计算热度值：点赞数 × 1 + 评论数 × 2
        Long hotScore = (long) (review.getLikeCount() + (review.getReplyCount() != null ? review.getReplyCount() * 2 : 0));
        vo.setHotScore(hotScore);

        // 设置时间描述
        vo.setTimeDesc(getTimeDesc(review.getCreateTime()));

        // 获取书籍信息
        if (review.getBookId() != null) {
            Book book = bookUserMapper.selectById(review.getBookId());
            if (book != null) {
                vo.setBookId(book.getId());
                vo.setBookName(book.getTitle());
                vo.setBookCover(book.getCoverUrl());
                vo.setCategory(getCategoryName(book.getCategoryId()));
            }
        }

        // 获取作者信息
        if (review.getUserId() != null) {
            UserInfo user = userInfoUserMapper.selectOne(
                    new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, review.getUserId())
            );
            if (user != null) {
                vo.setAuthorId(user.getUserId());
                vo.setAuthorName(user.getNickname());
                vo.setAuthorAvatar(user.getAvatar());
            }
        }

        return vo;
    }

    private BookRankingVO convertToBookRankingVO(Book book) {
        BookRankingVO vo = new BookRankingVO();
        BeanUtils.copyProperties(book, vo);
        vo.setBookId(book.getId());
        vo.setBookName(book.getTitle());
        vo.setBookCover(book.getCoverUrl());
        vo.setAvgRating(book.getAverageScore());
        vo.setRatingCount(book.getScoreCount());
        vo.setReadCount(book.getFavoriteCount() != null ? book.getFavoriteCount().longValue() : 0L);
        vo.setPublishTime(book.getCreateTime());

        // 设置分类（这里简化处理，实际可能需要关联查询分类表）
        vo.setCategory(getCategoryName(book.getCategoryId()));

        return vo;
    }

    private LocalDateTime getStartTimeByPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();

        switch (period) {
            case "daily":
                return now.minus(1, ChronoUnit.DAYS);
            case "weekly":
                return now.minus(7, ChronoUnit.DAYS);
            case "monthly":
                return now.minus(30, ChronoUnit.DAYS);
            case "all_time":
            default:
                return LocalDateTime.of(2000, 1, 1, 0, 0); // 很早的时间
        }
    }

    private String getTimeDesc(LocalDateTime createTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createTime, now);

        if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else {
            return (minutes / 1440) + "天前";
        }
    }

    // ==================== Redis操作方法 ====================

    private void saveHotReviewsToRedis(String key, List<HotReviewVO> hotReviews) {
        try {
            customObjectRedisTemplate.delete(key);
            if (!hotReviews.isEmpty()) {
                customObjectRedisTemplate.opsForValue().set(key, hotReviews);
                customObjectRedisTemplate.expire(key, 2, java.util.concurrent.TimeUnit.HOURS); // 2小时过期
            }
        } catch (Exception e) {
            log.error("保存热门书评榜单到Redis失败: key={}, error: {}", key, e.getMessage(), e);
        }
    }

    private void saveBookRankingsToRedis(String key, List<BookRankingVO> rankings) {
        try {
            customObjectRedisTemplate.delete(key);
            if (!rankings.isEmpty()) {
                customObjectRedisTemplate.opsForValue().set(key, rankings);
                customObjectRedisTemplate.expire(key, 24, java.util.concurrent.TimeUnit.HOURS); // 24小时过期
            }
        } catch (Exception e) {
            log.error("保存书籍榜单到Redis失败: key={}, error: {}", key, e.getMessage(), e);
        }
    }

    private List<HotReviewVO> getCachedHotReviews(String key, int page, int size) {
        try {
            Object cachedData = customObjectRedisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                // 确保数据类型正确
                if (cachedData instanceof List) {
                    List<HotReviewVO> cachedList = (List<HotReviewVO>) cachedData;
                    int start = (page - 1) * size;
                    int end = Math.min(start + size, cachedList.size());
                    return start < cachedList.size() ?
                            cachedList.subList(start, end) : new ArrayList<>();
                }
            }
        } catch (Exception e) {
            log.error("从Redis获取热门书评榜单失败: key={}, error: {}", key, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    private List<BookRankingVO> getCachedBookRankings(String key, int page, int size) {
        try {
            Object cachedData = customObjectRedisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                // 确保数据类型正确
                if (cachedData instanceof List) {
                    List<BookRankingVO> cachedList = (List<BookRankingVO>) cachedData;
                    int start = (page - 1) * size;
                    int end = Math.min(start + size, cachedList.size());
                    return start < cachedList.size() ?
                            cachedList.subList(start, end) : new ArrayList<>();
                }
            }
        } catch (Exception e) {
            log.error("从Redis获取书籍榜单失败: key={}, error: {}", key, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    private Long getTotalHotReviews(String key) {
        try {
            Object cachedData = customObjectRedisTemplate.opsForValue().get(key);
            if (cachedData instanceof List) {
                return (long) ((List<?>) cachedData).size();
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long getTotalBookRankings(String key) {
        try {
            Object cachedData = customObjectRedisTemplate.opsForValue().get(key);
            if (cachedData instanceof List) {
                return (long) ((List<?>) cachedData).size();
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<BookRankingVO> getBookRatingRanking() {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Book::getAverageScore)
               .orderByDesc(Book::getScoreCount)
               .last("LIMIT 50");

        return bookUserMapper.selectList(wrapper).stream()
                .map(this::convertToBookRankingVO)
                .collect(Collectors.toList());
    }

    private List<BookRankingVO> getHotDiscussionRanking() {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Book::getFavoriteCount)
               .orderByDesc(Book::getScoreCount)
               .last("LIMIT 50");

        return bookUserMapper.selectList(wrapper).stream()
                .map(this::convertToBookRankingVO)
                .collect(Collectors.toList());
    }

    private List<BookRankingVO> getNewBooksRanking() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Book::getCreateTime, thirtyDaysAgo)
               .orderByDesc(Book::getAverageScore)
               .orderByDesc(Book::getScoreCount)
               .last("LIMIT 50");

        return bookUserMapper.selectList(wrapper).stream()
                .map(this::convertToBookRankingVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据分类ID获取分类名称
     * 直接查询数据库获取分类信息
     * @param categoryId 分类ID
     * @return 分类名称
     */
    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "未分类";
        }

        try {
            BookCategory category = categoryUserMapper.selectById(categoryId);
            return category != null ? category.getName() : "未分类";
        } catch (Exception e) {
            log.error("查询分类名称失败: categoryId={}", categoryId, e);
            return "未分类";
        }
    }
}