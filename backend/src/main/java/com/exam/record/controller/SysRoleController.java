package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.SysRoleCreateDTO;
import com.exam.record.dto.SysRoleMenuAssignDTO;
import com.exam.record.dto.SysRoleUpdateDTO;
import com.exam.record.service.SysRoleService;
import com.exam.record.vo.SysRoleVO;
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
 * @brief 系统角色管理接口。
 */
@RestController
@RequestMapping("/api/system/roles")
public class SysRoleController {
    private final SysRoleService sysRoleService;

    /**
     * @brief 构造系统角色管理控制器。
     *
     * @param sysRoleService 系统角色管理业务服务。
     */
    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    /**
     * @brief 分页查询系统角色。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param status 角色状态。
     * @return 系统角色分页数据。
     */
    @GetMapping("/page")
    public Result<Page<SysRoleVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(sysRoleService.pageRoles(pageNo, pageSize, keyword, status));
    }

    /**
     * @brief 查询全部系统角色。
     *
     * @param status 角色状态。
     * @return 系统角色列表。
     */
    @GetMapping
    public Result<List<SysRoleVO>> list(@RequestParam(required = false) String status) {
        return Result.success(sysRoleService.listRoles(status));
    }

    /**
     * @brief 查询系统角色详情。
     *
     * @param id 角色ID。
     * @return 系统角色详情。
     */
    @GetMapping("/{id}")
    public Result<SysRoleVO> detail(@PathVariable Long id) {
        return Result.success(sysRoleService.getRoleDetail(id));
    }

    /**
     * @brief 新增系统角色。
     *
     * @param dto 新增系统角色请求对象。
     * @return 新增后的系统角色。
     */
    @PostMapping
    public Result<SysRoleVO> create(@Valid @RequestBody SysRoleCreateDTO dto) {
        return Result.success(sysRoleService.createRole(dto));
    }

    /**
     * @brief 修改系统角色。
     *
     * @param id 角色ID。
     * @param dto 修改系统角色请求对象。
     * @return 修改后的系统角色。
     */
    @PutMapping("/{id}")
    public Result<SysRoleVO> update(@PathVariable Long id, @Valid @RequestBody SysRoleUpdateDTO dto) {
        return Result.success(sysRoleService.updateRole(id, dto));
    }

    /**
     * @brief 删除系统角色。
     *
     * @param id 角色ID。
     * @return 删除结果。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return Result.success();
    }

    /**
     * @brief 为角色分配菜单权限。
     *
     * @param id 角色ID。
     * @param dto 分配菜单权限请求对象。
     * @return 分配后的系统角色详情。
     */
    @PutMapping("/{id}/menus")
    public Result<SysRoleVO> assignMenus(@PathVariable Long id, @Valid @RequestBody SysRoleMenuAssignDTO dto) {
        return Result.success(sysRoleService.assignMenus(id, dto));
    }
}
