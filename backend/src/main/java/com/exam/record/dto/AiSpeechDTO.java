package com.exam.record.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @brief 智能语音识别请求对象。
 */
@Data
public class AiSpeechDTO {
    @NotBlank(message = "音频文件地址不能为空")
    @JsonAlias("audioUrl")
    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonAlias("businessId")
    @JsonProperty("business_id")
    private Long businessId;
    private String scene;
    @JsonAlias("languageHint")
    @JsonProperty("language_hint")
    private String languageHint = "zh-CN";
}
