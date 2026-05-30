package com.exam.record.service;

import com.exam.record.dto.RecordMaterialUploadDTO;
import com.exam.record.vo.BusinessMaterialBundleVO;
import com.exam.record.vo.MaterialFileResourceVO;
import com.exam.record.vo.RecordMaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @brief 档案材料业务接口。
 */
public interface RecordMaterialService {
    /**
     * @brief 查询指定考籍档案下的材料列表。
     *
     * @param recordId 考籍档案ID。
     * @param materialType 材料类型，可为空。
     * @return 档案材料列表。
     */
    List<RecordMaterialVO> listMaterials(Long recordId, String materialType);

    /**
     * @brief 按业务编号查询业务申请及其材料列表。
     *
     * @param businessNo 业务申请编号或业务申请 ID。
     * @return 业务申请材料包。
     */
    BusinessMaterialBundleVO getBusinessMaterials(String businessNo);

    /**
     * @brief 上传档案材料。
     *
     * @param dto 材料上传业务字段。
     * @param file 上传文件。
     * @return 上传后的材料记录。
     */
    RecordMaterialVO uploadMaterial(RecordMaterialUploadDTO dto, MultipartFile file);

    /**
     * @brief 按业务编号上传材料并绑定到业务申请。
     *
     * @param businessNo 业务申请编号或业务申请 ID。
     * @param materialType 材料类型编码。
     * @param file 上传文件。
     * @return 上传后同步完成的业务申请材料包。
     */
    BusinessMaterialBundleVO uploadBusinessMaterial(String businessNo, String materialType, MultipartFile file);

    /**
     * @brief 查询材料下载资源。
     *
     * @param id 材料ID。
     * @return 文件资源响应对象。
     */
    MaterialFileResourceVO loadDownloadResource(Long id);

    /**
     * @brief 查询材料预览资源。
     *
     * @param id 材料ID。
     * @return 文件资源响应对象。
     */
    MaterialFileResourceVO loadPreviewResource(Long id);

    /**
     * @brief 删除单条档案材料及其本地文件。
     *
     * @param id 材料ID。
     */
    void deleteMaterial(Long id);
}
