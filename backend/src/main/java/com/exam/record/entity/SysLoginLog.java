package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 系统登录日志实体。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {
    private Long id;
    private String username;
    private Long userId;
    private String loginStatus;
    private String failureReason;
    private String loginIp;
    private String userAgent;
    private LocalDateTime loginTime;
}
