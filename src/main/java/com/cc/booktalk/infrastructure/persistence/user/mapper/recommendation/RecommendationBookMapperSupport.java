package com.cc.booktalk.infrastructure.persistence.user.mapper.recommendation;

import com.cc.booktalk.application.user.service.recommendation.model.RecommendationBookCandidate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐书籍查询补充接口
 */
public interface RecommendationBookMapperSupport {

    List<RecommendationBookCandidate> getBooksByTagName(@Param("tagName") String tagName,
                                                        @Param("limit") Integer limit);

    List<RecommendationBookCandidate> findBooksByCategoryIds(@Param("categoryIds") List<Long> categoryIds,
                                                             @Param("limit") Integer limit);

    List<RecommendationBookCandidate> findBooksByAuthors(@Param("authors") List<String> authors,
                                                         @Param("limit") Integer limit);
}
