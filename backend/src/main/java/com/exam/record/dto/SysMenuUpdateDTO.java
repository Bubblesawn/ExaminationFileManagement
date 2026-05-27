package com.exam.record.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 修改系统菜单请求对象。
 */
@Data
public class SysMenuUpdateDTO {
    @Min(value = 0, message = "不能小于0")
    private Long parentId;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过64位")
    private String menuName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "CATALOG|MENU|BUTTON", message = "只能为CATALOG、MENU或BUTTON")
    private String menuType;

    @Size(max = 255, message = "长度不能超过255位")
    private String routePath;

    @Size(max = 255, message = "长度不能超过255位")
    private String componentPath;

    @Size(max = 128, message = "长度不能超过128位")
    private String permissionCode;

    @Size(max = 64, message = "长度不能超过64位")
    private String icon;

    @Min(value = 0, message = "不能小于0")
    private Integer menuSort;

    @Min(value = 0, message = "只能为0或1")
    @Max(value = 1, message = "只能为0或1")
    private Integer visible;

    @Pattern(regexp = "ENABLED|DISABLED", message = "只能为ENABLED或DISABLED")
    private String status;
}
