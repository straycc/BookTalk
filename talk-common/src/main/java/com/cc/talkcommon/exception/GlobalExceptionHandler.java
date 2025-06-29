package com.cc.talkcommon.exception;


import com.cc.talkcommon.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//@RestControllerAdvice = @ControllerAdvice 和 @ResponseBody 两个注解的组合体
// RESTful API，接口统一返回 JSON
@RestControllerAdvice

public class GlobalExceptionHandler {

    // 捕获所有运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        // 返回统一错误结构
        return Result.error("运行时异常：" + e.getMessage());
    }

    // 捕获所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        return Result.error("系统异常：" + e.getMessage());
    }
}
