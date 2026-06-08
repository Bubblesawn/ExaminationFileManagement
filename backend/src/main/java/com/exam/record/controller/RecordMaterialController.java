package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.dto.RecordMaterialUploadDTO;
import com.exam.record.service.RecordMaterialService;
import com.exam.record.vo.BusinessMaterialBundleVO;
import com.exam.record.vo.MaterialFileResourceVO;
import com.exam.record.vo.RecordMaterialVO;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @brief 档案材料管理接口。
 *
 * @details
 * 提供第三阶段 3.4 要求的材料上传、下载、预览、删除和材料列表查询能力。
 * 上传文件保存到后端本地材料目录，材料元数据写入 record_material 表。
 */
@Validated
@RestController
@RequestMapping("/api/materials")
public class RecordMaterialController {
    private final RecordMaterialService recordMaterialService;

    /**
     * @brief 构造档案材料控制器。
     *
     * @param recordMaterialService 档案材料业务服务。
     */
    public RecordMaterialController(RecordMaterialService recordMaterialService) {
        this.recordMaterialService = recordMaterialService;
    }

    /**
     * @brief 查询档案材料列表。
     *
     * @param recordId 考籍档案ID。
     * @param materialType 材料类型。
     * @return 档案材料列表。
     */
    @GetMapping
    public Result<List<RecordMaterialVO>> list(@RequestParam(required = false) Long recordId,
                                               @RequestParam(required = false) String materialType) {
        return Result.success(recordMaterialService.listMaterials(recordId, materialType));
    }

    /**
     * @brief 按业务编号查询业务申请及其材料。
     *
     * @param businessNo 业务申请编号或业务申请 ID。
     * @return 业务申请材料包。
     */
    @GetMapping("/business/{businessNo}")
    public Result<BusinessMaterialBundleVO> getBusinessMaterials(@PathVariable String businessNo) {
        return Result.success(recordMaterialService.getBusinessMaterials(businessNo));
    }

    /**
     * @brief 按考籍档案查询业务申请材料包列表。
     *
     * @param recordId 考籍档案ID。
     * @return 考籍档案相关业务申请材料包列表。
     */
    @GetMapping("/records/{recordId}/businesses")
    public Result<List<BusinessMaterialBundleVO>> listRecordBusinessMaterials(@PathVariable Long recordId) {
        return Result.success(recordMaterialService.listRecordBusinessMaterials(recordId));
    }

    /**
     * @brief 上传档案材料。
     *
     * @param dto 上传业务字段。
     * @param file 上传文件。
     * @return 上传后的材料记录。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<RecordMaterialVO> upload(@Valid @ModelAttribute RecordMaterialUploadDTO dto,
                                           @RequestPart("file") MultipartFile file) {
        return Result.success(recordMaterialService.uploadMaterial(dto, file));
    }

    /**
     * @brief 按业务编号上传材料并绑定到业务申请。
     *
     * @param businessNo 业务申请编号或业务申请 ID。
     * @param materialType 材料类型编码。
     * @param file 上传文件。
     * @return 上传后同步完成的业务申请材料包。
     */
    @PostMapping(value = "/business/{businessNo}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<BusinessMaterialBundleVO> uploadBusinessMaterial(@PathVariable String businessNo,
                                                                   @RequestParam String materialType,
                                                                   @RequestPart("file") MultipartFile file) {
        return Result.success(recordMaterialService.uploadBusinessMaterial(businessNo, materialType, file));
    }

    /**
     * @brief 审核通过单条档案材料。
     *
     * @param id 材料ID。
     * @return 审核通过后的材料记录。
     */
    @PutMapping("/{id}/approve")
    public Result<RecordMaterialVO> approveMaterial(@PathVariable Long id) {
        return Result.success(recordMaterialService.approveMaterial(id));
    }

    /**
     * @brief 下载材料文件。
     *
     * @param id 材料ID。
     * @return 文件下载响应。
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        MaterialFileResourceVO fileResource = recordMaterialService.loadDownloadResource(id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(fileResource.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileResource.getOriginalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(fileResource.getResource());
    }

    /**
     * @brief 预览材料文件。
     *
     * @param id 材料ID。
     * @return 文件预览响应。
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        MaterialFileResourceVO fileResource = recordMaterialService.loadPreviewResource(id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(fileResource.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(fileResource.getOriginalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(fileResource.getResource());
    }

    /**
     * @brief 删除单条档案材料。
     *
     * @param id 材料ID。
     * @return 无数据成功响应。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recordMaterialService.deleteMaterial(id);
        return Result.success();
    }

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(mimeType);
    }
}
