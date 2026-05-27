package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 档案状态记录实体。
 *
 * @details
 * 对应 record_status_log 表，用于记录考籍档案状态每一次流转的前后状态、
 * 变更原因和操作人信息，便于档案状态维护和流程追溯。
 */
@Data
@TableName("record_status_log")
public class RecordStatusLog {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 考籍档案 ID，关联 student_record.id。 */
    private Long recordId;

    /** @brief 变更前状态，新建档案时可为空。 */
    private String beforeStatus;

    /** @brief 变更后状态。 */
    private String afterStatus;

    /** @brief 状态变更原因。 */
    private String changeReason;

    /** @brief 操作人 ID。 */
    private Long operatorId;

    /** @brief 操作人姓名。 */
    private String operatorName;

    /** @brief 操作时间。 */
    private LocalDateTime operationTime;
}
