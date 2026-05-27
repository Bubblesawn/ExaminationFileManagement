package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 审核记录实体。
 *
 * @details
 * 对应 audit_record 表，用于记录业务申请提交、撤回、审核通过和审核驳回等流程节点，
 * 为第四阶段流程记录查询提供统一数据来源。该表既可记录通用申请的流程动作，也可
 * 兼容材料审核等已有业务的审计记录。
 */
@Data
@TableName("audit_record")
public class AuditRecord {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 通用申请 ID，关联 business_application.id。 */
    private Long applicationId;

    /** @brief 业务类型。 */
    private String businessType;

    /** @brief 业务 ID。 */
    private Long businessId;

    /** @brief 考籍档案 ID。 */
    private Long recordId;

    /** @brief 流程动作，例如 SUBMIT、APPROVE、REJECT、WITHDRAW。 */
    private String auditAction;

    /** @brief 操作前申请状态。 */
    private String beforeStatus;

    /** @brief 操作后申请状态。 */
    private String afterStatus;

    /** @brief 审核或流程状态。 */
    private String auditStatus;

    /** @brief 审核意见或流程说明。 */
    private String auditOpinion;

    /** @brief 审核人或操作人 ID。 */
    private Long auditorId;

    /** @brief 审核人或操作人姓名。 */
    private String auditorName;

    /** @brief 操作时间。 */
    private LocalDateTime operationTime;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;
}
