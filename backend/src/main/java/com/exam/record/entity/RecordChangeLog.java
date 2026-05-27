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
    private Long id;
    private Long recordId;
    private String changeType;
    private String changeField;
    private String beforeValue;
    private String afterValue;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
}
