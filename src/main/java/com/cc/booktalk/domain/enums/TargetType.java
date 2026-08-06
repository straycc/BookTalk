package com.cc.booktalk.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TargetType {
    BOOK("BOOK"),
    REVIEW("REVIEW"),
    POST("POST"),
    COMMENT("COMMENT");

    @EnumValue
    private final String code;
}
