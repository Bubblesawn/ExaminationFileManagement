package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 角色菜单关联实体。
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    private Long id;
    private Long roleId;
    private Long menuId;
    private LocalDateTime createTime;
}
