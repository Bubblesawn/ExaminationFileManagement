package com.exam.record.vo;

import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.StudentRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @brief 课程顶替申请响应对象。
 */
@Data
public class CourseReplacementApplicationVO {
    private Long id;
    private String applicationNo;
    private String businessType;
    private Long recordId;
    private String recordNo;
    private Long candidateId;
    private String candidateName;
    private String idCard;
    private String admissionNo;
    private String applicationStatus;
    private Long ruleId;
    private String sourceCourseCode;
    private String sourceCourseName;
    private String targetCourseCode;
    private String targetCourseName;
    private String majorCode;
    private String educationLevel;
    private String applyReason;
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
     * @brief 组装课程顶替申请响应对象。
     *
     * @param application 通用业务申请实体。
     * @param record 考籍档案实体，允许为空。
     * @param candidate 考生实体，允许为空。
     * @param extensionData 课程顶替扩展字段快照。
     * @param materialIds 申请材料 ID 列表。
     * @return 课程顶替申请响应对象。
     */
    public static CourseReplacementApplicationVO fromEntity(BusinessApplication application,
                                                            StudentRecord record,
                                                            Candidate candidate,
                                                            Map<String, String> extensionData,
                                                            List<Long> materialIds) {
        CourseReplacementApplicationVO vo = new CourseReplacementApplicationVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setBusinessType(application.getBusinessType());
        vo.setRecordId(application.getRecordId());
        vo.setCandidateId(application.getCandidateId());
        vo.setApplicationStatus(application.getApplicationStatus());
        vo.setRuleId(parseLong(extensionData.get("ruleId")));
        vo.setSourceCourseCode(extensionData.get("sourceCourseCode"));
        vo.setSourceCourseName(extensionData.get("sourceCourseName"));
        vo.setTargetCourseCode(extensionData.get("targetCourseCode"));
        vo.setTargetCourseName(extensionData.get("targetCourseName"));
        vo.setMajorCode(extensionData.get("majorCode"));
        vo.setEducationLevel(extensionData.get("educationLevel"));
        vo.setApplyReason(extensionData.get("applyReason"));
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

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
