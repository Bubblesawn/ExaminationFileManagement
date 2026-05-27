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
    private Long id;
    private Long recordId;
    private String beforeStatus;
    private String afterStatus;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
}
