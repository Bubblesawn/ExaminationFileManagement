package com.exam.record.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief 登录成功响应数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private LoginUserVO user;
}
