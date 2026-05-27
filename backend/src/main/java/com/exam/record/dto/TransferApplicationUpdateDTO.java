package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 考籍转入转出申请修改请求对象。
 *
 * @details
 * 仅允许修改已提交且未进入终态的转考申请，业务类型和考籍档案不允许通过修改接口变更。
 */
@Data
public class TransferApplicationUpdateDTO {
    @Size(max = 64, message = "原考籍省份不能超过64个字符")
    private String sourceProvince;

    @Size(max = 128, message = "原考籍单位不能超过128个字符")
    private String sourceSchool;

    @Size(max = 64, message = "原考籍号不能超过64个字符")
    private String sourceRecordNo;

    @Size(max = 64, message = "目标省份不能超过64个字符")
    private String targetProvince;

    @Size(max = 128, message = "目标接收单位不能超过128个字符")
    private String targetSchool;

    @NotBlank(message = "转考原因不能为空")
    @Size(max = 512, message = "转考原因不能超过512个字符")
    private String transferReason;

    private List<Long> materialIds;

    @Size(max = 512, message = "备注不能超过512个字符")
    private String remark;
}
