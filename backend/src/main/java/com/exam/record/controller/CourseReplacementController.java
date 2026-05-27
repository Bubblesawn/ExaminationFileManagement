package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.CourseReplacementApplicationSubmitDTO;
import com.exam.record.dto.CourseReplacementApplicationUpdateDTO;
import com.exam.record.dto.CourseReplacementRuleCreateDTO;
import com.exam.record.dto.CourseReplacementRuleStatusDTO;
import com.exam.record.dto.CourseReplacementRuleUpdateDTO;
import com.exam.record.dto.ExemptionApplicationAuditDTO;
import com.exam.record.dto.ExemptionApplicationWithdrawDTO;
import com.exam.record.service.CourseReplacementService;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.CourseReplacementApplicationVO;
import com.exam.record.vo.CourseReplacementRuleVO;
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
 * @brief 课程顶替接口。
 *
 * @details
 * 对应第四阶段 4.3 任务，提供课程顶替规则维护、课程顶替申请提交、修改、
 * 撤回、审核通过、审核驳回、分页查询、详情查询和流程记录查询能力。
 */
@RestController
@RequestMapping("/api/course-replacements")
public class CourseReplacementController {
    private final CourseReplacementService courseReplacementService;

    /**
     * @brief 构造课程顶替控制器。
     *
     * @param courseReplacementService 课程顶替业务服务。
     */
    public CourseReplacementController(CourseReplacementService courseReplacementService) {
        this.courseReplacementService = courseReplacementService;
    }

    /**
     * @brief 分页查询课程顶替规则。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param ruleStatus 规则状态。
     * @return 课程顶替规则分页数据。
     */
    @GetMapping("/rules/page")
    public Result<Page<CourseReplacementRuleVO>> pageRules(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ruleStatus) {
        return Result.success(courseReplacementService.pageRules(pageNo, pageSize, keyword, ruleStatus));
    }

    /**
     * @brief 查询课程顶替规则详情。
     *
     * @param id 规则 ID。
     * @return 课程顶替规则详情。
     */
    @GetMapping("/rules/{id}")
    public Result<CourseReplacementRuleVO> ruleDetail(@PathVariable Long id) {
        return Result.success(courseReplacementService.getRuleDetail(id));
    }

    /**
     * @brief 新增课程顶替规则。
     *
     * @param dto 新增请求对象。
     * @return 新增后的课程顶替规则。
     */
    @PostMapping("/rules")
    public Result<CourseReplacementRuleVO> createRule(@Valid @RequestBody CourseReplacementRuleCreateDTO dto) {
        return Result.success(courseReplacementService.createRule(dto));
    }

    /**
     * @brief 修改课程顶替规则。
     *
     * @param id 规则 ID。
     * @param dto 修改请求对象。
     * @return 修改后的课程顶替规则。
     */
    @PutMapping("/rules/{id}")
    public Result<CourseReplacementRuleVO> updateRule(@PathVariable Long id,
                                                      @Valid @RequestBody CourseReplacementRuleUpdateDTO dto) {
        return Result.success(courseReplacementService.updateRule(id, dto));
    }

    /**
     * @brief 修改课程顶替规则状态。
     *
     * @param id 规则 ID。
     * @param dto 状态请求对象。
     * @return 修改后的课程顶替规则。
     */
    @PutMapping("/rules/{id}/status")
    public Result<CourseReplacementRuleVO> updateRuleStatus(@PathVariable Long id,
                                                            @Valid @RequestBody CourseReplacementRuleStatusDTO dto) {
        return Result.success(courseReplacementService.updateRuleStatus(id, dto));
    }

    /**
     * @brief 分页查询课程顶替申请。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param applicationStatus 申请状态。
     * @param recordId 考籍档案 ID。
     * @param candidateId 考生 ID。
     * @return 课程顶替申请分页数据。
     */
    @GetMapping("/applications/page")
    public Result<Page<CourseReplacementApplicationVO>> pageApplications(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String applicationStatus,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) Long candidateId) {
        return Result.success(courseReplacementService.pageApplications(
                pageNo, pageSize, keyword, applicationStatus, recordId, candidateId));
    }

    /**
     * @brief 查询课程顶替申请详情。
     *
     * @param id 申请 ID。
     * @return 课程顶替申请详情。
     */
    @GetMapping("/applications/{id}")
    public Result<CourseReplacementApplicationVO> applicationDetail(@PathVariable Long id) {
        return Result.success(courseReplacementService.getApplicationDetail(id));
    }

    /**
     * @brief 提交课程顶替申请。
     *
     * @param dto 提交请求对象。
     * @return 已提交的课程顶替申请。
     */
    @PostMapping("/applications")
    public Result<CourseReplacementApplicationVO> submitApplication(
            @Valid @RequestBody CourseReplacementApplicationSubmitDTO dto) {
        return Result.success(courseReplacementService.submitApplication(dto));
    }

    /**
     * @brief 修改课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 修改请求对象。
     * @return 修改后的课程顶替申请。
     */
    @PutMapping("/applications/{id}")
    public Result<CourseReplacementApplicationVO> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody CourseReplacementApplicationUpdateDTO dto) {
        return Result.success(courseReplacementService.updateApplication(id, dto));
    }

    /**
     * @brief 撤回课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的课程顶替申请。
     */
    @PutMapping("/applications/{id}/withdraw")
    public Result<CourseReplacementApplicationVO> withdrawApplication(
            @PathVariable Long id,
            @Valid @RequestBody ExemptionApplicationWithdrawDTO dto) {
        return Result.success(courseReplacementService.withdrawApplication(id, dto));
    }

    /**
     * @brief 审核通过课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的课程顶替申请。
     */
    @PutMapping("/applications/{id}/approve")
    public Result<CourseReplacementApplicationVO> approveApplication(
            @PathVariable Long id,
            @Valid @RequestBody ExemptionApplicationAuditDTO dto) {
        return Result.success(courseReplacementService.approveApplication(id, dto));
    }

    /**
     * @brief 驳回课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的课程顶替申请。
     */
    @PutMapping("/applications/{id}/reject")
    public Result<CourseReplacementApplicationVO> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody ExemptionApplicationAuditDTO dto) {
        return Result.success(courseReplacementService.rejectApplication(id, dto));
    }

    /**
     * @brief 查询课程顶替申请流程记录。
     *
     * @param id 申请 ID。
     * @return 流程记录列表。
     */
    @GetMapping("/applications/{id}/flow-records")
    public Result<List<AuditRecordVO>> flowRecords(@PathVariable Long id) {
        return Result.success(courseReplacementService.listFlowRecords(id));
    }
}
