package com.exam.record.vo;

import com.exam.record.entity.BusinessApplication;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @brief 业务申请材料包响应对象。
 *
 * @details
 * 用于材料审核页面按业务编号定位申请，并一次性返回业务摘要、已绑定材料 ID
 * 和该考籍档案下的材料清单，便于材料上传后业务审核页面同步读取。
 */
@Data
public class BusinessMaterialBundleVO {
    private Long id;
    private String applicationNo;
    private String businessType;
    private Long recordId;
    private Long candidateId;
    private String applicationTitle;
    private String applicationStatus;
    private String currentNodeName;
    private String applyUserName;
    private Long auditUserId;
    private String auditUserName;
    private LocalDateTime auditTime;
    private LocalDateTime submitTime;
    private List<Long> materialIds;
    private List<RecordMaterialVO> materials;

    /**
     * @brief 组装业务材料包响应对象。
     *
     * @param application 业务申请实体。
     * @param materialIds 业务申请已绑定材料 ID 列表。
     * @param materials 考籍档案材料列表。
     * @return 业务材料包响应对象。
     */
    public static BusinessMaterialBundleVO fromEntity(BusinessApplication application,
                                                      List<Long> materialIds,
                                                      List<RecordMaterialVO> materials) {
        BusinessMaterialBundleVO vo = new BusinessMaterialBundleVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setBusinessType(application.getBusinessType());
        vo.setRecordId(application.getRecordId());
        vo.setCandidateId(application.getCandidateId());
        vo.setApplicationTitle(application.getApplicationTitle());
        vo.setApplicationStatus(application.getApplicationStatus());
        vo.setCurrentNodeName(application.getCurrentNodeName());
        vo.setApplyUserName(application.getApplyUserName());
        vo.setAuditUserId(application.getAuditUserId());
        vo.setAuditUserName(application.getAuditUserName());
        vo.setAuditTime(application.getAuditTime());
        vo.setSubmitTime(application.getSubmitTime());
        vo.setMaterialIds(materialIds);
        vo.setMaterials(materials);
        return vo;
    }
}
