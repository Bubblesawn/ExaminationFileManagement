package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 考籍档案实体。
 *
 * @details
 * 对应 student_record 表，用于记录考生取得考籍后的档案编号、专业批次、
 * 当前考籍状态和归档状态。后续免考、转考、毕业等业务均围绕该档案流转。
 */
@Data
@TableName("student_record")
public class StudentRecord {
    private Long id;
    private Long candidateId;
    private String recordNo;
    private String enrollBatch;
    private String educationLevel;
    private String majorCode;
    private String majorName;
    private String recordStatus;
    private String archiveStatus;
    private LocalDateTime archiveTime;
    private Long archiveOperatorId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
