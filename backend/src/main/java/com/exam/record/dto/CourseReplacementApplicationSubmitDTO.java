package com.exam.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 课程顶替申请提交请求对象。
 *
 * @details
 * 课程信息以规则 ID 为准，提交时系统会读取启用且有效的课程顶替规则，
 * 并将规则快照写入通用申请扩展字段，避免后续规则调整影响已提交申请。
 */
@Data
public class CourseReplacementApplicationSubmitDTO {
    @NotNull(message = "考籍档案ID不能为空")
    private Long recordId;

    @NotNull(message = "课程顶替规则ID不能为空")
    private Long ruleId;

    @Size(max = 512, message = "申请原因不能超过512个字符")
    private String applyReason;

    private List<Long> materialIds;

    @Size(max = 512, message = "备注不能超过512个字符")
    private String remark;
}
