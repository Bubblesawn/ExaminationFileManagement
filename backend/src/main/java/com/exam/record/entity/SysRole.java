package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 系统角色实体。
 */
@Data
@TableName("sys_role")
public class SysRole {
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
