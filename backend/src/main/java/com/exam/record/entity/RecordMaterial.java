package com.exam.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 档案材料实体。
 *
 * @details
 * 对应 record_material 表，用于保存考籍档案关联材料的文件信息、预览地址、
 * 上传人和审核结论。材料上传、预览、下载和智能预处理接口均基于该实体扩展。
 */
@Data
@TableName("record_material")
public class RecordMaterial {
    /** @brief 主键 ID。 */
    private Long id;

    /** @brief 考籍档案 ID，关联 student_record.id。 */
    private Long recordId;

    /** @brief 材料类型，例如 ID_CARD、COURSE_EXEMPTION、TRANSFER、GRADUATION。 */
    private String materialType;

    /** @brief 系统保存文件名。 */
    private String fileName;

    /** @brief 原始文件名。 */
    private String originalFileName;

    /** @brief 文件访问地址。 */
    private String fileUrl;

    /** @brief 文件大小，单位字节。 */
    private Long fileSize;

    /** @brief 文件后缀。 */
    private String fileSuffix;

    /** @brief 文件 MIME 类型。 */
    private String mimeType;

    /** @brief 预览地址。 */
    private String previewUrl;

    /** @brief 上传人 ID。 */
    private Long uploadUserId;

    /** @brief 审核状态，例如 PENDING、APPROVED、REJECTED。 */
    private String auditStatus;

    /** @brief 审核意见。 */
    private String auditOpinion;

    /** @brief 审核人 ID。 */
    private Long auditUserId;

    /** @brief 审核时间。 */
    private LocalDateTime auditTime;

    /** @brief 创建时间。 */
    private LocalDateTime createTime;

    /** @brief 更新时间。 */
    private LocalDateTime updateTime;
}
