package com.exam.record.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Token 校验后的登录用户信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUserVO {
    private Long id;
    private String username;
    private String realName;
}
