package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @brief 课程顶替规则实体。
 *
 * @details
 * 对应 course_replacement_rule 表，用于维护可被申请引用的课程顶替关系。
 * 规则以“原课程 -> 顶替课程”为核心，并可按专业、层次、生效日期和状态进行约束。
 */
@Data
@TableName("course_replacement_rule")
public class CourseReplacementRule {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 原课程代码。 */
    private String sourceCourseCode;

    /** @brief 原课程名称。 */
    private String sourceCourseName;

    /** @brief 顶替课程代码。 */
    private String targetCourseCode;

    /** @brief 顶替课程名称。 */
    private String targetCourseName;

    /** @brief 适用专业代码，空值表示通用规则。 */
    private String majorCode;

    /** @brief 适用学历层次，空值表示通用规则。 */
    private String educationLevel;

    /** @brief 顶替课程学分。 */
    private BigDecimal credit;

    /** @brief 规则状态：ENABLED 启用，DISABLED 禁用。 */
    private String ruleStatus;

    /** @brief 生效日期。 */
    private LocalDate effectiveDate;

    /** @brief 失效日期。 */
    private LocalDate expireDate;

    /** @brief 备注。 */
    private String remark;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
