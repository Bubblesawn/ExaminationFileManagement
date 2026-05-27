package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.SysRoleCreateDTO;
import com.exam.record.dto.SysRoleMenuAssignDTO;
import com.exam.record.dto.SysRoleUpdateDTO;
import com.exam.record.entity.SysMenu;
import com.exam.record.entity.SysRole;
import com.exam.record.entity.SysRoleMenu;
import com.exam.record.entity.SysUserRole;
import com.exam.record.mapper.SysMenuMapper;
import com.exam.record.mapper.SysRoleMapper;
import com.exam.record.mapper.SysRoleMenuMapper;
import com.exam.record.mapper.SysUserRoleMapper;
import com.exam.record.service.SysRoleService;
import com.exam.record.vo.SysRoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @brief 系统角色管理业务实现。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String DATA_SCOPE_ALL = "ALL";
    private static final String DATA_SCOPE_SELF = "SELF";

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * @brief 构造系统角色管理业务实现。
     *
     * @param sysMenuMapper 系统菜单 Mapper。
     * @param sysRoleMenuMapper 角色菜单关联 Mapper。
     * @param sysUserRoleMapper 用户角色关联 Mapper。
     */
    public SysRoleServiceImpl(SysMenuMapper sysMenuMapper,
                              SysRoleMenuMapper sysRoleMenuMapper,
                              SysUserRoleMapper sysUserRoleMapper) {
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    /**
     * @brief 分页查询系统角色。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配角色编码、角色名称或备注。
     * @param status 角色状态。
     * @return 系统角色分页数据。
     */
    @Override
    public Page<SysRoleVO> pageRoles(long pageNo, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<SysRole> wrapper = buildRoleQueryWrapper(keyword, status);
        Page<SysRole> rolePage = page(new Page<>(pageNo, pageSize), wrapper);
        Page<SysRoleVO> voPage = new Page<>(rolePage.getCurrent(), rolePage.getSize(), rolePage.getTotal());
        voPage.setRecords(rolePage.getRecords().stream()
                .map(role -> SysRoleVO.fromEntity(role, listMenuIds(role.getId())))
                .toList());
        return voPage;
    }

    /**
     * @brief 查询系统角色详情。
     *
     * @param id 角色ID。
     * @return 系统角色详情。
     */
    @Override
    public SysRoleVO getRoleDetail(Long id) {
        SysRole role = getExistingRole(id);
        return SysRoleVO.fromEntity(role, listMenuIds(id));
    }

    /**
     * @brief 查询全部系统角色。
     *
     * @param status 角色状态。
     * @return 系统角色列表。
     */
    @Override
    public List<SysRoleVO> listRoles(String status) {
        LambdaQueryWrapper<SysRole> wrapper = buildRoleQueryWrapper(null, status);
        return list(wrapper).stream()
                .map(role -> SysRoleVO.fromEntity(role, listMenuIds(role.getId())))
                .toList();
    }

    /**
     * @brief 新增系统角色。
     *
     * @param dto 新增系统角色请求对象。
     * @return 新增后的系统角色。
     */
    @Override
    public SysRoleVO createRole(SysRoleCreateDTO dto) {
        if (isRoleCodeExists(dto.getRoleCode(), null)) {
            throw new BusinessException(409, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        role.setDataScope(StringUtils.hasText(dto.getDataScope()) ? dto.getDataScope() : DATA_SCOPE_ALL);
        role.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED);
        role.setRemark(dto.getRemark());
        validateDataScope(role.getDataScope());
        validateStatus(role.getStatus());
        save(role);
        return SysRoleVO.fromEntity(role, List.of());
    }

    /**
     * @brief 修改系统角色。
     *
     * @param id 角色ID。
     * @param dto 修改系统角色请求对象。
     * @return 修改后的系统角色。
     */
    @Override
    public SysRoleVO updateRole(Long id, SysRoleUpdateDTO dto) {
        SysRole role = getExistingRole(id);
        role.setRoleName(dto.getRoleName());
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        if (StringUtils.hasText(dto.getDataScope())) {
            validateDataScope(dto.getDataScope());
            role.setDataScope(dto.getDataScope());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            validateStatus(dto.getStatus());
            role.setStatus(dto.getStatus());
        }
        role.setRemark(dto.getRemark());
        updateById(role);
        return getRoleDetail(id);
    }

    /**
     * @brief 删除系统角色。
     *
     * @details
     * 删除前校验角色存在性和用户绑定情况，只有未分配给任何用户的角色才允许删除，同时清理该角色菜单权限。
     *
     * @param id 角色ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        getExistingRole(id);
        Long usedCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (usedCount > 0) {
            throw new BusinessException(400, "角色已分配给用户，不能删除");
        }
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
        removeById(id);
    }

    /**
     * @brief 为角色分配菜单权限。
     *
     * @details
     * 授权时先对菜单ID去重，再校验所有菜单存在，随后清理旧权限并写入新权限，避免局部更新造成权限残留。
     *
     * @param id 角色ID。
     * @param dto 分配菜单权限请求对象。
     * @return 分配后的系统角色详情。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO assignMenus(Long id, SysRoleMenuAssignDTO dto) {
        getExistingRole(id);
        List<Long> menuIds = normalizeMenuIds(dto.getMenuIds());
        validateMenusExist(menuIds);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(id);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
        return getRoleDetail(id);
    }

    private LambdaQueryWrapper<SysRole> buildRoleQueryWrapper(String keyword, String status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysRole::getRoleCode, keyword)
                    .or()
                    .like(SysRole::getRoleName, keyword)
                    .or()
                    .like(SysRole::getRemark, keyword));
        }
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByAsc(SysRole::getRoleSort).orderByDesc(SysRole::getCreateTime);
        return wrapper;
    }

    private SysRole getExistingRole(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    private List<Long> listMenuIds(Long roleId) {
        return sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    private List<Long> normalizeMenuIds(List<Long> menuIds) {
        Set<Long> idSet = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null || menuId <= 0) {
                throw new BusinessException(400, "菜单ID不能为空且必须大于0");
            }
            idSet.add(menuId);
        }
        return new ArrayList<>(idSet);
    }

    private void validateMenusExist(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds));
        if (count != menuIds.size()) {
            throw new BusinessException(400, "存在无效的菜单ID");
        }
    }

    private boolean isRoleCodeExists(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "角色状态只能为ENABLED或DISABLED");
        }
    }

    private void validateDataScope(String dataScope) {
        if (!DATA_SCOPE_ALL.equals(dataScope) && !DATA_SCOPE_SELF.equals(dataScope)) {
            throw new BusinessException(400, "数据权限范围只能为ALL或SELF");
        }
    }
}
