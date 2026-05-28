package com.exam.record.service;

/**
 * @brief 档案状态联动服务接口。
 *
 * @details
 * 业务申请审核结果需要影响考籍档案状态时，通过该服务统一写入 student_record、
 * record_status_log 和 record_change_log，避免各流程自行拼装状态记录导致口径不一致。
 */
public interface RecordStatusLinkageService {

    /**
     * @brief 根据业务流程结果联动考籍档案状态。
     *
     * @param recordId 考籍档案 ID。
     * @param targetStatus 目标考籍状态。
     * @param changeReason 状态变更原因。
     * @param businessType 触发联动的业务类型。
     * @param applicationId 触发联动的申请 ID。
     */
    void linkRecordStatus(Long recordId,
                          String targetStatus,
                          String changeReason,
                          String businessType,
                          Long applicationId);
}
