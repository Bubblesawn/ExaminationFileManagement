package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.BusinessException;
import com.exam.record.entity.SysLoginLog;
import com.exam.record.entity.SysOperationLog;
import com.exam.record.mapper.SysLoginLogMapper;
import com.exam.record.mapper.SysOperationLogMapper;
import com.exam.record.service.SysLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @brief 系统日志管理业务实现。
 */
@Service
public class SysLogServiceImpl implements SysLogService {
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";

    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysOperationLogMapper sysOperationLogMapper;

    /**
     * @brief 构造系统日志管理业务实现。
     *
     * @param sysLoginLogMapper 登录日志 Mapper。
     * @param sysOperationLogMapper 操作日志 Mapper。
     */
    public SysLogServiceImpl(SysLoginLogMapper sysLoginLogMapper, SysOperationLogMapper sysOperationLogMapper) {
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.sysOperationLogMapper = sysOperationLogMapper;
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
    @Override
    public Page<SysLoginLog> pageLoginLogs(long pageNo, long pageSize, String username, String loginStatus) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (StringUtils.hasText(loginStatus)) {
            validateStatus(loginStatus);
            wrapper.eq(SysLoginLog::getLoginStatus, loginStatus);
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        return sysLoginLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    /**
     * @brief 查询登录日志详情。
     *
     * @param id 登录日志ID。
     * @return 登录日志详情。
     */
    @Override
    public SysLoginLog getLoginLogDetail(Long id) {
        SysLoginLog loginLog = sysLoginLogMapper.selectById(id);
        if (loginLog == null) {
            throw new BusinessException(404, "登录日志不存在");
        }
        return loginLog;
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
    @Override
    public Page<SysOperationLog> pageOperationLogs(long pageNo,
                                                   long pageSize,
                                                   String moduleName,
                                                   String operatorName,
                                                   String operationStatus) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(moduleName)) {
            wrapper.like(SysOperationLog::getModuleName, moduleName);
        }
        if (StringUtils.hasText(operatorName)) {
            wrapper.like(SysOperationLog::getOperatorName, operatorName);
        }
        if (StringUtils.hasText(operationStatus)) {
            validateStatus(operationStatus);
            wrapper.eq(SysOperationLog::getOperationStatus, operationStatus);
        }
        wrapper.orderByDesc(SysOperationLog::getOperationTime);
        return sysOperationLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    /**
     * @brief 查询操作日志详情。
     *
     * @param id 操作日志ID。
     * @return 操作日志详情。
     */
    @Override
    public SysOperationLog getOperationLogDetail(Long id) {
        SysOperationLog operationLog = sysOperationLogMapper.selectById(id);
        if (operationLog == null) {
            throw new BusinessException(404, "操作日志不存在");
        }
        return operationLog;
    }

    /**
     * @brief 保存操作日志。
     *
     * @details
     * 操作日志写入失败不应阻断主业务流程，调用方需要在请求完成阶段尽量记录审计信息。
     *
     * @param operationLog 操作日志实体。
     */
    @Override
    public void recordOperationLog(SysOperationLog operationLog) {
        sysOperationLogMapper.insert(operationLog);
    }

    private void validateStatus(String status) {
        if (!STATUS_SUCCESS.equals(status) && !STATUS_FAIL.equals(status)) {
            throw new BusinessException(400, "日志状态只能为SUCCESS或FAIL");
        }
    }
}
