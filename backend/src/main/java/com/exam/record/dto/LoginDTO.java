package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 登录请求参数。
 */
@Data
public class LoginDTO {
    /**
     * @brief 登录账号。
     */
    @NotBlank(message = "不能为空")
    private String username;

    /**
     * @brief 登录密码。
     */
    @NotBlank(message = "不能为空")
    private String password;
}
