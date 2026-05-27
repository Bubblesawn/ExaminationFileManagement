package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.ExemptionApplicationAuditDTO;
import com.exam.record.dto.ExemptionApplicationSubmitDTO;
import com.exam.record.dto.ExemptionApplicationUpdateDTO;
import com.exam.record.dto.ExemptionApplicationWithdrawDTO;
import com.exam.record.service.ExemptionApplicationService;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.ExemptionApplicationVO;
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
 * @brief 免考申请接口。
 *
 * @details
 * 对应第四阶段 4.2 任务，提供免考申请提交、修改、撤回、审核通过、驳回、
 * 分页查询、详情查询和流程记录查询能力。
 */
@RestController
@RequestMapping("/api/exemptions")
public class ExemptionApplicationController {
    private final ExemptionApplicationService exemptionApplicationService;

    /**
     * @brief 构造免考申请控制器。
     *
     * @param exemptionApplicationService 免考申请业务服务。
     */
    public ExemptionApplicationController(ExemptionApplicationService exemptionApplicationService) {
        this.exemptionApplicationService = exemptionApplicationService;
    }

    /**
     * @brief 分页查询免考申请。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param applicationStatus 申请状态。
     * @param recordId 考籍档案 ID。
     * @param candidateId 考生 ID。
     * @return 免考申请分页数据。
     */
    @GetMapping("/page")
    public Result<Page<ExemptionApplicationVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String applicationStatus,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) Long candidateId) {
        return Result.success(exemptionApplicationService.pageExemptionApplications(
                pageNo, pageSize, keyword, applicationStatus, recordId, candidateId));
    }

    /**
     * @brief 查询免考申请详情。
     *
     * @param id 免考申请 ID。
     * @return 免考申请详情。
     */
    @GetMapping("/{id}")
    public Result<ExemptionApplicationVO> detail(@PathVariable Long id) {
        return Result.success(exemptionApplicationService.getExemptionApplicationDetail(id));
    }

    /**
     * @brief 提交免考申请。
     *
     * @param dto 免考申请提交请求对象。
     * @return 已提交的免考申请。
     */
    @PostMapping
    public Result<ExemptionApplicationVO> submit(@Valid @RequestBody ExemptionApplicationSubmitDTO dto) {
        return Result.success(exemptionApplicationService.submitExemptionApplication(dto));
    }

    /**
     * @brief 修改免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 免考申请修改请求对象。
     * @return 修改后的免考申请。
     */
    @PutMapping("/{id}")
    public Result<ExemptionApplicationVO> update(@PathVariable Long id,
                                                 @Valid @RequestBody ExemptionApplicationUpdateDTO dto) {
        return Result.success(exemptionApplicationService.updateExemptionApplication(id, dto));
    }

    /**
     * @brief 撤回免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的免考申请。
     */
    @PutMapping("/{id}/withdraw")
    public Result<ExemptionApplicationVO> withdraw(@PathVariable Long id,
                                                   @Valid @RequestBody ExemptionApplicationWithdrawDTO dto) {
        return Result.success(exemptionApplicationService.withdrawExemptionApplication(id, dto));
    }

    /**
     * @brief 审核通过免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的免考申请。
     */
    @PutMapping("/{id}/approve")
    public Result<ExemptionApplicationVO> approve(@PathVariable Long id,
                                                  @Valid @RequestBody ExemptionApplicationAuditDTO dto) {
        return Result.success(exemptionApplicationService.approveExemptionApplication(id, dto));
    }

    /**
     * @brief 驳回免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的免考申请。
     */
    @PutMapping("/{id}/reject")
    public Result<ExemptionApplicationVO> reject(@PathVariable Long id,
                                                 @Valid @RequestBody ExemptionApplicationAuditDTO dto) {
        return Result.success(exemptionApplicationService.rejectExemptionApplication(id, dto));
    }

    /**
     * @brief 查询免考申请流程记录。
     *
     * @param id 免考申请 ID。
     * @return 流程记录列表。
     */
    @GetMapping("/{id}/flow-records")
    public Result<List<AuditRecordVO>> flowRecords(@PathVariable Long id) {
        return Result.success(exemptionApplicationService.listFlowRecords(id));
    }
}
