package com.exam.record.vo;

import com.exam.record.entity.SysMenu;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @brief 系统菜单响应对象。
 */
@Data
public class SysMenuVO {
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
    private List<SysMenuVO> children = new ArrayList<>();

    /**
     * @brief 将系统菜单实体转换为响应对象。
     *
     * @param menu 系统菜单实体。
     * @return 系统菜单响应对象。
     */
    public static SysMenuVO fromEntity(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setRoutePath(menu.getRoutePath());
        vo.setComponentPath(menu.getComponentPath());
        vo.setPermissionCode(menu.getPermissionCode());
        vo.setIcon(menu.getIcon());
        vo.setMenuSort(menu.getMenuSort());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setCreateTime(menu.getCreateTime());
        vo.setUpdateTime(menu.getUpdateTime());
        return vo;
    }
}
