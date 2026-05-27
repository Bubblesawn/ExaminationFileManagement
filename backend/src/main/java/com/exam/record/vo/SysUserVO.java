package com.exam.record.vo;

import java.time.LocalDateTime;

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
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
