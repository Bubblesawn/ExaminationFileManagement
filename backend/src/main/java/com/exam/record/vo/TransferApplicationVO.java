package com.exam.record.vo;

import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.StudentRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @brief 考籍转入转出申请响应对象。
 *
 * @details
 * 在通用申请主表字段之外补充考籍号、考生摘要和转考扩展字段，用于转入转出申请
 * 列表、详情和审核页面展示。
 */
@Data
public class TransferApplicationVO {
    private Long id;
    private String applicationNo;
    private String businessType;
    private Long recordId;
    private String recordNo;
    private String recordStatus;
    private Long candidateId;
    private String candidateName;
    private String idCard;
    private String admissionNo;
    private String applicationTitle;
    private String applicationStatus;
    private String sourceProvince;
    private String sourceSchool;
    private String sourceRecordNo;
    private String targetProvince;
    private String targetSchool;
    private String transferReason;
    private List<Long> materialIds;
    private Long applyUserId;
    private String applyUserName;
    private LocalDateTime submitTime;
    private LocalDateTime withdrawTime;
    private String withdrawReason;
    private Long auditUserId;
    private String auditUserName;
    private LocalDateTime auditTime;
    private String auditOpinion;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @brief 组装考籍转入转出申请响应对象。
     *
     * @param application 通用业务申请实体。
     * @param record 考籍档案实体，允许为空。
     * @param candidate 考生实体，允许为空。
     * @param materialIds 申请材料 ID 列表。
     * @param extensionData 转考业务扩展字段。
     * @return 考籍转入转出申请响应对象。
     */
    public static TransferApplicationVO fromEntity(BusinessApplication application,
                                                   StudentRecord record,
                                                   Candidate candidate,
                                                   List<Long> materialIds,
                                                   Map<String, String> extensionData) {
        TransferApplicationVO vo = new TransferApplicationVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setBusinessType(application.getBusinessType());
        vo.setRecordId(application.getRecordId());
        vo.setCandidateId(application.getCandidateId());
        vo.setApplicationTitle(application.getApplicationTitle());
        vo.setApplicationStatus(application.getApplicationStatus());
        vo.setSourceProvince(extensionData.get("sourceProvince"));
        vo.setSourceSchool(extensionData.get("sourceSchool"));
        vo.setSourceRecordNo(extensionData.get("sourceRecordNo"));
        vo.setTargetProvince(extensionData.get("targetProvince"));
        vo.setTargetSchool(extensionData.get("targetSchool"));
        vo.setTransferReason(extensionData.get("transferReason"));
        vo.setMaterialIds(materialIds);
        vo.setApplyUserId(application.getApplyUserId());
        vo.setApplyUserName(application.getApplyUserName());
        vo.setSubmitTime(application.getSubmitTime());
        vo.setWithdrawTime(application.getWithdrawTime());
        vo.setWithdrawReason(application.getWithdrawReason());
        vo.setAuditUserId(application.getAuditUserId());
        vo.setAuditUserName(application.getAuditUserName());
        vo.setAuditTime(application.getAuditTime());
        vo.setAuditOpinion(application.getAuditOpinion());
        vo.setRemark(application.getRemark());
        vo.setCreateTime(application.getCreateTime());
        vo.setUpdateTime(application.getUpdateTime());
        if (record != null) {
            vo.setRecordNo(record.getRecordNo());
            vo.setRecordStatus(record.getRecordStatus());
        }
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setIdCard(candidate.getIdCard());
            vo.setAdmissionNo(candidate.getAdmissionNo());
        }
        return vo;
    }
}
