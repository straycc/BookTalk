package com.cc.talkcommon.exception;


import com.cc.talkcommon.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//@RestControllerAdvice = @ControllerAdvice 和 @ResponseBody 两个注解的组合体
// RESTful API，接口统一返回 JSON
@RestControllerAdvice

public class GlobalExceptionHandler {

    // 1. 捕获自定义业务异常 BaseException
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e) {
        // 直接返回异常中定义的错误码和信息
        return Result.error(e.getCode(), e.getMsg());
    }

    // 2. 捕获运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();  // 生产环境可换成日志打印
        return Result.error("运行时异常：" + e.getMessage());
    }

    // 3. 捕获所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统异常：" + e.getMessage());
    }
}
