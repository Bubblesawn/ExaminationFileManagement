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
    private Long id;
    private String name;
    private String gender;
    private String idCard;
    private String admissionNo;
    private LocalDate birthDate;
    private String nation;
    private String politicalStatus;
    private String educationLevel;
    private String majorName;
    private String phone;
    private String email;
    private String address;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
