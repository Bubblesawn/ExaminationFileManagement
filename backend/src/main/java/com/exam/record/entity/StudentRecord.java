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
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 考生 ID，关联 candidate.id。 */
    private Long candidateId;

    /** @brief 考籍号，作为考籍档案唯一业务编号。 */
    private String recordNo;

    /** @brief 注册批次。 */
    private String enrollBatch;

    /** @brief 考籍层次。 */
    private String educationLevel;

    /** @brief 专业代码。 */
    private String majorCode;

    /** @brief 专业名称。 */
    private String majorName;

    /** @brief 考籍状态，例如 NORMAL、SUSPENDED、TRANSFERRED_OUT、GRADUATED。 */
    private String recordStatus;

    /** @brief 归档状态，例如 UNARCHIVED、ARCHIVED。 */
    private String archiveStatus;

    /** @brief 归档时间。 */
    private LocalDateTime archiveTime;

    /** @brief 归档操作人 ID。 */
    private Long archiveOperatorId;

    /** @brief 备注。 */
    private String remark;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
