package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.dto.SysMenuCreateDTO;
import com.exam.record.dto.SysMenuSortDTO;
import com.exam.record.dto.SysMenuUpdateDTO;
import com.exam.record.service.SysMenuService;
import com.exam.record.vo.SysMenuVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @brief 系统菜单管理接口。
 */
@RestController
@RequestMapping("/api/system/menus")
public class SysMenuController {
    private final SysMenuService sysMenuService;

    /**
     * @brief 构造系统菜单管理控制器。
     *
     * @param sysMenuService 系统菜单管理业务服务。
     */
    public SysMenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    /**
     * @brief 查询系统菜单列表。
     *
     * @param keyword 查询关键字。
     * @param status 菜单状态。
     * @param menuType 菜单类型。
     * @return 系统菜单列表。
     */
    @GetMapping
    public Result<List<SysMenuVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String menuType) {
        return Result.success(sysMenuService.listMenus(keyword, status, menuType));
    }

    /**
     * @brief 查询系统菜单树。
     *
     * @param keyword 查询关键字。
     * @param status 菜单状态。
     * @param menuType 菜单类型。
     * @return 系统菜单树。
     */
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> tree(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String menuType) {
        return Result.success(sysMenuService.treeMenus(keyword, status, menuType));
    }

    /**
     * @brief 查询系统菜单详情。
     *
     * @param id 菜单ID。
     * @return 系统菜单详情。
     */
    @GetMapping("/{id}")
    public Result<SysMenuVO> detail(@PathVariable Long id) {
        return Result.success(sysMenuService.getMenuDetail(id));
    }

    /**
     * @brief 新增系统菜单。
     *
     * @param dto 新增系统菜单请求对象。
     * @return 新增后的系统菜单。
     */
    @PostMapping
    public Result<SysMenuVO> create(@Valid @RequestBody SysMenuCreateDTO dto) {
        return Result.success(sysMenuService.createMenu(dto));
    }

    /**
     * @brief 修改系统菜单。
     *
     * @param id 菜单ID。
     * @param dto 修改系统菜单请求对象。
     * @return 修改后的系统菜单。
     */
    @PutMapping("/{id}")
    public Result<SysMenuVO> update(@PathVariable Long id, @Valid @RequestBody SysMenuUpdateDTO dto) {
        return Result.success(sysMenuService.updateMenu(id, dto));
    }

    /**
     * @brief 删除系统菜单。
     *
     * @param id 菜单ID。
     * @return 删除结果。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.deleteMenu(id);
        return Result.success();
    }

    /**
     * @brief 调整系统菜单排序。
     *
     * @param id 菜单ID。
     * @param dto 排序请求对象。
     * @return 调整后的系统菜单。
     */
    @PutMapping("/{id}/sort")
    public Result<SysMenuVO> sort(@PathVariable Long id, @Valid @RequestBody SysMenuSortDTO dto) {
        return Result.success(sysMenuService.sortMenu(id, dto));
    }
}
