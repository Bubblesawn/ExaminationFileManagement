package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.TransferApplicationAuditDTO;
import com.exam.record.dto.TransferApplicationSubmitDTO;
import com.exam.record.dto.TransferApplicationUpdateDTO;
import com.exam.record.dto.TransferApplicationWithdrawDTO;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.TransferApplicationVO;

import java.util.List;

/**
 * @brief 考籍转入转出申请业务接口。
 */
public interface TransferApplicationService extends IService<BusinessApplication> {

    /**
     * @brief 分页查询考籍转入转出申请。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param transferType 转考类型，支持 TRANSFER_IN、TRANSFER_OUT。
     * @param applicationStatus 申请状态。
     * @param recordId 考籍档案 ID。
     * @param candidateId 考生 ID。
     * @return 考籍转入转出申请分页数据。
     */
    Page<TransferApplicationVO> pageTransferApplications(long pageNo,
                                                         long pageSize,
                                                         String keyword,
                                                         String transferType,
                                                         String applicationStatus,
                                                         Long recordId,
                                                         Long candidateId);

    /**
     * @brief 查询考籍转入转出申请详情。
     *
     * @param id 转考申请 ID。
     * @return 转考申请详情。
     */
    TransferApplicationVO getTransferApplicationDetail(Long id);

    /**
     * @brief 提交考籍转入转出申请。
     *
     * @param dto 转考申请提交请求对象。
     * @return 已提交的转考申请。
     */
    TransferApplicationVO submitTransferApplication(TransferApplicationSubmitDTO dto);

    /**
     * @brief 修改考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 转考申请修改请求对象。
     * @return 修改后的转考申请。
     */
    TransferApplicationVO updateTransferApplication(Long id, TransferApplicationUpdateDTO dto);

    /**
     * @brief 撤回考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的转考申请。
     */
    TransferApplicationVO withdrawTransferApplication(Long id, TransferApplicationWithdrawDTO dto);

    /**
     * @brief 审核通过考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的转考申请。
     */
    TransferApplicationVO approveTransferApplication(Long id, TransferApplicationAuditDTO dto);

    /**
     * @brief 驳回考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的转考申请。
     */
    TransferApplicationVO rejectTransferApplication(Long id, TransferApplicationAuditDTO dto);

    /**
     * @brief 查询考籍转入转出申请流程记录。
     *
     * @param id 转考申请 ID。
     * @return 流程记录列表。
     */
    List<AuditRecordVO> listFlowRecords(Long id);
}
