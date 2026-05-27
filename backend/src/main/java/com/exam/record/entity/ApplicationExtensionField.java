package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 申请业务扩展字段实体。
 *
 * @details
 * 对应 application_extension_field 表，用于保存不同业务申请的差异化字段。
 * 例如免考申请的免考课程、证明来源课程和免考原因，毕业申请的资格校验结果等。
 */
@Data
@TableName("application_extension_field")
public class ApplicationExtensionField {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 通用申请 ID，关联 business_application.id。 */
    private Long applicationId;

    /** @brief 业务类型。 */
    private String businessType;

    /** @brief 扩展字段编码。 */
    private String fieldCode;

    /** @brief 扩展字段名称。 */
    private String fieldName;

    /** @brief 扩展字段值。 */
    private String fieldValue;

    /** @brief 字段值类型，例如 STRING、NUMBER、DATE、JSON。 */
    private String valueType;

    /** @brief 是否必填，1 表示是，0 表示否。 */
    private Integer requiredFlag;

    /** @brief 排序值。 */
    private Integer sortOrder;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
