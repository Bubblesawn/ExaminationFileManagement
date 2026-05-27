package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 通用业务申请实体。
 *
 * @details
 * 对应 business_application 表，用于承载免考、课程顶替、转入转出和毕业申请等
 * 第四阶段业务流程的主申请数据。业务差异字段通过 extensionDataJson 快照和
 * application_extension_field 明细表保存，避免通用申请表随业务类型持续膨胀。
 */
@Data
@TableName("business_application")
public class BusinessApplication {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 申请编号。 */
    private String applicationNo;

    /** @brief 业务类型，例如 EXEMPTION 表示免考申请。 */
    private String businessType;

    /** @brief 考籍档案 ID。 */
    private Long recordId;

    /** @brief 考生 ID。 */
    private Long candidateId;

    /** @brief 申请标题，用于列表和流程记录快速识别申请事项。 */
    private String applicationTitle;

    /** @brief 申请状态，例如 DRAFT、SUBMITTED、AUDITING、APPROVED、REJECTED、WITHDRAWN。 */
    private String applicationStatus;

    /** @brief 当前流程节点编码。 */
    private String currentNodeCode;

    /** @brief 当前流程节点名称。 */
    private String currentNodeName;

    /** @brief 免考或课程顶替的目标课程代码。 */
    private String courseCode;

    /** @brief 免考或课程顶替的目标课程名称。 */
    private String courseName;

    /** @brief 免考或课程顶替的来源课程代码。 */
    private String sourceCourseCode;

    /** @brief 免考或课程顶替的来源课程名称。 */
    private String sourceCourseName;

    /** @brief 免考申请原因。 */
    private String exemptionReason;

    /** @brief 申请材料 ID 列表 JSON。 */
    private String materialIdsJson;

    /** @brief 业务扩展字段快照 JSON。 */
    private String extensionDataJson;

    /** @brief 提交人 ID。 */
    private Long applyUserId;

    /** @brief 提交人姓名。 */
    private String applyUserName;

    /** @brief 提交时间。 */
    private LocalDateTime submitTime;

    /** @brief 撤回时间。 */
    private LocalDateTime withdrawTime;

    /** @brief 撤回原因。 */
    private String withdrawReason;

    /** @brief 审核人 ID。 */
    private Long auditUserId;

    /** @brief 审核人姓名。 */
    private String auditUserName;

    /** @brief 审核时间。 */
    private LocalDateTime auditTime;

    /** @brief 审核意见。 */
    private String auditOpinion;

    /** @brief 备注。 */
    private String remark;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
