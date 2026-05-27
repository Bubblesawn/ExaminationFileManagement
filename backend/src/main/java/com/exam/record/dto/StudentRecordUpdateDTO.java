package com.exam.record.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @brief 修改考籍档案请求对象。
 *
 * @details
 * 修改接口不允许变更考籍号，避免影响材料、流程和历史记录的业务关联。
 */
@Data
public class StudentRecordUpdateDTO {
    @NotNull(message = "不能为空")
    private Long candidateId;

    private String enrollBatch;
    private String educationLevel;
    private String majorCode;
    private String majorName;
    private String remark;
}
