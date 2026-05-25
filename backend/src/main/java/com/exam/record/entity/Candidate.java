package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 考生基础信息实体。
 */
@Data
@TableName("candidate")
public class Candidate {
    private Long id;
    private String name;
    private String gender;
    private String idCard;
    private String admissionNo;
    private String phone;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

