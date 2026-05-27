package com.exam.record.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief 登录用户基础信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVO {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
}
