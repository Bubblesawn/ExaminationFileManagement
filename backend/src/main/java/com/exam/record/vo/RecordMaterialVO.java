package com.exam.record.vo;

import com.exam.record.entity.RecordMaterial;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 档案材料响应对象。
 */
@Data
public class RecordMaterialVO {
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

    /**
     * @brief 将档案材料实体转换为响应对象。
     *
     * @param material 档案材料实体。
     * @return 档案材料响应对象。
     */
    public static RecordMaterialVO fromEntity(RecordMaterial material) {
        RecordMaterialVO vo = new RecordMaterialVO();
        vo.setId(material.getId());
        vo.setRecordId(material.getRecordId());
        vo.setMaterialType(material.getMaterialType());
        vo.setFileName(material.getFileName());
        vo.setOriginalFileName(material.getOriginalFileName());
        vo.setFileUrl(material.getFileUrl());
        vo.setFileSize(material.getFileSize());
        vo.setFileSuffix(material.getFileSuffix());
        vo.setMimeType(material.getMimeType());
        vo.setPreviewUrl(material.getPreviewUrl());
        vo.setUploadUserId(material.getUploadUserId());
        vo.setAuditStatus(material.getAuditStatus());
        vo.setAuditOpinion(material.getAuditOpinion());
        vo.setAuditUserId(material.getAuditUserId());
        vo.setAuditTime(material.getAuditTime());
        vo.setCreateTime(material.getCreateTime());
        vo.setUpdateTime(material.getUpdateTime());
        return vo;
    }
}
