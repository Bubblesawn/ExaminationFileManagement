package com.exam.record.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 智能图片任务请求对象。
 */
@Data
public class AiImageTaskDTO {
    @NotBlank(message = "图片文件地址不能为空")
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
    @JsonAlias("materialTypeHint")
    @JsonProperty("material_type_hint")
    private String materialTypeHint;
}
