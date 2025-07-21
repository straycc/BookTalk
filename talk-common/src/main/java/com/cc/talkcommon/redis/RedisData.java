package com.cc.talkcommon.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisData<T> {
    private T data;              // 实际数据，如 BookVO
    private LocalDateTime expireTime; // 逻辑过期时间
}
