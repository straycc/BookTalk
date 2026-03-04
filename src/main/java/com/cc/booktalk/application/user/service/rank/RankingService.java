package com.cc.booktalk.application.user.service.rank;

import com.cc.booktalk.entity.dto.ranking.RankingQueryDTO;
import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.vo.BookRankingVO;
import com.cc.booktalk.entity.vo.HotReviewVO;
import com.cc.booktalk.entity.vo.PersonalizedRecVO;

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
     * 获取热门书评列表
     * @param queryDTO 查询参数
     * @return 热门书评列表
     */
    List<HotReviewVO> getHotReviews(RankingQueryDTO queryDTO);

    /**
     * 获取热门书籍榜单
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
     * 获取热门书评Top N
     * @param period 时间周期
     * @param limit 数量限制
     * @return 热门书评列表
     */
    List<HotReviewVO> getHotReviewsTopN(String period, Integer limit);

    /**
     * 获取热门书籍Top
     * @param rankingType 榜单类型（现在统一为热门推荐）
     * @param period 时间周期（现在统一为基于用户行为）
     * @param limit 数量限制
     * @return 书籍榜单列表
     */
    List<BookRankingVO> getBookRankingTopN(String rankingType, String period, Integer limit);

    /**
     * 获取热门书籍推荐（直接调用推荐系统）
     * @param limit 数量限制
     * @return 推荐书籍列表
     */
    List<PersonalizedRecVO> getHotBooks(Integer limit);

    /**
     * 更新热门书评榜单（保持原有逻辑）
     * @param period 时间周期
     */
    void updateHotReviewsRanking(String period);

    /**
     * 更新书籍榜单（现在简化为缓存热门推荐）
     * @param rankingType 榜单类型
     * @param period 时间周期
     */
    void updateBookRanking(String rankingType, String period);
}