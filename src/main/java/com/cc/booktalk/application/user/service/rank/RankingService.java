package com.cc.booktalk.application.user.service.rank;

import com.cc.booktalk.interfaces.dto.user.ranking.RankingQueryDTO;
import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.interfaces.vo.user.ranking.BookRankingVO;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;

import java.util.List;

/**
 * 榜单推荐服务接口
 * 统一使用推荐系统作为权威计算引擎
 *
 * @author cc
 * @since 2025-10-13
 */
public interface RankingService {

    /**
     * 获取书籍榜单分页（完整榜单）
     */
    PageResult<BookRankingVO> getBookRankings(RankingQueryDTO queryDTO);

    /**
     * 获取热门书评榜单Top N
     */
    List<HotReviewVO> getHotReviewRankingTopN(String period, Integer limit);

    /**
     * 获取指定书籍的相似推荐
     * @param bookId 书籍ID
     * @param limit 推荐数量
     * @return 推荐书籍列表
     */
    List<BookRankingVO> getSimilarBooks(Long bookId, Integer limit);


    /**
     * 获取热门书籍Top
     * @param rankingType 榜单类型（现在统一为热门推荐）
     * @param period 时间周期（现在统一为基于用户行为）
     * @param limit 数量限制
     * @return 书籍榜单列表
     */
    List<BookRankingVO> getBookRankingTopN(String rankingType, String period, Integer limit);
}
