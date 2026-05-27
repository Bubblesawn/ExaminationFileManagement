package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.TransferApplicationAuditDTO;
import com.exam.record.dto.TransferApplicationSubmitDTO;
import com.exam.record.dto.TransferApplicationUpdateDTO;
import com.exam.record.dto.TransferApplicationWithdrawDTO;
import com.exam.record.service.TransferApplicationService;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.TransferApplicationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @brief 考籍转入转出申请接口。
 *
 * @details
 * 对应第四阶段 4.4 任务，提供考籍转入、考籍转出申请提交、修改、撤回、
 * 审核通过、驳回、分页查询、详情查询和流程记录查询能力。
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferApplicationController {
    private final TransferApplicationService transferApplicationService;

    /**
     * @brief 构造考籍转入转出申请控制器。
     *
     * @param transferApplicationService 考籍转入转出申请业务服务。
     */
    public TransferApplicationController(TransferApplicationService transferApplicationService) {
        this.transferApplicationService = transferApplicationService;
    }

    /**
     * @brief 分页查询考籍转入转出申请。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param transferType 转考类型。
     * @param applicationStatus 申请状态。
     * @param recordId 考籍档案 ID。
     * @param candidateId 考生 ID。
     * @return 考籍转入转出申请分页数据。
     */
    @GetMapping("/page")
    public Result<Page<TransferApplicationVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String applicationStatus,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) Long candidateId) {
        return Result.success(transferApplicationService.pageTransferApplications(
                pageNo, pageSize, keyword, transferType, applicationStatus, recordId, candidateId));
    }

    /**
     * @brief 查询考籍转入转出申请详情。
     *
     * @param id 转考申请 ID。
     * @return 转考申请详情。
     */
    @GetMapping("/{id}")
    public Result<TransferApplicationVO> detail(@PathVariable Long id) {
        return Result.success(transferApplicationService.getTransferApplicationDetail(id));
    }

    /**
     * @brief 提交考籍转入转出申请。
     *
     * @param dto 转考申请提交请求对象。
     * @return 已提交的转考申请。
     */
    @PostMapping
    public Result<TransferApplicationVO> submit(@Valid @RequestBody TransferApplicationSubmitDTO dto) {
        return Result.success(transferApplicationService.submitTransferApplication(dto));
    }

    /**
     * @brief 修改考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 转考申请修改请求对象。
     * @return 修改后的转考申请。
     */
    @PutMapping("/{id}")
    public Result<TransferApplicationVO> update(@PathVariable Long id,
                                                @Valid @RequestBody TransferApplicationUpdateDTO dto) {
        return Result.success(transferApplicationService.updateTransferApplication(id, dto));
    }

    /**
     * @brief 撤回考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的转考申请。
     */
    @PutMapping("/{id}/withdraw")
    public Result<TransferApplicationVO> withdraw(@PathVariable Long id,
                                                  @Valid @RequestBody TransferApplicationWithdrawDTO dto) {
        return Result.success(transferApplicationService.withdrawTransferApplication(id, dto));
    }

    /**
     * @brief 审核通过考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的转考申请。
     */
    @PutMapping("/{id}/approve")
    public Result<TransferApplicationVO> approve(@PathVariable Long id,
                                                 @Valid @RequestBody TransferApplicationAuditDTO dto) {
        return Result.success(transferApplicationService.approveTransferApplication(id, dto));
    }

    /**
     * @brief 驳回考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的转考申请。
     */
    @PutMapping("/{id}/reject")
    public Result<TransferApplicationVO> reject(@PathVariable Long id,
                                                @Valid @RequestBody TransferApplicationAuditDTO dto) {
        return Result.success(transferApplicationService.rejectTransferApplication(id, dto));
    }

    /**
     * @brief 查询考籍转入转出申请流程记录。
     *
     * @param id 转考申请 ID。
     * @return 流程记录列表。
     */
    @GetMapping("/{id}/flow-records")
    public Result<List<AuditRecordVO>> flowRecords(@PathVariable Long id) {
        return Result.success(transferApplicationService.listFlowRecords(id));
    }
}
