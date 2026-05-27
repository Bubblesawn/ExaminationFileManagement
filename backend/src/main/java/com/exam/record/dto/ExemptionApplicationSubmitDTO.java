package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 免考申请提交请求对象。
 *
 * @details
 * 用于第四阶段 4.2 的免考申请提交接口，承载考籍档案、申请免考课程、证明来源课程、
 * 免考原因和申请材料 ID 列表等业务字段。
 */
@Data
public class ExemptionApplicationSubmitDTO {
    @NotNull(message = "考籍档案ID不能为空")
    private Long recordId;

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
