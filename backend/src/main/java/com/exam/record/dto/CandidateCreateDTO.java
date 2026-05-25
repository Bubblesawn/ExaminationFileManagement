package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 新增考生请求对象。
 */
@Data
public class CandidateCreateDTO {
    @NotBlank(message = "不能为空")
    private String name;

    private String gender;

    @NotBlank(message = "不能为空")
    private String idCard;

    private String admissionNo;
    private String phone;
}

