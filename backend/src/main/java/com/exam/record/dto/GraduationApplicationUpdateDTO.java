package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 毕业申请修改请求对象。
 */
@Data
public class GraduationApplicationUpdateDTO {
    @NotBlank(message = "申请毕业批次不能为空")
    @Size(max = 64, message = "申请毕业批次不能超过64个字符")
    private String graduationBatch;

    @Size(max = 64, message = "学位申请类型不能超过64个字符")
    private String degreeApplyType;

    @NotBlank(message = "毕业申请原因不能为空")
    @Size(max = 512, message = "毕业申请原因不能超过512个字符")
    private String applyReason;

    private List<Long> materialIds;

    @Size(max = 512, message = "备注不能超过512个字符")
    private String remark;
}
