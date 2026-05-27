package com.exam.record.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.SysMenuCreateDTO;
import com.exam.record.dto.SysMenuSortDTO;
import com.exam.record.dto.SysMenuUpdateDTO;
import com.exam.record.entity.SysMenu;
import com.exam.record.vo.SysMenuVO;

import java.util.List;

/**
 * @brief 系统菜单管理业务接口。
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * @brief 查询系统菜单列表。
     *
     * @param keyword 查询关键字，可匹配菜单名称、路由路径或权限标识。
     * @param status 菜单状态。
     * @param menuType 菜单类型。
     * @return 系统菜单列表。
     */
    List<SysMenuVO> listMenus(String keyword, String status, String menuType);

    /**
     * @brief 查询系统菜单树。
     *
     * @param keyword 查询关键字，可匹配菜单名称、路由路径或权限标识。
     * @param status 菜单状态。
     * @param menuType 菜单类型。
     * @return 系统菜单树。
     */
    List<SysMenuVO> treeMenus(String keyword, String status, String menuType);

    /**
     * @brief 查询系统菜单详情。
     *
     * @param id 菜单ID。
     * @return 系统菜单详情。
     */
    SysMenuVO getMenuDetail(Long id);

    /**
     * @brief 新增系统菜单。
     *
     * @param dto 新增系统菜单请求对象。
     * @return 新增后的系统菜单。
     */
    SysMenuVO createMenu(SysMenuCreateDTO dto);

    /**
     * @brief 修改系统菜单。
     *
     * @param id 菜单ID。
     * @param dto 修改系统菜单请求对象。
     * @return 修改后的系统菜单。
     */
    SysMenuVO updateMenu(Long id, SysMenuUpdateDTO dto);

    /**
     * @brief 删除系统菜单。
     *
     * @param id 菜单ID。
     */
    void deleteMenu(Long id);

    /**
     * @brief 调整系统菜单排序。
     *
     * @param id 菜单ID。
     * @param dto 排序请求对象。
     * @return 调整后的系统菜单。
     */
    SysMenuVO sortMenu(Long id, SysMenuSortDTO dto);
}
