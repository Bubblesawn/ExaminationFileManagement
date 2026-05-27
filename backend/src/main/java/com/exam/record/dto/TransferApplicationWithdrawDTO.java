package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 考籍转入转出申请撤回请求对象。
 */
@Data
public class TransferApplicationWithdrawDTO {
    @NotBlank(message = "撤回原因不能为空")
    @Size(max = 512, message = "撤回原因不能超过512个字符")
    private String withdrawReason;
}
