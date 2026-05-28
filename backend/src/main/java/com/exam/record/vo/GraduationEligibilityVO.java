package com.exam.record.vo;

import lombok.Data;

import java.util.List;

/**
 * @brief 毕业资格校验结果响应对象。
 *
 * @details
 * 资格校验结果由毕业申请提交、修改和独立校验接口复用，当前覆盖档案状态、
 * 基础档案字段、毕业材料和材料审核状态等可由现有数据直接判定的条件。
 */
@Data
public class GraduationEligibilityVO {
    private Boolean eligible;
    private List<String> passedItems;
    private List<String> failedItems;
    private List<String> warningItems;
}
