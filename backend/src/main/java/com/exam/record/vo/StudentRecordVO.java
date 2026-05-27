package com.exam.record.vo;

import com.exam.record.entity.Candidate;
import com.exam.record.entity.StudentRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 考籍档案响应对象。
 *
 * @details
 * 在档案基础字段之外补充考生姓名、身份证号、准考证号等摘要信息，
 * 便于前端列表和详情页面直接展示档案与考生的对应关系。
 */
@Data
public class StudentRecordVO {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private String idCard;
    private String admissionNo;
    private String phone;
    private String recordNo;
    private String enrollBatch;
    private String educationLevel;
    private String majorCode;
    private String majorName;
    private String recordStatus;
    private String archiveStatus;
    private LocalDateTime archiveTime;
    private Long archiveOperatorId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @brief 将考籍档案实体转换为响应对象。
     *
     * @param record 考籍档案实体。
     * @param candidate 档案所属考生，允许为空。
     * @return 考籍档案响应对象。
     */
    public static StudentRecordVO fromEntity(StudentRecord record, Candidate candidate) {
        StudentRecordVO vo = new StudentRecordVO();
        vo.setId(record.getId());
        vo.setCandidateId(record.getCandidateId());
        vo.setRecordNo(record.getRecordNo());
        vo.setEnrollBatch(record.getEnrollBatch());
        vo.setEducationLevel(record.getEducationLevel());
        vo.setMajorCode(record.getMajorCode());
        vo.setMajorName(record.getMajorName());
        vo.setRecordStatus(record.getRecordStatus());
        vo.setArchiveStatus(record.getArchiveStatus());
        vo.setArchiveTime(record.getArchiveTime());
        vo.setArchiveOperatorId(record.getArchiveOperatorId());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        if (candidate != null) {
            vo.setCandidateName(candidate.getName());
            vo.setIdCard(candidate.getIdCard());
            vo.setAdmissionNo(candidate.getAdmissionNo());
            vo.setPhone(candidate.getPhone());
        }
        return vo;
    }
}
