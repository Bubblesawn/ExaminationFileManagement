package com.exam.record.service;

import com.exam.record.vo.AuditRecordVO;

import java.util.List;

/**
 * @brief 业务流程记录查询服务接口。
 *
 * @details
 * 面向第四阶段 4.6 的统一流程记录查询能力，屏蔽免考、课程顶替、转考和毕业等
 * 业务模块各自的存储细节，统一从 audit_record 表返回流程轨迹。
 */
public interface WorkflowRecordService {

    /**
     * @brief 查询业务流程记录。
     *
     * @param businessType 业务类型，可为空。
     * @param businessId 业务 ID，可为空。
     * @param applicationId 通用申请 ID，可为空。
     * @param recordId 考籍档案 ID，可为空。
     * @return 按操作时间升序排列的流程记录列表。
     */
    List<AuditRecordVO> listFlowRecords(String businessType, Long businessId, Long applicationId, Long recordId);
}
