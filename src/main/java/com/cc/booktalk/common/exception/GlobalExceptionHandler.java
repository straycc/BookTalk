package com.cc.booktalk.common.exception;


import com.cc.booktalk.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;
import lombok.extern.slf4j.Slf4j;

//@RestControllerAdvice = @ControllerAdvice 和 @ResponseBody 两个注解的组合体
// RESTful API，接口统一返回 JSON
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. 捕获自定义业务异常 BaseException
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e) {
        log.debug("业务处理失败: code={}, message={}", e.getCode(), e.getMsg());
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else {
            message = ((BindException) e).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return Result.error(400, message);
    }

    // 2. 捕获运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("未处理的运行时异常", e);
        return Result.error("系统处理失败，请稍后重试");
    }

    // 3. 捕获所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("未处理的系统异常", e);
        return Result.error("系统处理失败，请稍后重试");
    }
}
