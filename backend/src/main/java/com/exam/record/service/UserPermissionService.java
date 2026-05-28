package com.exam.record.service;

import java.util.List;
import java.util.Set;

/**
 * @brief 当前登录用户权限查询服务。
 */
public interface UserPermissionService {
    /**
     * @brief 查询用户已启用角色编码。
     *
     * @param userId 用户ID。
     * @return 角色编码列表。
     */
    List<String> listRoleCodes(Long userId);

    /**
     * @brief 查询用户已启用菜单权限码。
     *
     * @param userId 用户ID。
     * @return 菜单权限码集合。
     */
    Set<String> listPermissions(Long userId);
}
