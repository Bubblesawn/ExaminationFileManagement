package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.entity.SysLoginLog;
import com.exam.record.entity.SysOperationLog;
import com.exam.record.service.SysLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @brief 系统日志管理接口。
 */
@RestController
@RequestMapping("/api/system/logs")
public class SysLogController {
    private final SysLogService sysLogService;

    /**
     * @brief 构造系统日志管理控制器。
     *
     * @param sysLogService 系统日志管理业务服务。
     */
    public SysLogController(SysLogService sysLogService) {
        this.sysLogService = sysLogService;
    }

    /**
     * @brief 分页查询登录日志。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param username 登录账号关键字。
     * @param loginStatus 登录状态。
     * @return 登录日志分页数据。
     */
    @GetMapping("/login/page")
    public Result<Page<SysLoginLog>> pageLoginLogs(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String loginStatus) {
        return Result.success(sysLogService.pageLoginLogs(pageNo, pageSize, username, loginStatus));
    }

    /**
     * @brief 查询登录日志详情。
     *
     * @param id 登录日志ID。
     * @return 登录日志详情。
     */
    @GetMapping("/login/{id}")
    public Result<SysLoginLog> loginLogDetail(@PathVariable Long id) {
        return Result.success(sysLogService.getLoginLogDetail(id));
    }

    /**
     * @brief 分页查询操作日志。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param moduleName 模块名称。
     * @param operatorName 操作人姓名或账号关键字。
     * @param operationStatus 操作状态。
     * @return 操作日志分页数据。
     */
    @GetMapping("/operation/page")
    public Result<Page<SysOperationLog>> pageOperationLogs(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String operationStatus) {
        return Result.success(sysLogService.pageOperationLogs(pageNo, pageSize, moduleName, operatorName, operationStatus));
    }

    /**
     * @brief 查询操作日志详情。
     *
     * @param id 操作日志ID。
     * @return 操作日志详情。
     */
    @GetMapping("/operation/{id}")
    public Result<SysOperationLog> operationLogDetail(@PathVariable Long id) {
        return Result.success(sysLogService.getOperationLogDetail(id));
    }
}
