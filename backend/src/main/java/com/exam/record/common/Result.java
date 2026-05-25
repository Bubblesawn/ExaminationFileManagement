package com.exam.record.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief 后端接口统一响应对象。
 *
 * @tparam T 响应数据类型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * @brief 构造成功响应。
     *
     * @param data 响应数据。
     * @return 成功响应对象。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * @brief 构造无数据成功响应。
     *
     * @return 成功响应对象。
     */
    public static Result<Void> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * @brief 构造失败响应。
     *
     * @param code 业务错误码。
     * @param message 错误提示。
     * @return 失败响应对象。
     */
    public static Result<Void> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}

