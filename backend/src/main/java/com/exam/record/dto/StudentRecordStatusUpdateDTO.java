package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 考籍档案状态更新请求对象。
 */
@Data
public class StudentRecordStatusUpdateDTO {
    @NotBlank(message = "不能为空")
    private String recordStatus;

    private String changeReason;
}
