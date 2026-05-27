package com.exam.record.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @brief 课程顶替规则新增请求对象。
 */
@Data
public class CourseReplacementRuleCreateDTO {
    @NotBlank(message = "原课程代码不能为空")
    private String sourceCourseCode;

    @NotBlank(message = "原课程名称不能为空")
    private String sourceCourseName;

    @NotBlank(message = "顶替课程代码不能为空")
    private String targetCourseCode;

    @NotBlank(message = "顶替课程名称不能为空")
    private String targetCourseName;

    private String majorCode;
    private String educationLevel;

    @DecimalMin(value = "0.0", inclusive = false, message = "学分必须大于0")
    private BigDecimal credit;

    private LocalDate effectiveDate;
    private LocalDate expireDate;

    @Size(max = 512, message = "备注不能超过512个字符")
    private String remark;
}
