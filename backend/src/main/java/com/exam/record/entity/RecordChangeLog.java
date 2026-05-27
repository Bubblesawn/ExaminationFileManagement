package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 档案变更记录实体。
 *
 * @details
 * 对应 record_change_log 表，用于记录考籍档案关键字段或关联业务数据的变更内容。
 * 该实体为后续自动生成变更记录、详情追溯和审计查询提供统一数据模型。
 */
@Data
@TableName("record_change_log")
public class RecordChangeLog {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 考籍档案 ID，关联 student_record.id。 */
    private Long recordId;

    /** @brief 变更类型，例如 CREATE、UPDATE、ARCHIVE、STATUS_CHANGE、MATERIAL_CHANGE。 */
    private String changeType;

    /** @brief 变更字段。 */
    private String changeField;

    /** @brief 变更前内容。 */
    private String beforeValue;

    /** @brief 变更后内容。 */
    private String afterValue;

    /** @brief 变更原因。 */
    private String changeReason;

    /** @brief 操作人 ID。 */
    private Long operatorId;

    /** @brief 操作人姓名。 */
    private String operatorName;

    /** @brief 操作时间。 */
    private LocalDateTime operationTime;
}
