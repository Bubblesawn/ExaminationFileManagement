package com.exam.record.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.exam.record.entity.SysUser;
import lombok.Data;

/**
 * @brief 系统用户响应对象。
 *
 * @details
 * 用户管理接口统一返回该对象，避免向前端暴露密码哈希等敏感字段。
 */
@Data
public class SysUserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private String status;
    private List<Long> roleIds;
    private List<String> roleNames;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @brief 将系统用户实体转换为响应对象。
     *
     * @param user 系统用户实体。
     * @return 系统用户响应对象。
     */
    public static SysUserVO fromEntity(SysUser user) {
        return fromEntity(user, List.of(), List.of());
    }

    /**
     * @brief 将系统用户实体和角色信息转换为响应对象。
     *
     * @param user 系统用户实体。
     * @param roleIds 用户绑定的角色ID列表。
     * @param roleNames 用户绑定的角色名称列表。
     * @return 系统用户响应对象。
     */
    public static SysUserVO fromEntity(SysUser user, List<Long> roleIds, List<String> roleNames) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRoleIds(roleIds);
        vo.setRoleNames(roleNames);
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
