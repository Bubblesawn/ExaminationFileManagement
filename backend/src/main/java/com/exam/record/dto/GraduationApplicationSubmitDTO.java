package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 毕业申请提交请求对象。
 *
 * @details
 * 用于第四阶段 4.5 的毕业申请提交接口。申请提交时会同步执行资格校验，
 * 校验结果写入通用申请扩展字段快照，供审核和结果查询使用。
 */
@Data
public class GraduationApplicationSubmitDTO {
    @NotNull(message = "考籍档案ID不能为空")
    private Long recordId;

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
