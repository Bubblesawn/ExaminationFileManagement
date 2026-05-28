package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.record.entity.SysMenu;
import com.exam.record.entity.SysRole;
import com.exam.record.entity.SysRoleMenu;
import com.exam.record.entity.SysUserRole;
import com.exam.record.mapper.SysMenuMapper;
import com.exam.record.mapper.SysRoleMapper;
import com.exam.record.mapper.SysRoleMenuMapper;
import com.exam.record.mapper.SysUserRoleMapper;
import com.exam.record.service.UserPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @brief 当前登录用户权限查询服务实现。
 *
 * @details
 * 根据用户角色关联、启用角色、启用菜单和角色菜单关联计算最终权限，供登录响应、
 * 前端菜单过滤和后端接口拦截共同使用。
 */
@Service
public class UserPermissionServiceImpl implements UserPermissionService {
    private static final String STATUS_ENABLED = "ENABLED";

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    /**
     * @brief 构造用户权限查询服务。
     *
     * @param sysUserRoleMapper 用户角色关联 Mapper。
     * @param sysRoleMapper 角色 Mapper。
     * @param sysRoleMenuMapper 角色菜单关联 Mapper。
     * @param sysMenuMapper 菜单 Mapper。
     */
    public UserPermissionServiceImpl(SysUserRoleMapper sysUserRoleMapper,
                                     SysRoleMapper sysRoleMapper,
                                     SysRoleMenuMapper sysRoleMenuMapper,
                                     SysMenuMapper sysMenuMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysMenuMapper = sysMenuMapper;
    }

    /**
     * @brief 查询用户已启用角色编码。
     *
     * @param userId 用户ID。
     * @return 角色编码列表。
     */
    @Override
    public List<String> listRoleCodes(Long userId) {
        return listEnabledRoles(userId).stream()
                .map(SysRole::getRoleCode)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * @brief 查询用户已启用菜单权限码。
     *
     * @param userId 用户ID。
     * @return 菜单权限码集合。
     */
    @Override
    public Set<String> listPermissions(Long userId) {
        List<Long> roleIds = listEnabledRoles(userId).stream()
                .map(SysRole::getId)
                .toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            return Set.of();
        }

        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getStatus, STATUS_ENABLED))
                .stream()
                .map(SysMenu::getPermissionCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SysRole> listEnabledRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Map<Long, SysRole> roleMap = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, STATUS_ENABLED))
                .stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        return roleIds.stream()
                .map(roleMap::get)
                .filter(role -> role != null)
                .toList();
    }
}
