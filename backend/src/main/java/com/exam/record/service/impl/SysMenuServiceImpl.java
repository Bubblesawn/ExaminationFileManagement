package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.SysMenuCreateDTO;
import com.exam.record.dto.SysMenuSortDTO;
import com.exam.record.dto.SysMenuUpdateDTO;
import com.exam.record.entity.SysMenu;
import com.exam.record.mapper.SysMenuMapper;
import com.exam.record.service.SysMenuService;
import com.exam.record.vo.SysMenuVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @brief 系统菜单管理业务实现。
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    private static final long ROOT_PARENT_ID = 0L;
    private static final int DEFAULT_MENU_SORT = 0;
    private static final int VISIBLE_YES = 1;
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String TYPE_CATALOG = "CATALOG";
    private static final String TYPE_MENU = "MENU";
    private static final String TYPE_BUTTON = "BUTTON";

    /**
     * @brief 查询系统菜单列表。
     *
     * @param keyword 查询关键字，可匹配菜单名称、路由路径或权限标识。
     * @param status 菜单状态。
     * @param menuType 菜单类型。
     * @return 系统菜单列表。
     */
    @Override
    public List<SysMenuVO> listMenus(String keyword, String status, String menuType) {
        return list(buildQueryWrapper(keyword, status, menuType)).stream()
                .map(SysMenuVO::fromEntity)
                .toList();
    }

    /**
     * @brief 查询系统菜单树。
     *
     * @details
     * 先按查询条件获取扁平菜单，再按 parentId 组装父子结构。若父节点未包含在查询结果中，
     * 当前节点会作为顶层节点返回，避免关键字过滤时有效菜单被静默丢弃。
     *
     * @param keyword 查询关键字，可匹配菜单名称、路由路径或权限标识。
     * @param status 菜单状态。
     * @param menuType 菜单类型。
     * @return 系统菜单树。
     */
    @Override
    public List<SysMenuVO> treeMenus(String keyword, String status, String menuType) {
        List<SysMenuVO> menus = listMenus(keyword, status, menuType);
        Map<Long, SysMenuVO> menuMap = new LinkedHashMap<>();
        menus.forEach(menu -> menuMap.put(menu.getId(), menu));

        List<SysMenuVO> roots = new ArrayList<>();
        for (SysMenuVO menu : menus) {
            SysMenuVO parent = menuMap.get(menu.getParentId());
            if (menu.getParentId() != null && menu.getParentId() != ROOT_PARENT_ID && parent != null) {
                parent.getChildren().add(menu);
            } else {
                roots.add(menu);
            }
        }
        sortTree(roots);
        return roots;
    }

    /**
     * @brief 查询系统菜单详情。
     *
     * @param id 菜单ID。
     * @return 系统菜单详情。
     */
    @Override
    public SysMenuVO getMenuDetail(Long id) {
        return SysMenuVO.fromEntity(getExistingMenu(id));
    }

    /**
     * @brief 新增系统菜单。
     *
     * @param dto 新增系统菜单请求对象。
     * @return 新增后的系统菜单。
     */
    @Override
    public SysMenuVO createMenu(SysMenuCreateDTO dto) {
        Long parentId = normalizeParentId(dto.getParentId());
        validateParent(parentId, null);
        validatePermissionCode(dto.getPermissionCode(), null);

        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setRoutePath(dto.getRoutePath());
        menu.setComponentPath(dto.getComponentPath());
        menu.setPermissionCode(trimToNull(dto.getPermissionCode()));
        menu.setIcon(dto.getIcon());
        menu.setMenuSort(dto.getMenuSort() == null ? DEFAULT_MENU_SORT : dto.getMenuSort());
        menu.setVisible(dto.getVisible() == null ? VISIBLE_YES : dto.getVisible());
        menu.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED);
        save(menu);
        return SysMenuVO.fromEntity(menu);
    }

    /**
     * @brief 修改系统菜单。
     *
     * @details
     * 修改父级时会校验目标父级是否存在，并阻止把菜单挂到自身或自身子孙节点下面，
     * 避免形成环形菜单树。
     *
     * @param id 菜单ID。
     * @param dto 修改系统菜单请求对象。
     * @return 修改后的系统菜单。
     */
    @Override
    public SysMenuVO updateMenu(Long id, SysMenuUpdateDTO dto) {
        SysMenu menu = getExistingMenu(id);
        Long parentId = normalizeParentId(dto.getParentId());
        validateParent(parentId, id);
        validatePermissionCode(dto.getPermissionCode(), id);

        menu.setParentId(parentId);
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setRoutePath(dto.getRoutePath());
        menu.setComponentPath(dto.getComponentPath());
        menu.setPermissionCode(trimToNull(dto.getPermissionCode()));
        menu.setIcon(dto.getIcon());
        menu.setMenuSort(dto.getMenuSort() == null ? DEFAULT_MENU_SORT : dto.getMenuSort());
        menu.setVisible(dto.getVisible() == null ? VISIBLE_YES : dto.getVisible());
        if (StringUtils.hasText(dto.getStatus())) {
            validateStatus(dto.getStatus());
            menu.setStatus(dto.getStatus());
        }
        updateById(menu);
        return SysMenuVO.fromEntity(getById(id));
    }

    /**
     * @brief 删除系统菜单。
     *
     * @details
     * 仅允许删除单个叶子菜单。若菜单下存在子菜单，需要先逐个处理子菜单后再删除当前菜单。
     *
     * @param id 菜单ID。
     */
    @Override
    public void deleteMenu(Long id) {
        getExistingMenu(id);
        long childCount = count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(400, "存在子菜单，不能直接删除");
        }
        removeById(id);
    }

    /**
     * @brief 调整系统菜单排序。
     *
     * @param id 菜单ID。
     * @param dto 排序请求对象。
     * @return 调整后的系统菜单。
     */
    @Override
    public SysMenuVO sortMenu(Long id, SysMenuSortDTO dto) {
        SysMenu menu = getExistingMenu(id);
        Long parentId = normalizeParentId(dto.getParentId());
        validateParent(parentId, id);
        menu.setParentId(parentId);
        menu.setMenuSort(dto.getMenuSort() == null ? DEFAULT_MENU_SORT : dto.getMenuSort());
        updateById(menu);
        return SysMenuVO.fromEntity(getById(id));
    }

    private LambdaQueryWrapper<SysMenu> buildQueryWrapper(String keyword, String status, String menuType) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysMenu::getMenuName, keyword)
                    .or()
                    .like(SysMenu::getRoutePath, keyword)
                    .or()
                    .like(SysMenu::getPermissionCode, keyword));
        }
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            wrapper.eq(SysMenu::getStatus, status);
        }
        if (StringUtils.hasText(menuType)) {
            validateMenuType(menuType);
            wrapper.eq(SysMenu::getMenuType, menuType);
        }
        wrapper.orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getMenuSort)
                .orderByAsc(SysMenu::getId);
        return wrapper;
    }

    private SysMenu getExistingMenu(Long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return;
        }
        if (Objects.equals(parentId, currentId)) {
            throw new BusinessException(400, "父菜单不能选择自身");
        }
        SysMenu parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException(400, "父菜单不存在");
        }
        if (currentId != null && isDescendant(parentId, currentId)) {
            throw new BusinessException(400, "父菜单不能选择当前菜单的子菜单");
        }
    }

    private boolean isDescendant(Long targetId, Long ancestorId) {
        Long currentId = targetId;
        while (currentId != null && currentId != ROOT_PARENT_ID) {
            if (Objects.equals(currentId, ancestorId)) {
                return true;
            }
            SysMenu current = getById(currentId);
            if (current == null) {
                return false;
            }
            currentId = current.getParentId();
        }
        return false;
    }

    private void validatePermissionCode(String permissionCode, Long excludeId) {
        String normalizedPermissionCode = trimToNull(permissionCode);
        if (!StringUtils.hasText(normalizedPermissionCode)) {
            return;
        }
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPermissionCode, normalizedPermissionCode);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "权限标识已存在");
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "菜单状态只能为ENABLED或DISABLED");
        }
    }

    private void validateMenuType(String menuType) {
        if (!TYPE_CATALOG.equals(menuType) && !TYPE_MENU.equals(menuType) && !TYPE_BUTTON.equals(menuType)) {
            throw new BusinessException(400, "菜单类型只能为CATALOG、MENU或BUTTON");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void sortTree(List<SysMenuVO> menus) {
        menus.sort(Comparator.comparing(SysMenuVO::getMenuSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysMenuVO::getId, Comparator.nullsLast(Long::compareTo)));
        menus.forEach(menu -> sortTree(menu.getChildren()));
    }
}
