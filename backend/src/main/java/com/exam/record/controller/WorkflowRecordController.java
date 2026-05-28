package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.service.WorkflowRecordService;
import com.exam.record.vo.AuditRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @brief 通用业务流程记录接口。
 *
 * @details
 * 对应第四阶段 4.6 任务，为免考、课程顶替、考籍转入转出和毕业申请提供统一流程记录
 * 查询入口。既支持按申请查询，也支持按考籍档案聚合查询。
 */
@RestController
@RequestMapping("/api/workflow-records")
public class WorkflowRecordController {
    private final WorkflowRecordService workflowRecordService;

    /**
     * @brief 构造通用业务流程记录控制器。
     *
     * @param workflowRecordService 业务流程记录查询服务。
     */
    public WorkflowRecordController(WorkflowRecordService workflowRecordService) {
        this.workflowRecordService = workflowRecordService;
    }

    /**
     * @brief 查询业务流程记录。
     *
     * @param businessType 业务类型，可为空。
     * @param businessId 业务 ID，可为空。
     * @param applicationId 通用申请 ID，可为空。
     * @param recordId 考籍档案 ID，可为空。
     * @return 流程记录列表。
     */
    @GetMapping
    public Result<List<AuditRecordVO>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Long recordId) {
        return Result.success(workflowRecordService.listFlowRecords(businessType, businessId, applicationId, recordId));
    }
}
