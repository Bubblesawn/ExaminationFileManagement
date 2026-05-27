package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.ExemptionApplicationAuditDTO;
import com.exam.record.dto.ExemptionApplicationSubmitDTO;
import com.exam.record.dto.ExemptionApplicationUpdateDTO;
import com.exam.record.dto.ExemptionApplicationWithdrawDTO;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.ExemptionApplicationVO;

import java.util.List;

/**
 * @brief 免考申请业务接口。
 */
public interface ExemptionApplicationService extends IService<BusinessApplication> {

    /**
     * @brief 分页查询免考申请。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配申请编号、课程、考籍号和考生摘要。
     * @param applicationStatus 申请状态。
     * @param recordId 考籍档案 ID。
     * @param candidateId 考生 ID。
     * @return 免考申请分页数据。
     */
    Page<ExemptionApplicationVO> pageExemptionApplications(long pageNo,
                                                           long pageSize,
                                                           String keyword,
                                                           String applicationStatus,
                                                           Long recordId,
                                                           Long candidateId);

    /**
     * @brief 查询免考申请详情。
     *
     * @param id 免考申请 ID。
     * @return 免考申请详情。
     */
    ExemptionApplicationVO getExemptionApplicationDetail(Long id);

    /**
     * @brief 提交免考申请。
     *
     * @param dto 免考申请提交请求对象。
     * @return 已提交的免考申请。
     */
    ExemptionApplicationVO submitExemptionApplication(ExemptionApplicationSubmitDTO dto);

    /**
     * @brief 修改免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 免考申请修改请求对象。
     * @return 修改后的免考申请。
     */
    ExemptionApplicationVO updateExemptionApplication(Long id, ExemptionApplicationUpdateDTO dto);

    /**
     * @brief 撤回免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的免考申请。
     */
    ExemptionApplicationVO withdrawExemptionApplication(Long id, ExemptionApplicationWithdrawDTO dto);

    /**
     * @brief 审核通过免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的免考申请。
     */
    ExemptionApplicationVO approveExemptionApplication(Long id, ExemptionApplicationAuditDTO dto);

    /**
     * @brief 驳回免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的免考申请。
     */
    ExemptionApplicationVO rejectExemptionApplication(Long id, ExemptionApplicationAuditDTO dto);

    /**
     * @brief 查询免考申请流程记录。
     *
     * @param id 免考申请 ID。
     * @return 流程记录列表。
     */
    List<AuditRecordVO> listFlowRecords(Long id);
}
