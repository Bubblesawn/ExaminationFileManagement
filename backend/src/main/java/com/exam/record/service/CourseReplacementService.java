package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.CourseReplacementApplicationSubmitDTO;
import com.exam.record.dto.CourseReplacementApplicationUpdateDTO;
import com.exam.record.dto.CourseReplacementRuleCreateDTO;
import com.exam.record.dto.CourseReplacementRuleStatusDTO;
import com.exam.record.dto.CourseReplacementRuleUpdateDTO;
import com.exam.record.dto.ExemptionApplicationAuditDTO;
import com.exam.record.dto.ExemptionApplicationWithdrawDTO;
import com.exam.record.entity.CourseReplacementRule;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.CourseReplacementApplicationVO;
import com.exam.record.vo.CourseReplacementRuleVO;

import java.util.List;

/**
 * @brief 课程顶替业务接口。
 */
public interface CourseReplacementService extends IService<CourseReplacementRule> {

    /**
     * @brief 分页查询课程顶替规则。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param ruleStatus 规则状态。
     * @return 课程顶替规则分页数据。
     */
    Page<CourseReplacementRuleVO> pageRules(long pageNo, long pageSize, String keyword, String ruleStatus);

    /**
     * @brief 查询课程顶替规则详情。
     *
     * @param id 规则 ID。
     * @return 课程顶替规则详情。
     */
    CourseReplacementRuleVO getRuleDetail(Long id);

    /**
     * @brief 新增课程顶替规则。
     *
     * @param dto 新增请求对象。
     * @return 新增后的规则。
     */
    CourseReplacementRuleVO createRule(CourseReplacementRuleCreateDTO dto);

    /**
     * @brief 修改课程顶替规则。
     *
     * @param id 规则 ID。
     * @param dto 修改请求对象。
     * @return 修改后的规则。
     */
    CourseReplacementRuleVO updateRule(Long id, CourseReplacementRuleUpdateDTO dto);

    /**
     * @brief 修改课程顶替规则状态。
     *
     * @param id 规则 ID。
     * @param dto 状态请求对象。
     * @return 修改后的规则。
     */
    CourseReplacementRuleVO updateRuleStatus(Long id, CourseReplacementRuleStatusDTO dto);

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
    Page<CourseReplacementApplicationVO> pageApplications(long pageNo,
                                                          long pageSize,
                                                          String keyword,
                                                          String applicationStatus,
                                                          Long recordId,
                                                          Long candidateId);

    /**
     * @brief 查询课程顶替申请详情。
     *
     * @param id 申请 ID。
     * @return 课程顶替申请详情。
     */
    CourseReplacementApplicationVO getApplicationDetail(Long id);

    /**
     * @brief 提交课程顶替申请。
     *
     * @param dto 提交请求对象。
     * @return 已提交的课程顶替申请。
     */
    CourseReplacementApplicationVO submitApplication(CourseReplacementApplicationSubmitDTO dto);

    /**
     * @brief 修改课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 修改请求对象。
     * @return 修改后的课程顶替申请。
     */
    CourseReplacementApplicationVO updateApplication(Long id, CourseReplacementApplicationUpdateDTO dto);

    /**
     * @brief 撤回课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的课程顶替申请。
     */
    CourseReplacementApplicationVO withdrawApplication(Long id, ExemptionApplicationWithdrawDTO dto);

    /**
     * @brief 审核通过课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的课程顶替申请。
     */
    CourseReplacementApplicationVO approveApplication(Long id, ExemptionApplicationAuditDTO dto);

    /**
     * @brief 驳回课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的课程顶替申请。
     */
    CourseReplacementApplicationVO rejectApplication(Long id, ExemptionApplicationAuditDTO dto);

    /**
     * @brief 查询课程顶替申请流程记录。
     *
     * @param id 申请 ID。
     * @return 流程记录列表。
     */
    List<AuditRecordVO> listFlowRecords(Long id);
}
