package com.cc.booktalk.application.user.service.rank.impl;

import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.redis.RedisCacheUtils;
import com.cc.booktalk.interfaces.dto.user.ranking.RankingQueryDTO;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import com.cc.booktalk.application.user.service.rank.BookRankingRefreshService;
import com.cc.booktalk.application.user.service.rank.ReviewRankingRefreshService;
import com.cc.booktalk.application.user.service.rank.RankingService;
import com.cc.booktalk.infrastructure.persistence.user.mapper.book.BookUserMapper;
import com.cc.booktalk.domain.entity.book.Book;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RankingServiceImpl implements RankingService {

    @Resource
    private RedisCacheUtils redisCacheUtils;

    @Resource
    private ReviewRankingRefreshService reviewRankingRefreshService;

    @Resource
    private BookRankingRefreshService bookRankingRefreshService;

    @Resource
    private BookUserMapper bookUserMapper;

    @Override
    public PageResult<BookRankingVO> getBookRankings(RankingQueryDTO queryDTO) {
        int page = (queryDTO != null && queryDTO.getPage() != null && queryDTO.getPage() > 0) ? queryDTO.getPage() : 1;
        int size = (queryDTO != null && queryDTO.getSize() != null && queryDTO.getSize() > 0) ? queryDTO.getSize() : 10;
        String rankingType = queryDTO != null ? queryDTO.getRankingType() : null;
        String period = queryDTO != null ? queryDTO.getRankingPeriod() : null;

        int fetchLimit = Math.max(page * size, 50);
        List<BookRankingVO> fullList = getBookRankingTopN(rankingType, period, fetchLimit);

        int fromIndex = Math.min((page - 1) * size, fullList.size());
        int toIndex = Math.min(fromIndex + size, fullList.size());
        List<BookRankingVO> pageRecords = fullList.subList(fromIndex, toIndex);

        return new PageResult<>(fullList.size(), pageRecords);
    }

    @Override
    public List<HotReviewVO> getHotReviewRankingTopN(String period, Integer limit) {
        int finalLimit = (limit != null && limit > 0) ? limit : 10;
        String finalPeriod = normalizePeriod(period);
        String cacheKey = RedisCacheConstant.RANKING_HOT_REVIEWS_PREFIX + finalPeriod;

        List<HotReviewVO> cachedData = redisCacheUtils.getCacheDataLimit(cacheKey, finalLimit, HotReviewVO.class);
        if (cachedData != null && !cachedData.isEmpty()) {
            return cachedData;
        }

        reviewRankingRefreshService.refreshHotReviewsRanking();
        cachedData = redisCacheUtils.getCacheDataLimit(cacheKey, finalLimit, HotReviewVO.class);
        return cachedData != null ? cachedData : Collections.emptyList();
    }

    /**
     * 获取特定时间段书籍榜单TopN
     * @param rankingType 榜单类型（现在统一为热门推荐）
     * @param period 时间周期（现在统一为基于用户行为）
     * @param limit 数量限制
     * @return
     */
    @Override
    public List<BookRankingVO> getBookRankingTopN(String rankingType, String period, Integer limit) {
        int finalLimit = (limit != null && limit > 0) ? limit : 10;
        String finalPeriod = normalizePeriod(period);
        String finalType = (rankingType == null || rankingType.isBlank()) ? "hot_books" : rankingType.trim().toLowerCase();

        String redisKey = resolveBookRankingCacheKey(finalType, finalPeriod);
        if (redisKey == null) {
            log.warn("暂不支持的书籍榜单类型: rankingType={}, period={}", finalType, finalPeriod);
            return Collections.emptyList();
        }

        List<BookRankingVO> cachedData = redisCacheUtils.getCacheDataLimit(redisKey, finalLimit, BookRankingVO.class);
        if (cachedData != null && !cachedData.isEmpty()) {
            return cachedData;
        }

        if ("hot_books".equals(finalType)) {
            bookRankingRefreshService.refreshHotBooksRanking();
        } else if ("book_rating".equals(finalType)) {
            bookRankingRefreshService.refreshBookRatingRanking();
        } else if ("new_books".equals(finalType)) {
            bookRankingRefreshService.refreshNewBooksRanking();
        }

        cachedData = redisCacheUtils.getCacheDataLimit(redisKey, finalLimit, BookRankingVO.class);
        if (cachedData != null && !cachedData.isEmpty()) {
            return cachedData;
        }

        // 极端情况下缓存构建失败时兜底DB查询
        return queryBookRankingsFromDb(finalType, finalPeriod, finalLimit);
    }

    private String resolveBookRankingCacheKey(String rankingType, String period) {
        if ("hot_books".equals(rankingType) || "hot".equals(rankingType)) {
            return RedisCacheConstant.RANKING_HOT_BOOKS_PREFIX + period;
        }
        if ("book_rating".equals(rankingType) || "new_books".equals(rankingType)) {
            return RedisCacheConstant.RANKING_BOOKS + ":" + rankingType + ":" + period;
        }
        return null;
    }

    private List<BookRankingVO> queryBookRankingsFromDb(String rankingType, String period, int limit) {
        LambdaQueryWrapper<Book> query = new LambdaQueryWrapper<>();
        if ("book_rating".equals(rankingType)) {
            query.ge(Book::getScoreCount, 20)
                    .orderByDesc(Book::getAverageScore)
                    .orderByDesc(Book::getScoreCount)
                    .orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + limit);
        } else if ("new_books".equals(rankingType)) {
            query.ge(Book::getCreateTime, LocalDateTime.now().minusDays(30))
                    .orderByDesc(Book::getCreateTime)
                    .orderByDesc(Book::getHotScore)
                    .last("LIMIT " + limit);
        } else if ("hot_books".equals(rankingType) || "hot".equals(rankingType)) {
            query.orderByDesc(Book::getHotScore)
                    .orderByDesc(Book::getHotScoreUpdateTime)
                    .orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + limit);
        } else {
            return Collections.emptyList();
        }

        List<Book> books = bookUserMapper.selectList(query);
        List<BookRankingVO> list = books.stream()
                .map(this::toBookRankingVO)
                .collect(Collectors.toList());
        for (int i = 0; i < list.size(); i++) {
            BookRankingVO item = list.get(i);
            item.setRank(i + 1);
            item.setRankingType(rankingType);
            item.setRankingPeriod(period);
        }
        return list;
    }

    private BookRankingVO toBookRankingVO(Book book) {
        BookRankingVO vo = new BookRankingVO();
        vo.setBookId(book.getId());
        vo.setBookTitle(book.getTitle());
        vo.setBookCover(book.getCoverUrl());
        vo.setAuthor(book.getAuthor());
        vo.setCategoryId(book.getCategoryId());
        vo.setAvgRating(book.getAverageScore());
        vo.setRatingCount(book.getScoreCount());
        vo.setReadCount(book.getFavoriteCount() != null ? book.getFavoriteCount().longValue() : 0L);
        vo.setPublishTime(book.getCreateTime());
        vo.setHotScore(book.getHotScore());
        return vo;
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "weekly";
        }
        String normalized = period.trim().toLowerCase();
        return "weekly".equals(normalized) ? normalized : "weekly";
    }

    @Override
    public List<BookRankingVO> getSimilarBooks(Long bookId, Integer limit) {
        return Collections.emptyList();
    }
}
