package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @brief 材料类型新增请求对象。
 */
@Data
public class MaterialTypeCreateDTO {
    @NotBlank(message = "材料类型编码不能为空")
    @Size(max = 64, message = "材料类型编码不能超过64个字符")
    private String typeCode;

    @NotBlank(message = "材料类型名称不能为空")
    @Size(max = 128, message = "材料类型名称不能超过128个字符")
    private String typeName;

    @Size(max = 512, message = "类型说明不能超过512个字符")
    private String description;

    private Integer sortOrder;

    private String status;
}
