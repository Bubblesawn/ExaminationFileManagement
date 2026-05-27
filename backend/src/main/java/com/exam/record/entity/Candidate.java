package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @brief 考生基础信息实体。
 *
 * @details
 * 对应 candidate 表，用于保存自学考试考生的身份信息、报考信息和联系信息。
 * 该实体是考籍档案、材料归集和后续业务流程的基础数据来源。
 */
@Data
@TableName("candidate")
public class Candidate {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 考生姓名。 */
    private String name;

    /** @brief 性别。 */
    private String gender;

    /** @brief 身份证号，作为考生身份唯一识别字段。 */
    private String idCard;

    /** @brief 准考证号，用于考试报名和成绩关联。 */
    private String admissionNo;

    /** @brief 出生日期。 */
    private LocalDate birthDate;

    /** @brief 民族。 */
    private String nation;

    /** @brief 政治面貌。 */
    private String politicalStatus;

    /** @brief 学历层次。 */
    private String educationLevel;

    /** @brief 报考专业名称。 */
    private String majorName;

    /** @brief 联系电话。 */
    private String phone;

    /** @brief 电子邮箱。 */
    private String email;

    /** @brief 联系地址。 */
    private String address;

    /** @brief 考生状态，例如 NORMAL、LOCKED、DISABLED。 */
    private String status;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
