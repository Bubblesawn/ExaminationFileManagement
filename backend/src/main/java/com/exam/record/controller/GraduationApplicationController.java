package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.GraduationApplicationAuditDTO;
import com.exam.record.dto.GraduationApplicationSubmitDTO;
import com.exam.record.dto.GraduationApplicationUpdateDTO;
import com.exam.record.dto.GraduationApplicationWithdrawDTO;
import com.exam.record.service.GraduationApplicationService;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.GraduationApplicationVO;
import com.exam.record.vo.GraduationEligibilityVO;
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
 * @brief 毕业申请接口。
 *
 * @details
 * 对应第四阶段 4.5 任务，提供毕业申请提交、修改、撤回、资格校验、审核通过、
 * 审核驳回、分页查询、详情查询、结果查询和流程记录查询能力。
 */
@RestController
@RequestMapping("/api/graduations")
public class GraduationApplicationController {
    private final GraduationApplicationService graduationApplicationService;

    /**
     * @brief 构造毕业申请控制器。
     *
     * @param graduationApplicationService 毕业申请业务服务。
     */
    public GraduationApplicationController(GraduationApplicationService graduationApplicationService) {
        this.graduationApplicationService = graduationApplicationService;
    }

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
    @GetMapping("/page")
    public Result<Page<GraduationApplicationVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String applicationStatus,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) Long candidateId) {
        return Result.success(graduationApplicationService.pageGraduationApplications(
                pageNo, pageSize, keyword, applicationStatus, recordId, candidateId));
    }

    /**
     * @brief 查询毕业申请详情。
     *
     * @param id 毕业申请 ID。
     * @return 毕业申请详情。
     */
    @GetMapping("/{id}")
    public Result<GraduationApplicationVO> detail(@PathVariable Long id) {
        return Result.success(graduationApplicationService.getGraduationApplicationDetail(id));
    }

    /**
     * @brief 校验考籍档案毕业资格。
     *
     * @param recordId 考籍档案 ID。
     * @return 毕业资格校验结果。
     */
    @GetMapping("/eligibility/{recordId}")
    public Result<GraduationEligibilityVO> eligibility(@PathVariable Long recordId) {
        return Result.success(graduationApplicationService.checkGraduationEligibility(recordId));
    }

    /**
     * @brief 提交毕业申请。
     *
     * @param dto 毕业申请提交请求对象。
     * @return 已提交的毕业申请。
     */
    @PostMapping
    public Result<GraduationApplicationVO> submit(@Valid @RequestBody GraduationApplicationSubmitDTO dto) {
        return Result.success(graduationApplicationService.submitGraduationApplication(dto));
    }

    /**
     * @brief 修改毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 毕业申请修改请求对象。
     * @return 修改后的毕业申请。
     */
    @PutMapping("/{id}")
    public Result<GraduationApplicationVO> update(@PathVariable Long id,
                                                  @Valid @RequestBody GraduationApplicationUpdateDTO dto) {
        return Result.success(graduationApplicationService.updateGraduationApplication(id, dto));
    }

    /**
     * @brief 撤回毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的毕业申请。
     */
    @PutMapping("/{id}/withdraw")
    public Result<GraduationApplicationVO> withdraw(@PathVariable Long id,
                                                    @Valid @RequestBody GraduationApplicationWithdrawDTO dto) {
        return Result.success(graduationApplicationService.withdrawGraduationApplication(id, dto));
    }

    /**
     * @brief 审核通过毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的毕业申请。
     */
    @PutMapping("/{id}/approve")
    public Result<GraduationApplicationVO> approve(@PathVariable Long id,
                                                   @Valid @RequestBody GraduationApplicationAuditDTO dto) {
        return Result.success(graduationApplicationService.approveGraduationApplication(id, dto));
    }

    /**
     * @brief 驳回毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的毕业申请。
     */
    @PutMapping("/{id}/reject")
    public Result<GraduationApplicationVO> reject(@PathVariable Long id,
                                                  @Valid @RequestBody GraduationApplicationAuditDTO dto) {
        return Result.success(graduationApplicationService.rejectGraduationApplication(id, dto));
    }

    /**
     * @brief 查询毕业申请审核结果。
     *
     * @param id 毕业申请 ID。
     * @return 毕业申请审核结果。
     */
    @GetMapping("/{id}/result")
    public Result<GraduationApplicationVO> result(@PathVariable Long id) {
        return Result.success(graduationApplicationService.getGraduationApplicationResult(id));
    }

    /**
     * @brief 查询毕业申请流程记录。
     *
     * @param id 毕业申请 ID。
     * @return 流程记录列表。
     */
    @GetMapping("/{id}/flow-records")
    public Result<List<AuditRecordVO>> flowRecords(@PathVariable Long id) {
        return Result.success(graduationApplicationService.listFlowRecords(id));
    }
}
