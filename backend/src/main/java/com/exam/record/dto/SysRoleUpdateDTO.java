package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 修改系统角色请求对象。
 */
@Data
public class SysRoleUpdateDTO {
    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过64位")
    private String roleName;

    private Integer roleSort;

    @Pattern(regexp = "ALL|SELF", message = "只能为ALL或SELF")
    private String dataScope;

    @Pattern(regexp = "ENABLED|DISABLED", message = "只能为ENABLED或DISABLED")
    private String status;

    @Size(max = 512, message = "长度不能超过512位")
    private String remark;
}
