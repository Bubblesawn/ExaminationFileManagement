package com.exam.record.vo;

import com.exam.record.entity.CourseReplacementRule;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @brief 课程顶替规则响应对象。
 */
@Data
public class CourseReplacementRuleVO {
    private Long id;
    private String sourceCourseCode;
    private String sourceCourseName;
    private String targetCourseCode;
    private String targetCourseName;
    private String majorCode;
    private String educationLevel;
    private BigDecimal credit;
    private String ruleStatus;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * @brief 将规则实体转换为响应对象。
     *
     * @param rule 课程顶替规则实体。
     * @return 课程顶替规则响应对象。
     */
    public static CourseReplacementRuleVO fromEntity(CourseReplacementRule rule) {
        CourseReplacementRuleVO vo = new CourseReplacementRuleVO();
        vo.setId(rule.getId());
        vo.setSourceCourseCode(rule.getSourceCourseCode());
        vo.setSourceCourseName(rule.getSourceCourseName());
        vo.setTargetCourseCode(rule.getTargetCourseCode());
        vo.setTargetCourseName(rule.getTargetCourseName());
        vo.setMajorCode(rule.getMajorCode());
        vo.setEducationLevel(rule.getEducationLevel());
        vo.setCredit(rule.getCredit());
        vo.setRuleStatus(rule.getRuleStatus());
        vo.setEffectiveDate(rule.getEffectiveDate());
        vo.setExpireDate(rule.getExpireDate());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }
}
