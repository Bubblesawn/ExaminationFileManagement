package com.exam.record.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 修改系统用户请求对象。
 */
@Data
public class SysUserUpdateDTO {
    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过64位")
    private String realName;

    @Size(max = 32, message = "长度不能超过32位")
    private String phone;

    @Email(message = "格式不正确")
    @Size(max = 128, message = "长度不能超过128位")
    private String email;

    @Size(max = 512, message = "长度不能超过512位")
    private String avatar;

    @Pattern(regexp = "ENABLED|DISABLED", message = "只能为ENABLED或DISABLED")
    private String status;
}
