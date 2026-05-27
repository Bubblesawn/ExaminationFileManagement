package com.exam.record.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @brief 分配角色菜单权限请求对象。
 */
@Data
public class SysRoleMenuAssignDTO {
    @NotNull(message = "不能为空")
    private List<Long> menuIds;
}
