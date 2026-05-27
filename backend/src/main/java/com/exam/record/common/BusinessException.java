package com.exam.record.common;

/**
 * @brief 业务异常。
 *
 * @details
 * 用于主动中断业务流程并返回明确的业务错误码和中文错误提示。
 */
public class BusinessException extends RuntimeException {
    private final Integer code;

    /**
     * @brief 构造业务异常。
     *
     * @param code 业务错误码。
     * @param message 中文错误提示。
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @brief 获取业务错误码。
     *
     * @return 业务错误码。
     */
    public Integer getCode() {
        return code;
    }
}
