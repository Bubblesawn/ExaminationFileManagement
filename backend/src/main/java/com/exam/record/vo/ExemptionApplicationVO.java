package com.exam.record.vo;

import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.StudentRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @brief 免考申请响应对象。
 *
 * @details
 * 在免考申请主表字段之外补充考籍号、考生姓名、身份证号等摘要字段，
 * 用于免考申请列表、详情和审核页面展示。
 */
@Data
public class ExemptionApplicationVO {
    private Long id;
    private String applicationNo;
    private String businessType;
    private Long recordId;
    private String recordNo;
    private Long candidateId;
    private String candidateName;
    private String idCard;
    private String admissionNo;
    private String applicationTitle;
    private String applicationStatus;
    private String currentNodeCode;
    private String currentNodeName;
    private String courseCode;
    private String courseName;
    private String sourceCourseCode;
    private String sourceCourseName;
    private String exemptionReason;
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
     * @brief 组装免考申请响应对象。
     *
     * @param application 业务申请实体。
     * @param record 考籍档案实体，允许为空。
     * @param candidate 考生实体，允许为空。
     * @param materialIds 申请材料 ID 列表。
     * @param extensionFields 免考扩展字段。
     * @return 免考申请响应对象。
     */
    public static ExemptionApplicationVO fromEntity(BusinessApplication application,
                                                    StudentRecord record,
                                                    Candidate candidate,
                                                    List<Long> materialIds,
                                                    Map<String, String> extensionFields) {
        ExemptionApplicationVO vo = new ExemptionApplicationVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setBusinessType(application.getBusinessType());
        vo.setRecordId(application.getRecordId());
        vo.setCandidateId(application.getCandidateId());
        vo.setApplicationTitle(application.getApplicationTitle());
        vo.setApplicationStatus(application.getApplicationStatus());
        vo.setCurrentNodeCode(application.getCurrentNodeCode());
        vo.setCurrentNodeName(application.getCurrentNodeName());
        vo.setCourseCode(extensionFields.get("courseCode"));
        vo.setCourseName(extensionFields.get("courseName"));
        vo.setSourceCourseCode(extensionFields.get("sourceCourseCode"));
        vo.setSourceCourseName(extensionFields.get("sourceCourseName"));
        vo.setExemptionReason(extensionFields.get("exemptionReason"));
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
        }
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setIdCard(candidate.getIdCard());
            vo.setAdmissionNo(candidate.getAdmissionNo());
        }
        return vo;
    }
}
