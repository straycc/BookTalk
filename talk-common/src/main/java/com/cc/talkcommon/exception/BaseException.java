package com.cc.talkcommon.exception;


import lombok.Getter;

/**
 * 自定义业务异常基类
 */
@Getter
public class BaseException extends RuntimeException {

    private final Integer code;
    private final String msg;

    public BaseException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }

    public BaseException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

}
