package com.cc.talkpojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeTargetType {

    BOOK_REVIEW( "bookReview"),
    COMMENT("comment"),
    BOOKLIST("bookList");

    @EnumValue
    private final String code;
}
