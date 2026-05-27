package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.entity.SysLoginLog;
import com.exam.record.entity.SysOperationLog;

/**
 * @brief 系统日志管理业务接口。
 */
public interface SysLogService {

    /**
     * @brief 分页查询登录日志。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param username 登录账号关键字。
     * @param loginStatus 登录状态。
     * @return 登录日志分页数据。
     */
    Page<SysLoginLog> pageLoginLogs(long pageNo, long pageSize, String username, String loginStatus);

    /**
     * @brief 查询登录日志详情。
     *
     * @param id 登录日志ID。
     * @return 登录日志详情。
     */
    SysLoginLog getLoginLogDetail(Long id);

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
    Page<SysOperationLog> pageOperationLogs(long pageNo,
                                            long pageSize,
                                            String moduleName,
                                            String operatorName,
                                            String operationStatus);

    /**
     * @brief 查询操作日志详情。
     *
     * @param id 操作日志ID。
     * @return 操作日志详情。
     */
    SysOperationLog getOperationLogDetail(Long id);

    /**
     * @brief 保存操作日志。
     *
     * @param operationLog 操作日志实体。
     */
    void recordOperationLog(SysOperationLog operationLog);
}
