package com.cc.booktalk.common.converter;
import com.cc.booktalk.entity.entity.review.BookReview;
import com.cc.booktalk.entity.vo.HotReviewVO;

public class ReviewConverter {

    public HotReviewVO  ToHotReviewsVO (BookReview  bookReview){
        HotReviewVO hotReviewVO = new HotReviewVO();
        hotReviewVO.setBookId(bookReview.getBookId());

        return hotReviewVO;
    }

}
