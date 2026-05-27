package com.exam.record.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 智能问答和语音播报请求对象。
 */
@Data
public class AiChatDTO {
    @NotBlank(message = "文本内容不能为空")
    private String content;

    @JsonAlias("businessId")
    @JsonProperty("business_id")
    private Long businessId;
    private String scene;
}
