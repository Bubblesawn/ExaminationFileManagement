package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 系统操作日志实体。
 *
 * @details
 * 记录内部业务接口的请求地址、操作人、执行结果和耗时等信息，用于系统审计和问题追踪。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    private Long id;
    private String moduleName;
    private String operationType;
    private String operationDesc;
    private String requestMethod;
    private String requestUri;
    private String requestParam;
    private String responseResult;
    private String operationStatus;
    private String errorMessage;
    private Long operatorId;
    private String operatorName;
    private String operationIp;
    private LocalDateTime operationTime;
    private Long costTime;
}
