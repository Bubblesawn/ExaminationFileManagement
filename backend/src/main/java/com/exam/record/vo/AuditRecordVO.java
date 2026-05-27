package com.exam.record.vo;

import com.exam.record.entity.AuditRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 审核记录响应对象。
 */
@Data
public class AuditRecordVO {
    private Long id;
    private Long applicationId;
    private String businessType;
    private Long businessId;
    private Long recordId;
    private String auditAction;
    private String beforeStatus;
    private String afterStatus;
    private String auditStatus;
    private String auditOpinion;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime operationTime;
    private LocalDateTime createTime;

    /**
     * @brief 将审核记录实体转换为响应对象。
     *
     * @param auditRecord 审核记录实体。
     * @return 审核记录响应对象。
     */
    public static AuditRecordVO fromEntity(AuditRecord auditRecord) {
        AuditRecordVO vo = new AuditRecordVO();
        vo.setId(auditRecord.getId());
        vo.setApplicationId(auditRecord.getApplicationId());
        vo.setBusinessType(auditRecord.getBusinessType());
        vo.setBusinessId(auditRecord.getBusinessId());
        vo.setRecordId(auditRecord.getRecordId());
        vo.setAuditAction(auditRecord.getAuditAction());
        vo.setBeforeStatus(auditRecord.getBeforeStatus());
        vo.setAfterStatus(auditRecord.getAfterStatus());
        vo.setAuditStatus(auditRecord.getAuditStatus());
        vo.setAuditOpinion(auditRecord.getAuditOpinion());
        vo.setAuditorId(auditRecord.getAuditorId());
        vo.setAuditorName(auditRecord.getAuditorName());
        vo.setOperationTime(auditRecord.getOperationTime());
        vo.setCreateTime(auditRecord.getCreateTime());
        return vo;
    }
}
