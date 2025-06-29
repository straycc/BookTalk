package com.cc.talkcommon.result;


import com.cc.talkcommon.constant.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回类
 * @param <T>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Result<T>{

    private Integer code;
    private String msg;
    private T data;


    //成功返回(含数据)
    public static <T> Result<T> success(T data){
        return new Result<T>(ResultCode.SUCCESS, "操作成功", data);
    }
    // 成功返回（无数据）
    public static <T> Result<T> success() {
        return new Result<T>(ResultCode.SUCCESS, "操作成功", null);
    }

    // 失败返回（自定义消息）
    public static <T> Result<T> error(String msg) {
        return new Result<T>(ResultCode.FAIL, msg, null);
    }

    // 失败返回（自定义状态码+消息）
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<T>(code, msg, null);
    }


}
