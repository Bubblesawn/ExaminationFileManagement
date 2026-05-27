package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @brief 新增考籍档案请求对象。
 *
 * @details
 * 用于接收考籍档案创建接口的核心字段，考生ID和考籍号必须明确提供，
 * 其余专业、批次和备注信息可按实际业务补充。
 */
@Data
public class StudentRecordCreateDTO {
    @NotNull(message = "不能为空")
    private Long candidateId;

    @NotBlank(message = "不能为空")
    private String recordNo;

    private String enrollBatch;
    private String educationLevel;
    private String majorCode;
    private String majorName;
    private String recordStatus;
    private String remark;
}
