package com.exam.record.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 材料预处理请求对象。
 *
 * @details
 * 用于上传材料进入业务审核前，将文件地址、基础文件信息和业务场景传递给算法服务，
 * 由算法服务返回格式校验、图片清晰度检测和基础材料分类结果。
 */
@Data
public class MaterialPreprocessDTO {
    @NotBlank(message = "材料文件地址不能为空")
    @JsonAlias("fileUrl")
    @JsonProperty("file_url")
    private String fileUrl;

    @JsonAlias("businessId")
    @JsonProperty("business_id")
    private Long businessId;

    private String scene;

    @JsonAlias("fileName")
    @JsonProperty("file_name")
    private String fileName;

    @JsonAlias("contentType")
    @JsonProperty("content_type")
    private String contentType;

    @Min(value = 0, message = "文件大小不能小于0")
    @JsonAlias("fileSizeKb")
    @JsonProperty("file_size_kb")
    private Integer fileSizeKb;

    @JsonAlias("materialTypeHint")
    @JsonProperty("material_type_hint")
    private String materialTypeHint;
}
