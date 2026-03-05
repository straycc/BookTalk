package com.cc.booktalk.application.user.service.rank.impl;

import com.cc.booktalk.common.constant.RedisCacheConstant;
import com.cc.booktalk.common.redis.RedisCacheUtils;
import com.cc.booktalk.interfaces.dto.user.ranking.RankingQueryDTO;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import com.cc.booktalk.application.user.service.rank.RankingRefreshService;
import com.cc.booktalk.application.user.service.rank.RankingService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RankingServiceImpl implements RankingService {

    @Resource
    private RedisCacheUtils redisCacheUtils;

    @Resource
    private RedissonClient redissonClient;


    @Resource
    private RankingRefreshService rankingRefreshService;

    // 热门书评缓存重建lock key
    private static final String HOT_REVIEWS_RANKING_LOCK = "HOT_REVIEWS_RANKING_LOCK";

    // 热门书评缓存key
    private static final String HOT_REVIEWS_CACHE_KEY = RedisCacheConstant.RANKING_HOT_REVIEWS_PREFIX + "weekly";



    /**
     * 查询热门书评
     * @param queryDTO 查询参数
     * @return
     */
    @Override
    public List<HotReviewVO> getHotReviews(RankingQueryDTO queryDTO) {
        int hotReviewCount = queryDTO.getLimit();

        //  从Redis获取缓存数据
        List<HotReviewVO> cachedData = redisCacheUtils.getCacheDataLimit(
                HOT_REVIEWS_CACHE_KEY, hotReviewCount, HotReviewVO.class
        );

        // 数据存在就直接返回
        if (cachedData != null && !cachedData.isEmpty()) {
            log.debug("返回缓存的热门书评: count={}", cachedData.size());
            return cachedData;
        }

        // 数据为空/不存在，异步重建缓存
        log.info("缓存为空，触发异步重建");
         asyncUpdateHotReviewsCache(hotReviewCount);
        return getFallbackHotReviews(hotReviewCount);

    }

    /**
     * 缓存异步重建
     * @return
     */
    private List<HotReviewVO>  asyncUpdateHotReviewsCache(int limit) {
        RLock lock = redissonClient.getLock(HOT_REVIEWS_RANKING_LOCK);
        try {
            boolean isLock = lock.tryLock(0,60, TimeUnit.SECONDS);
            if(isLock){
                log.info("成功获取分布式锁，开始热门书评缓存重建");
                //二次校验是否已经重建了缓存
                List<HotReviewVO> cachedDataSecond = redisCacheUtils.getCacheDataLimit(HOT_REVIEWS_CACHE_KEY,limit, HotReviewVO.class);
                if(cachedDataSecond != null && !cachedDataSecond.isEmpty()){
                    return cachedDataSecond;
                }
                // 调用应用服务重建缓存
                rankingRefreshService.updateWeeklyHotReviewsToRedis();
            }
        } catch (Exception e) {
            log.error("重建缓存时发生异常", e);

        }
        finally {
            lock.unlock();
            log.info("释放分布式锁");
        }

        return Collections.emptyList();
    }

    /**
     * 获取降级数据
     */
    private List<HotReviewVO> getFallbackHotReviews(int hotReviewCount) {
//        try {
//            // 查询最近3天的书评作为降级数据
//            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
//
//            LambdaQueryWrapper<BookReview> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.ge(BookReview::getCreateTime, threeDaysAgo)
//                    .orderByDesc(BookReview::getCreateTime)
//                    .last("LIMIT " + hotReviewCount);
//
//            List<BookReview> reviews = reviewUserMapper.selectList(queryWrapper);
//
//            return reviews.stream()
//                    .map(this::convertToHotReviewVO)
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("获取降级数据失败", e);
//            return Collections.emptyList();
//        }
        return new ArrayList<HotReviewVO>();
    }


    @Override
    public PageResult<BookRankingVO> getBookRankings(RankingQueryDTO queryDTO) {
        return null;
    }

    @Override
    public List<BookRankingVO> getSimilarBooks(Long bookId, Integer limit) {
        return List.of();
    }

    @Override
    public List<HotReviewVO> getHotReviewsTopN(String period, Integer limit) {
        return List.of();
    }

    @Override
    public List<BookRankingVO> getBookRankingTopN(String rankingType, String period, Integer limit) {
        return List.of();
    }

    @Override
    public List<PersonalizedRecVO> getHotBooks(Integer limit) {
        return List.of();
    }

    @Override
    public void updateHotReviewsRanking(String period) {

    }

    @Override
    public void updateBookRanking(String rankingType, String period) {

    }
}
