package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.SysRoleCreateDTO;
import com.exam.record.dto.SysRoleMenuAssignDTO;
import com.exam.record.dto.SysRoleUpdateDTO;
import com.exam.record.entity.SysRole;
import com.exam.record.vo.SysRoleVO;

import java.util.List;

/**
 * @brief 系统角色管理业务接口。
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * @brief 分页查询系统角色。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配角色编码、角色名称或备注。
     * @param status 角色状态。
     * @return 系统角色分页数据。
     */
    Page<SysRoleVO> pageRoles(long pageNo, long pageSize, String keyword, String status);

    /**
     * @brief 查询系统角色详情。
     *
     * @param id 角色ID。
     * @return 系统角色详情。
     */
    SysRoleVO getRoleDetail(Long id);

    /**
     * @brief 查询全部系统角色。
     *
     * @param status 角色状态。
     * @return 系统角色列表。
     */
    List<SysRoleVO> listRoles(String status);

    /**
     * @brief 新增系统角色。
     *
     * @param dto 新增系统角色请求对象。
     * @return 新增后的系统角色。
     */
    SysRoleVO createRole(SysRoleCreateDTO dto);

    /**
     * @brief 修改系统角色。
     *
     * @param id 角色ID。
     * @param dto 修改系统角色请求对象。
     * @return 修改后的系统角色。
     */
    SysRoleVO updateRole(Long id, SysRoleUpdateDTO dto);

    /**
     * @brief 删除系统角色。
     *
     * @param id 角色ID。
     */
    void deleteRole(Long id);

    /**
     * @brief 为角色分配菜单权限。
     *
     * @details
     * 授权时先校验角色和菜单存在性，再在同一事务内重建角色菜单关系，保证权限结果和请求菜单集合一致。
     *
     * @param id 角色ID。
     * @param dto 分配菜单权限请求对象。
     * @return 分配后的系统角色详情。
     */
    SysRoleVO assignMenus(Long id, SysRoleMenuAssignDTO dto);
}
