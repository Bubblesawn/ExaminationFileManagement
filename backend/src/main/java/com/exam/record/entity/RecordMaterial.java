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
    private Long id;
    private Long recordId;
    private String materialType;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private Long fileSize;
    private String fileSuffix;
    private String mimeType;
    private String previewUrl;
    private Long uploadUserId;
    private String auditStatus;
    private String auditOpinion;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
