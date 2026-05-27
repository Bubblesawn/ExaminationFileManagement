package com.exam.record.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @brief 材料文件上传响应视图对象。
 *
 * @details
 * 保存前端上传的真实材料文件后，向页面返回可访问地址和文件元信息，
 * 供智能识别、材料核验以及人工审核页面继续使用。
 */
@Data
@AllArgsConstructor
public class MaterialUploadVO {
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long size;
}
