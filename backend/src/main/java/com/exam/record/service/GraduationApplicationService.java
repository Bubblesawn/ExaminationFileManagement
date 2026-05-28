package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.GraduationApplicationAuditDTO;
import com.exam.record.dto.GraduationApplicationSubmitDTO;
import com.exam.record.dto.GraduationApplicationUpdateDTO;
import com.exam.record.dto.GraduationApplicationWithdrawDTO;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.GraduationApplicationVO;
import com.exam.record.vo.GraduationEligibilityVO;

import java.util.List;

/**
 * @brief 毕业申请业务接口。
 */
public interface GraduationApplicationService extends IService<BusinessApplication> {

    /**
     * @brief 分页查询毕业申请。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param applicationStatus 申请状态。
     * @param recordId 考籍档案 ID。
     * @param candidateId 考生 ID。
     * @return 毕业申请分页数据。
     */
    Page<GraduationApplicationVO> pageGraduationApplications(long pageNo,
                                                             long pageSize,
                                                             String keyword,
                                                             String applicationStatus,
                                                             Long recordId,
                                                             Long candidateId);

    /**
     * @brief 查询毕业申请详情。
     *
     * @param id 毕业申请 ID。
     * @return 毕业申请详情。
     */
    GraduationApplicationVO getGraduationApplicationDetail(Long id);

    /**
     * @brief 校验指定考籍档案是否具备毕业申请资格。
     *
     * @param recordId 考籍档案 ID。
     * @return 毕业资格校验结果。
     */
    GraduationEligibilityVO checkGraduationEligibility(Long recordId);

    /**
     * @brief 提交毕业申请。
     *
     * @param dto 毕业申请提交请求对象。
     * @return 已提交的毕业申请。
     */
    GraduationApplicationVO submitGraduationApplication(GraduationApplicationSubmitDTO dto);

    /**
     * @brief 修改毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 毕业申请修改请求对象。
     * @return 修改后的毕业申请。
     */
    GraduationApplicationVO updateGraduationApplication(Long id, GraduationApplicationUpdateDTO dto);

    /**
     * @brief 撤回毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的毕业申请。
     */
    GraduationApplicationVO withdrawGraduationApplication(Long id, GraduationApplicationWithdrawDTO dto);

    /**
     * @brief 审核通过毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的毕业申请。
     */
    GraduationApplicationVO approveGraduationApplication(Long id, GraduationApplicationAuditDTO dto);

    /**
     * @brief 驳回毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的毕业申请。
     */
    GraduationApplicationVO rejectGraduationApplication(Long id, GraduationApplicationAuditDTO dto);

    /**
     * @brief 查询毕业申请结果。
     *
     * @param id 毕业申请 ID。
     * @return 毕业申请结果。
     */
    GraduationApplicationVO getGraduationApplicationResult(Long id);

    /**
     * @brief 查询毕业申请流程记录。
     *
     * @param id 毕业申请 ID。
     * @return 流程记录列表。
     */
    List<AuditRecordVO> listFlowRecords(Long id);
}
