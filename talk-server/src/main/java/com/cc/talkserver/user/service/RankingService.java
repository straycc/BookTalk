package com.cc.talkserver.user.service;

import com.cc.talkpojo.dto.RankingQueryDTO;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.vo.BookRankingVO;
import com.cc.talkpojo.vo.HotReviewVO;

import java.util.List;

/**
 * 榜单推荐服务接口
 *
 * @author cc
 * @since 2025-10-13
 */
public interface RankingService {

    /**
     * 获取热门书评列表
     * @param queryDTO 查询参数
     * @return 热门书评列表
     */
    PageResult<HotReviewVO> getHotReviews(RankingQueryDTO queryDTO);

    /**
     * 获取书籍榜单
     * @param queryDTO 查询参数
     * @return 书籍榜单列表
     */
    PageResult<BookRankingVO> getBookRankings(RankingQueryDTO queryDTO);

    /**
     * 获取指定书籍的相似推荐
     * @param bookId 书籍ID
     * @param limit 推荐数量
     * @return 推荐书籍列表
     */
    List<BookRankingVO> getSimilarBooks(Long bookId, Integer limit);

    /**
     * 更新热门书评榜单
     * @param period 时间周期
     */
    void updateHotReviewsRanking(String period);

    /**
     * 更新书籍榜单
     * @param rankingType 榜单类型
     * @param period 时间周期
     */
    void updateBookRanking(String rankingType, String period);

    /**
     * 获取热门书评Top N
     * @param period 时间周期
     * @param limit 数量限制
     * @return 热门书评列表
     */
    List<HotReviewVO> getHotReviewsTopN(String period, Integer limit);

    /**
     * 获取书籍榜单Top N
     * @param rankingType 榜单类型
     * @param period 时间周期
     * @param limit 数量限制
     * @return 书籍榜单列表
     */
    List<BookRankingVO> getBookRankingTopN(String rankingType, String period, Integer limit);
}