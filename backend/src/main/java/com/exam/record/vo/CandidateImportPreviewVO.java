package com.exam.record.vo;

import lombok.Data;

import java.util.List;

/**
 * @brief 考生导入预览结果。
 *
 * @details
 * 当前阶段用于提供导入预留接口的稳定响应结构，后续接入 Excel 解析后可继续补充行级校验结果。
 */
@Data
public class CandidateImportPreviewVO {
    private String fileName;
    private Long fileSize;
    private List<String> expectedHeaders;
    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;
    private String message;
}

