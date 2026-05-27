package com.exam.record.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;

/**
 * @brief 材料文件资源响应对象。
 *
 * @details
 * 服务层使用该对象同时返回文件资源、原始文件名和 MIME 类型，控制层据此构造
 * 下载或预览响应头。
 */
@Data
@AllArgsConstructor
public class MaterialFileResourceVO {
    private Resource resource;
    private String originalFileName;
    private String mimeType;
}
