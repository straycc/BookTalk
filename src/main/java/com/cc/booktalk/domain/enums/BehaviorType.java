package com.cc.booktalk.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum BehaviorType {
    BOOK_VIEW("BOOK_VIEW"),
    BOOK_LIKE("BOOK_LIKE"),
    BOOK_COLLECT("BOOK_COLLECT"),
    BOOK_UNCOLLECT("BOOK_UNCOLLECT"),
    BOOK_SCORE("BOOK_SCORE"),
    BOOK_REVIEW("BOOK_REVIEW"),
    REVIEW_COMMENT("REVIEW_COMMENT"),
    REVIEW_LIKE("REVIEW_LIKE"),
    REVIEW_REPLY("REVIEW_REPLY"),
    BOOK_CLICK_RECOMMEND("BOOK_CLICK_RECOMMEND");

    @EnumValue
    private final String code;

    public static BehaviorType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
