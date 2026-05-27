package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @brief 考籍转入转出申请提交请求对象。
 *
 * @details
 * 用于第四阶段 4.4 的考籍转入、考籍转出申请提交接口。transferType 仅允许
 * TRANSFER_IN 或 TRANSFER_OUT，转入时重点填写原考籍地信息，转出时重点填写
 * 目标接收地信息。
 */
@Data
public class TransferApplicationSubmitDTO {
    @NotNull(message = "考籍档案ID不能为空")
    private Long recordId;

    @NotBlank(message = "转考类型不能为空")
    private String transferType;

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
