package com.cc.booktalk.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum BehaviorTargetType {
    BOOK("BOOK"),
    REVIEW("REVIEW");

    @EnumValue
    private final String code;

    public static BehaviorTargetType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
