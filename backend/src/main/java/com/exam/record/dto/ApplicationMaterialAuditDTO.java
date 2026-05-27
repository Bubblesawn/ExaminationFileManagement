package com.exam.record.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @brief 申请材料智能核验请求对象。
 *
 * @details
 * 用于业务流程提交或审核前，一次性向算法服务传递申请类型和已上传材料，
 * 由算法服务返回材料分类、缺失材料提示和异常材料提醒结果。
 */
@Data
public class ApplicationMaterialAuditDTO {
    @JsonAlias("businessId")
    @JsonProperty("business_id")
    private Long businessId;

    @NotBlank(message = "申请类型不能为空")
    @JsonAlias("applicationType")
    @JsonProperty("application_type")
    private String applicationType;

    @JsonAlias("applicantName")
    @JsonProperty("applicant_name")
    private String applicantName;

    @Valid
    @NotNull(message = "申请材料不能为空")
    private List<ApplicationMaterialItemDTO> materials;

    /**
     * @brief 申请材料智能核验单项材料请求对象。
     */
    @Data
    public static class ApplicationMaterialItemDTO {
        @JsonAlias("materialId")
        @JsonProperty("material_id")
        private Long materialId;

        @NotBlank(message = "材料文件地址不能为空")
        @JsonAlias("fileUrl")
        @JsonProperty("file_url")
        private String fileUrl;

        @JsonAlias("fileName")
        @JsonProperty("file_name")
        private String fileName;

        @JsonAlias("materialTypeHint")
        @JsonProperty("material_type_hint")
        private String materialTypeHint;

        @JsonAlias("uploadedCategoryCode")
        @JsonProperty("uploaded_category_code")
        private String uploadedCategoryCode;
    }
}
