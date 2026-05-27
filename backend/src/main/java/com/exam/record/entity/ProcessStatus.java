package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 流程状态实体。
 *
 * @details
 * 对应 process_status 表，用于定义免考、课程顶替、考籍转入转出和毕业申请的
 * 可用状态、终态标记以及允许流转的下一状态。后续审核服务应基于该表校验状态流转。
 */
@Data
@TableName("process_status")
public class ProcessStatus {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 业务类型，例如 EXEMPTION、COURSE_REPLACE、TRANSFER_IN、TRANSFER_OUT、GRADUATION。 */
    private String businessType;

    /** @brief 流程状态编码。 */
    private String statusCode;

    /** @brief 流程状态名称。 */
    private String statusName;

    /** @brief 状态排序。 */
    private Integer statusSort;

    /** @brief 是否初始状态，1 表示是，0 表示否。 */
    private Integer initialStatus;

    /** @brief 是否终态，1 表示是，0 表示否。 */
    private Integer finalStatus;

    /** @brief 当前状态是否允许编辑申请，1 表示允许，0 表示不允许。 */
    private Integer allowEdit;

    /** @brief 当前状态是否允许撤回申请，1 表示允许，0 表示不允许。 */
    private Integer allowWithdraw;

    /** @brief 允许流转的下一状态编码，多个状态使用英文逗号分隔。 */
    private String nextStatusCodes;

    /** @brief 状态说明。 */
    private String description;

    /** @brief 启用状态，例如 ENABLED、DISABLED。 */
    private String status;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
