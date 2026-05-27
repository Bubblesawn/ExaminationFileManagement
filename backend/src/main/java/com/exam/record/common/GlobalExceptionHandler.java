package com.exam.record.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @brief 全局异常处理器。
 *
 * @details
 * 统一拦截控制层异常，保证前端始终获得结构一致的响应数据。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @brief 处理业务异常。
     *
     * @param exception 业务异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * @brief 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + error.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(400, message);
    }

    /**
     * @brief 处理路径或查询参数校验异常。
     *
     * @param exception 参数校验异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return Result.fail(400, exception.getMessage());
    }

    /**
     * @brief 处理未预期系统异常。
     *
     * @param exception 系统异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(500, "系统异常：" + exception.getMessage());
    }
}
