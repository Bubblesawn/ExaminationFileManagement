package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 免考申请修改请求对象。
 *
 * @details
 * 仅允许修改处于已提交状态的免考申请，避免审核完成或撤回后的业务数据被再次变更。
 */
@Data
public class ExemptionApplicationUpdateDTO {
    @NotBlank(message = "免考课程代码不能为空")
    private String courseCode;

    @NotBlank(message = "免考课程名称不能为空")
    private String courseName;

    private String sourceCourseCode;
    private String sourceCourseName;

    @NotBlank(message = "免考原因不能为空")
    @Size(max = 512, message = "免考原因不能超过512个字符")
    private String exemptionReason;

    private List<Long> materialIds;

    private String remark;
}
