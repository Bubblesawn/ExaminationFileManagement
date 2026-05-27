package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 课程顶替规则状态修改请求对象。
 */
@Data
public class CourseReplacementRuleStatusDTO {
    @NotBlank(message = "规则状态不能为空")
    private String ruleStatus;
}
