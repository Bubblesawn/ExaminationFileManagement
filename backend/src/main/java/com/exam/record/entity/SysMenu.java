package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 系统菜单实体。
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String routePath;
    private String componentPath;
    private String permissionCode;
    private String icon;
    private Integer menuSort;
    private Integer visible;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
