package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 重置系统用户密码请求对象。
 */
@Data
public class SysUserResetPasswordDTO {
    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 64, message = "长度必须在6到64位之间")
    private String password;
}
