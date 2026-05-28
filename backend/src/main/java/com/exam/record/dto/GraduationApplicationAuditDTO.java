package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 毕业申请审核请求对象。
 *
 * @details
 * 审核通过和驳回接口共用该对象，审核意见会写入通用申请表和流程记录表。
 */
@Data
public class GraduationApplicationAuditDTO {
    @NotBlank(message = "审核意见不能为空")
    @Size(max = 512, message = "审核意见不能超过512个字符")
    private String auditOpinion;
}
