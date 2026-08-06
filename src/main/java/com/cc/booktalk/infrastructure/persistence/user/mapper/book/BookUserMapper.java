package com.cc.booktalk.infrastructure.persistence.user.mapper.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.booktalk.domain.entity.book.Book;
import com.cc.booktalk.domain.recommendation.model.RecommendationBookCandidate;
import com.cc.booktalk.interfaces.vo.user.rec.PersonalizedRecVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 图书主表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2025-06-30
 */
@Mapper
public interface BookUserMapper extends BaseMapper<Book> {

    /**
     * 根据书评评分重算图书评分聚合。
     *
     * @param bookId 图书ID
     * @return 更新行数
     */
    int refreshReviewScoreStats(@Param("bookId") Long bookId);

    /**
     * 批量查询推荐场景所需的图书基础信息
     *
     * @param bookIds 书籍ID列表
     * @return 推荐视图列表
     */
    List<PersonalizedRecVO> getRecBookBaseByIds(@Param("bookIds") List<Long> bookIds);

    /**
     * 无行为日志时的热门回退推荐
     *
     * @param limit 推荐数量
     * @return 回退热门列表
     */
    List<PersonalizedRecVO> getFallbackHotBooks(@Param("limit") Integer limit);

    /**
     * 按分类召回候选图书
     *
     * @param categoryIds 分类ID列表
     * @param limit 数量限制
     * @return 候选图书
     */
    List<RecommendationBookCandidate> findBooksByCategoryIds(@Param("categoryIds") List<Long> categoryIds,
                                                             @Param("limit") Integer limit);

    /**
     * 按作者召回候选图书
     *
     * @param authors 作者列表
     * @param limit 数量限制
     * @return 候选图书
     */
    List<RecommendationBookCandidate> findBooksByAuthors(@Param("authors") List<String> authors,
                                                         @Param("limit") Integer limit);
}
