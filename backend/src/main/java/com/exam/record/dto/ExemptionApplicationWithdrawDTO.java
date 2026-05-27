package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 免考申请撤回请求对象。
 */
@Data
public class ExemptionApplicationWithdrawDTO {
    @NotBlank(message = "撤回原因不能为空")
    @Size(max = 512, message = "撤回原因不能超过512个字符")
    private String withdrawReason;
}
