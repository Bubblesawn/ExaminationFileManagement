package com.exam.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @brief 档案材料上传请求对象。
 *
 * @details
 * 该对象承载 multipart/form-data 中除文件以外的业务字段，控制层会将上传文件
 * 与该对象一起交给材料服务完成文件落盘和材料记录入库。
 */
@Data
public class RecordMaterialUploadDTO {
    @NotNull(message = "考籍档案ID不能为空")
    private Long recordId;

    @NotBlank(message = "材料类型不能为空")
    private String materialType;
}
