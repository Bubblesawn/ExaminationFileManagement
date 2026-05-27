package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 用户角色关联实体。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createTime;
}
