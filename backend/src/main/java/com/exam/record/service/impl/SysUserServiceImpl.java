package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.SysUserCreateDTO;
import com.exam.record.dto.SysUserResetPasswordDTO;
import com.exam.record.dto.SysUserUpdateDTO;
import com.exam.record.entity.SysRole;
import com.exam.record.entity.SysUser;
import com.exam.record.entity.SysUserRole;
import com.exam.record.mapper.SysRoleMapper;
import com.exam.record.mapper.SysUserMapper;
import com.exam.record.mapper.SysUserRoleMapper;
import com.exam.record.service.SysUserService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.util.PasswordUtil;
import com.exam.record.vo.SysUserVO;
import com.exam.record.vo.TokenUserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @brief 系统用户管理业务实现。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * @brief 构造系统用户管理业务实现。
     *
     * @param sysUserRoleMapper 用户角色关联 Mapper。
     * @param sysRoleMapper 角色 Mapper。
     */
    public SysUserServiceImpl(SysUserRoleMapper sysUserRoleMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    /**
     * @brief 分页查询系统用户。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配账号、姓名、手机号或邮箱。
     * @param status 用户状态。
     * @return 系统用户分页数据。
     */
    @Override
    public Page<SysUserVO> pageUsers(long pageNo, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = page(new Page<>(pageNo, pageSize), wrapper);
        Page<SysUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(toUserVOList(userPage.getRecords()));
        return voPage;
    }

    /**
     * @brief 查询系统用户详情。
     *
     * @param id 用户ID。
     * @return 系统用户详情。
     */
    @Override
    public SysUserVO getUserDetail(Long id) {
        return toUserVO(getExistingUser(id));
    }

    /**
     * @brief 新增系统用户。
     *
     * @param dto 新增系统用户请求对象。
     * @return 新增后的系统用户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO createUser(SysUserCreateDTO dto) {
        if (isUsernameExists(dto.getUsername(), null)) {
            throw new BusinessException(409, "登录账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED);
        save(user);
        replaceUserRoles(user.getId(), dto.getRoleIds());
        return toUserVO(getById(user.getId()));
    }

    /**
     * @brief 修改系统用户。
     *
     * @param id 用户ID。
     * @param dto 修改系统用户请求对象。
     * @return 修改后的系统用户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO updateUser(Long id, SysUserUpdateDTO dto) {
        SysUser user = getExistingUser(id);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        if (StringUtils.hasText(dto.getStatus())) {
            validateStatus(dto.getStatus());
            user.setStatus(dto.getStatus());
        }
        updateById(user);
        if (dto.getRoleIds() != null) {
            replaceUserRoles(id, dto.getRoleIds());
        }
        return toUserVO(getById(id));
    }

    /**
     * @brief 删除系统用户。
     *
     * @details
     * 删除前会校验用户存在性，并禁止当前登录用户删除自己的账号，避免误删后当前会话失去管理入口。
     *
     * @param id 用户ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        getExistingUser(id);
        if (isCurrentUser(id)) {
            throw new BusinessException(400, "不能删除当前登录用户");
        }
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
        removeById(id);
    }

    /**
     * @brief 启用系统用户。
     *
     * @param id 用户ID。
     * @return 启用后的系统用户。
     */
    @Override
    public SysUserVO enableUser(Long id) {
        return updateStatus(id, STATUS_ENABLED);
    }

    /**
     * @brief 禁用系统用户。
     *
     * @param id 用户ID。
     * @return 禁用后的系统用户。
     */
    @Override
    public SysUserVO disableUser(Long id) {
        if (isCurrentUser(id)) {
            throw new BusinessException(400, "不能禁用当前登录用户");
        }
        return updateStatus(id, STATUS_DISABLED);
    }

    /**
     * @brief 重置系统用户密码。
     *
     * @param id 用户ID。
     * @param dto 重置密码请求对象。
     */
    @Override
    public void resetPassword(Long id, SysUserResetPasswordDTO dto) {
        SysUser user = getExistingUser(id);
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        updateById(user);
    }

    private SysUserVO updateStatus(Long id, String status) {
        SysUser user = getExistingUser(id);
        user.setStatus(status);
        updateById(user);
        return toUserVO(getById(id));
    }

    private SysUser getExistingUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private boolean isUsernameExists(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "用户状态只能为ENABLED或DISABLED");
        }
    }

    private boolean isCurrentUser(Long id) {
        TokenUserVO currentUser = AuthContextHolder.getUser();
        return currentUser != null && id != null && id.equals(currentUser.getId());
    }

    /**
     * @brief 替换用户角色绑定。
     *
     * @details
     * 先对前端传入的角色ID去重并校验角色存在且启用，再清理旧绑定并写入新绑定，
     * 保证用户编辑保存后权限关系与页面选择完全一致。
     *
     * @param userId 用户ID。
     * @param roleIds 目标角色ID列表。
     */
    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        List<Long> distinctRoleIds = normalizeRoleIds(roleIds);
        validateRoles(distinctRoleIds);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        for (Long roleId : distinctRoleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(roleIds.stream()
                .filter(roleId -> roleId != null)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private void validateRoles(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds));
        Map<Long, SysRole> roleMap = roles.stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        for (Long roleId : roleIds) {
            SysRole role = roleMap.get(roleId);
            if (role == null) {
                throw new BusinessException(404, "角色不存在");
            }
            if (!STATUS_ENABLED.equals(role.getStatus())) {
                throw new BusinessException(400, "只能分配启用状态的角色");
            }
        }
    }

    private SysUserVO toUserVO(SysUser user) {
        if (user == null) {
            return null;
        }
        List<Long> roleIds = listUserRoleIds(user.getId());
        List<String> roleNames = listRoleNames(roleIds);
        return SysUserVO.fromEntity(user, roleIds, roleNames);
    }

    private List<SysUserVO> toUserVOList(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
        List<Long> allRoleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
        Map<Long, SysRole> roleMap = allRoleIds.isEmpty()
                ? Map.of()
                : sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, allRoleIds))
                        .stream()
                        .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        Map<Long, List<SysUserRole>> userRoleMap = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId));
        return users.stream()
                .map(user -> {
                    List<SysUserRole> bindings = userRoleMap.getOrDefault(user.getId(), List.of());
                    List<Long> roleIds = bindings.stream().map(SysUserRole::getRoleId).toList();
                    List<String> roleNames = roleIds.stream()
                            .map(roleMap::get)
                            .filter(role -> role != null)
                            .map(SysRole::getRoleName)
                            .toList();
                    return SysUserVO.fromEntity(user, roleIds, roleNames);
                })
                .toList();
    }

    private List<Long> listUserRoleIds(Long userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    private List<String> listRoleNames(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysRole> roleMap = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds))
                .stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        return roleIds.stream()
                .map(roleMap::get)
                .filter(role -> role != null)
                .map(SysRole::getRoleName)
                .toList();
    }
}
