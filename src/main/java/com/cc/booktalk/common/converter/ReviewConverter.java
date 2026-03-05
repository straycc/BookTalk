package com.cc.booktalk.common.converter;
import com.cc.booktalk.domain.entity.review.BookReview;
import com.cc.booktalk.interfaces.vo.user.review.HotReviewVO;

public class ReviewConverter {

    public HotReviewVO  ToHotReviewsVO (BookReview  bookReview){
        HotReviewVO hotReviewVO = new HotReviewVO();
        hotReviewVO.setBookId(bookReview.getBookId());

        return hotReviewVO;
    }

}
