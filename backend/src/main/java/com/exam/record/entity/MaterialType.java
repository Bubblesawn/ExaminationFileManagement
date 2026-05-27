package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 材料类型实体。
 *
 * @details
 * 对应 material_type 表，用于维护材料上传时可选择的业务材料分类，
 * 为前端下拉选项、材料校验和后续智能分类结果映射提供统一数据来源。
 */
@Data
@TableName("material_type")
public class MaterialType {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 材料类型编码，例如 ID_CARD、DIPLOMA。 */
    private String typeCode;

    /** @brief 材料类型名称。 */
    private String typeName;

    /** @brief 类型说明。 */
    private String description;

    /** @brief 排序值，数值越小越靠前。 */
    private Integer sortOrder;

    /** @brief 状态：ENABLED 启用，DISABLED 禁用。 */
    private String status;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
