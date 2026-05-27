package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * @brief 修改考生请求对象。
 *
 * @details
 * 用于承载考生基础信息修改接口的请求参数，更新时要求保留考生姓名和身份证号。
 */
@Data
public class CandidateUpdateDTO {
    @NotBlank(message = "不能为空")
    private String name;

    private String gender;

    @NotBlank(message = "不能为空")
    private String idCard;

    private String admissionNo;
    private LocalDate birthDate;
    private String nation;
    private String politicalStatus;
    private String educationLevel;
    private String majorName;
    private String phone;
    private String email;
    private String address;
    private String status;
}

