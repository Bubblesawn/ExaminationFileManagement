package com.exam.record.vo;

import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.StudentRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @brief 毕业申请响应对象。
 *
 * @details
 * 在通用申请主表字段之外补充考籍号、考生摘要、毕业批次、学位申请类型和
 * 资格校验结果，用于毕业申请列表、详情、审核和结果查询。
 */
@Data
public class GraduationApplicationVO {
    private Long id;
    private String applicationNo;
    private String businessType;
    private Long recordId;
    private String recordNo;
    private String recordStatus;
    private String archiveStatus;
    private String educationLevel;
    private String majorCode;
    private String majorName;
    private Long candidateId;
    private String candidateName;
    private String idCard;
    private String admissionNo;
    private String phone;
    private String applicationTitle;
    private String applicationStatus;
    private String graduationBatch;
    private String degreeApplyType;
    private String applyReason;
    private Boolean eligibilityPassed;
    private String eligibilitySummary;
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
     * @brief 组装毕业申请响应对象。
     *
     * @param application 通用业务申请实体。
     * @param record 考籍档案实体，允许为空。
     * @param candidate 考生实体，允许为空。
     * @param materialIds 申请材料 ID 列表。
     * @param extensionData 毕业业务扩展字段。
     * @return 毕业申请响应对象。
     */
    public static GraduationApplicationVO fromEntity(BusinessApplication application,
                                                     StudentRecord record,
                                                     Candidate candidate,
                                                     List<Long> materialIds,
                                                     Map<String, String> extensionData) {
        GraduationApplicationVO vo = new GraduationApplicationVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setBusinessType(application.getBusinessType());
        vo.setRecordId(application.getRecordId());
        vo.setCandidateId(application.getCandidateId());
        vo.setApplicationTitle(application.getApplicationTitle());
        vo.setApplicationStatus(application.getApplicationStatus());
        vo.setGraduationBatch(extensionData.get("graduationBatch"));
        vo.setDegreeApplyType(extensionData.get("degreeApplyType"));
        vo.setApplyReason(extensionData.get("applyReason"));
        vo.setEligibilityPassed(Boolean.parseBoolean(extensionData.getOrDefault("eligibilityPassed", "false")));
        vo.setEligibilitySummary(extensionData.get("eligibilitySummary"));
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
            vo.setArchiveStatus(record.getArchiveStatus());
            vo.setEducationLevel(record.getEducationLevel());
            vo.setMajorCode(record.getMajorCode());
            vo.setMajorName(record.getMajorName());
        }
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setIdCard(candidate.getIdCard());
            vo.setAdmissionNo(candidate.getAdmissionNo());
            vo.setPhone(candidate.getPhone());
        }
        return vo;
    }
}
