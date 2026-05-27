package com.exam.record.vo;

import com.exam.record.entity.SysRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @brief 系统角色响应对象。
 */
@Data
public class SysRoleVO {
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;
    private List<Long> menuIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @brief 将系统角色实体转换为响应对象。
     *
     * @param role 系统角色实体。
     * @param menuIds 角色已分配菜单ID列表。
     * @return 系统角色响应对象。
     */
    public static SysRoleVO fromEntity(SysRole role, List<Long> menuIds) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleSort(role.getRoleSort());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setMenuIds(menuIds);
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
        return vo;
    }
}
