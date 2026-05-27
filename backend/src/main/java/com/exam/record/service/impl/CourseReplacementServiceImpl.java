package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.CourseReplacementApplicationSubmitDTO;
import com.exam.record.dto.CourseReplacementApplicationUpdateDTO;
import com.exam.record.dto.CourseReplacementRuleCreateDTO;
import com.exam.record.dto.CourseReplacementRuleStatusDTO;
import com.exam.record.dto.CourseReplacementRuleUpdateDTO;
import com.exam.record.dto.ExemptionApplicationAuditDTO;
import com.exam.record.dto.ExemptionApplicationWithdrawDTO;
import com.exam.record.entity.ApplicationExtensionField;
import com.exam.record.entity.AuditRecord;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.CourseReplacementRule;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.ApplicationExtensionFieldMapper;
import com.exam.record.mapper.AuditRecordMapper;
import com.exam.record.mapper.BusinessApplicationMapper;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.mapper.CourseReplacementRuleMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.CourseReplacementService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.CourseReplacementApplicationVO;
import com.exam.record.vo.CourseReplacementRuleVO;
import com.exam.record.vo.TokenUserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @brief 课程顶替业务实现。
 *
 * @details
 * 对应第四阶段 4.3 任务，负责课程顶替规则维护、课程顶替申请提交、修改、
 * 撤回、审核通过、审核驳回和流程记录查询。申请主流程复用 business_application，
 * 差异字段写入 application_extension_field 和 extension_data_json 快照。
 */
@Service
public class CourseReplacementServiceImpl extends ServiceImpl<CourseReplacementRuleMapper, CourseReplacementRule>
        implements CourseReplacementService {
    private static final String BUSINESS_TYPE_COURSE_REPLACE = "COURSE_REPLACE";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_UPDATED = "UPDATED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String RULE_STATUS_ENABLED = "ENABLED";
    private static final String RULE_STATUS_DISABLED = "DISABLED";
    private static final DateTimeFormatter APPLICATION_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final TypeReference<List<Long>> MATERIAL_ID_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> EXTENSION_DATA_TYPE = new TypeReference<>() {
    };

    private final BusinessApplicationMapper businessApplicationMapper;
    private final StudentRecordMapper studentRecordMapper;
    private final CandidateMapper candidateMapper;
    private final RecordMaterialMapper recordMaterialMapper;
    private final ApplicationExtensionFieldMapper extensionFieldMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final ObjectMapper objectMapper;

    /**
     * @brief 构造课程顶替业务实现。
     *
     * @param businessApplicationMapper 通用业务申请 Mapper。
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param candidateMapper 考生信息 Mapper。
     * @param recordMaterialMapper 档案材料 Mapper。
     * @param extensionFieldMapper 扩展字段 Mapper。
     * @param auditRecordMapper 审核记录 Mapper。
     * @param objectMapper JSON 序列化组件。
     */
    public CourseReplacementServiceImpl(BusinessApplicationMapper businessApplicationMapper,
                                        StudentRecordMapper studentRecordMapper,
                                        CandidateMapper candidateMapper,
                                        RecordMaterialMapper recordMaterialMapper,
                                        ApplicationExtensionFieldMapper extensionFieldMapper,
                                        AuditRecordMapper auditRecordMapper,
                                        ObjectMapper objectMapper) {
        this.businessApplicationMapper = businessApplicationMapper;
        this.studentRecordMapper = studentRecordMapper;
        this.candidateMapper = candidateMapper;
        this.recordMaterialMapper = recordMaterialMapper;
        this.extensionFieldMapper = extensionFieldMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.objectMapper = objectMapper;
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
    @Override
    public Page<CourseReplacementRuleVO> pageRules(long pageNo, long pageSize, String keyword, String ruleStatus) {
        validateOptionalRuleStatus(ruleStatus);
        LambdaQueryWrapper<CourseReplacementRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(CourseReplacementRule::getSourceCourseCode, keyword)
                    .or()
                    .like(CourseReplacementRule::getSourceCourseName, keyword)
                    .or()
                    .like(CourseReplacementRule::getTargetCourseCode, keyword)
                    .or()
                    .like(CourseReplacementRule::getTargetCourseName, keyword)
                    .or()
                    .like(CourseReplacementRule::getMajorCode, keyword));
        }
        if (StringUtils.hasText(ruleStatus)) {
            wrapper.eq(CourseReplacementRule::getRuleStatus, ruleStatus);
        }
        wrapper.orderByDesc(CourseReplacementRule::getCreateTime).orderByDesc(CourseReplacementRule::getId);
        Page<CourseReplacementRule> rulePage = page(new Page<>(pageNo, pageSize), wrapper);
        Page<CourseReplacementRuleVO> voPage = new Page<>(rulePage.getCurrent(), rulePage.getSize(), rulePage.getTotal());
        voPage.setRecords(rulePage.getRecords().stream().map(CourseReplacementRuleVO::fromEntity).toList());
        return voPage;
    }

    /**
     * @brief 查询课程顶替规则详情。
     *
     * @param id 规则 ID。
     * @return 课程顶替规则详情。
     */
    @Override
    public CourseReplacementRuleVO getRuleDetail(Long id) {
        return CourseReplacementRuleVO.fromEntity(getExistingRule(id));
    }

    /**
     * @brief 新增课程顶替规则。
     *
     * @param dto 新增请求对象。
     * @return 新增后的规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementRuleVO createRule(CourseReplacementRuleCreateDTO dto) {
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());
        validateUniqueRule(dto.getSourceCourseCode(), dto.getTargetCourseCode(), dto.getMajorCode(), null);
        CourseReplacementRule rule = new CourseReplacementRule();
        fillRule(rule, dto);
        rule.setRuleStatus(RULE_STATUS_ENABLED);
        save(rule);
        return getRuleDetail(rule.getId());
    }

    /**
     * @brief 修改课程顶替规则。
     *
     * @param id 规则 ID。
     * @param dto 修改请求对象。
     * @return 修改后的规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementRuleVO updateRule(Long id, CourseReplacementRuleUpdateDTO dto) {
        CourseReplacementRule rule = getExistingRule(id);
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());
        validateUniqueRule(dto.getSourceCourseCode(), dto.getTargetCourseCode(), dto.getMajorCode(), id);
        fillRule(rule, dto);
        updateById(rule);
        return getRuleDetail(id);
    }

    /**
     * @brief 修改课程顶替规则状态。
     *
     * @param id 规则 ID。
     * @param dto 状态请求对象。
     * @return 修改后的规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementRuleVO updateRuleStatus(Long id, CourseReplacementRuleStatusDTO dto) {
        validateRuleStatus(dto.getRuleStatus());
        CourseReplacementRule rule = getExistingRule(id);
        rule.setRuleStatus(dto.getRuleStatus());
        updateById(rule);
        return getRuleDetail(id);
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
    @Override
    public Page<CourseReplacementApplicationVO> pageApplications(long pageNo,
                                                                 long pageSize,
                                                                 String keyword,
                                                                 String applicationStatus,
                                                                 Long recordId,
                                                                 Long candidateId) {
        validateOptionalApplicationStatus(applicationStatus);
        LambdaQueryWrapper<BusinessApplication> wrapper = buildApplicationQueryWrapper(
                keyword, applicationStatus, recordId, candidateId);
        Page<BusinessApplication> applicationPage = businessApplicationMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, StudentRecord> recordMap = loadRecordMap(applicationPage.getRecords());
        Map<Long, Candidate> candidateMap = loadCandidateMap(applicationPage.getRecords());
        Page<CourseReplacementApplicationVO> voPage = new Page<>(
                applicationPage.getCurrent(), applicationPage.getSize(), applicationPage.getTotal());
        voPage.setRecords(applicationPage.getRecords().stream()
                .map(application -> CourseReplacementApplicationVO.fromEntity(
                        application,
                        recordMap.get(application.getRecordId()),
                        candidateMap.get(application.getCandidateId()),
                        parseExtensionData(application),
                        parseMaterialIds(application)))
                .toList());
        return voPage;
    }

    /**
     * @brief 查询课程顶替申请详情。
     *
     * @param id 申请 ID。
     * @return 课程顶替申请详情。
     */
    @Override
    public CourseReplacementApplicationVO getApplicationDetail(Long id) {
        BusinessApplication application = getExistingApplication(id);
        StudentRecord record = studentRecordMapper.selectById(application.getRecordId());
        Candidate candidate = candidateMapper.selectById(application.getCandidateId());
        return CourseReplacementApplicationVO.fromEntity(
                application, record, candidate, parseExtensionData(application), parseMaterialIds(application));
    }

    /**
     * @brief 提交课程顶替申请。
     *
     * @details
     * 提交时校验考籍档案、启用规则、规则适用范围、材料归属和重复在办申请。
     * 审核通过或驳回前均保持 SUBMITTED 状态，并写入流程记录以便追踪。
     *
     * @param dto 提交请求对象。
     * @return 已提交的课程顶替申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementApplicationVO submitApplication(CourseReplacementApplicationSubmitDTO dto) {
        StudentRecord record = getExistingRecord(dto.getRecordId());
        CourseReplacementRule rule = getUsableRule(dto.getRuleId(), record);
        validateNoActiveApplication(record.getId(), rule.getSourceCourseCode(), rule.getTargetCourseCode(), null);
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(record.getId(), materialIds);
        TokenUserVO user = AuthContextHolder.getUser();
        BusinessApplication application = new BusinessApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setBusinessType(BUSINESS_TYPE_COURSE_REPLACE);
        application.setRecordId(record.getId());
        application.setCandidateId(record.getCandidateId());
        application.setApplicationTitle(rule.getSourceCourseName() + "顶替" + rule.getTargetCourseName());
        application.setApplicationStatus(STATUS_SUBMITTED);
        application.setCurrentNodeCode(STATUS_SUBMITTED);
        application.setCurrentNodeName("已提交");
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(buildExtensionData(rule, dto.getApplyReason())));
        application.setApplyUserId(user == null ? null : user.getId());
        application.setApplyUserName(user == null ? null : user.getRealName());
        application.setSubmitTime(LocalDateTime.now());
        application.setRemark(dto.getRemark());
        businessApplicationMapper.insert(application);
        saveExtensionFields(application.getId(), buildExtensionData(rule, dto.getApplyReason()));
        saveFlowRecord(application, null, STATUS_SUBMITTED, "SUBMIT", "课程顶替申请已提交");
        return getApplicationDetail(application.getId());
    }

    /**
     * @brief 修改课程顶替申请。
     *
     * @details
     * 仅允许修改 SUBMITTED 状态的申请。修改时会重新读取规则并刷新扩展字段快照，
     * 避免申请中保留已经停用或不适用的规则。
     *
     * @param id 申请 ID。
     * @param dto 修改请求对象。
     * @return 修改后的课程顶替申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementApplicationVO updateApplication(Long id, CourseReplacementApplicationUpdateDTO dto) {
        BusinessApplication application = getExistingApplication(id);
        assertSubmittedStatus(application, "只有已提交的课程顶替申请可以修改");
        StudentRecord record = getExistingRecord(dto.getRecordId());
        if (!record.getId().equals(application.getRecordId())) {
            throw new BusinessException(400, "不能修改课程顶替申请所属考籍档案");
        }
        CourseReplacementRule rule = getUsableRule(dto.getRuleId(), record);
        validateNoActiveApplication(record.getId(), rule.getSourceCourseCode(), rule.getTargetCourseCode(), id);
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(record.getId(), materialIds);
        Map<String, String> extensionData = buildExtensionData(rule, dto.getApplyReason());
        application.setApplicationTitle(rule.getSourceCourseName() + "顶替" + rule.getTargetCourseName());
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(extensionData));
        application.setRemark(dto.getRemark());
        businessApplicationMapper.updateById(application);
        saveExtensionFields(id, extensionData);
        saveFlowRecord(application, STATUS_SUBMITTED, STATUS_SUBMITTED, "UPDATE", "课程顶替申请内容已修改");
        return getApplicationDetail(id);
    }

    /**
     * @brief 撤回课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的课程顶替申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementApplicationVO withdrawApplication(Long id, ExemptionApplicationWithdrawDTO dto) {
        BusinessApplication application = getExistingApplication(id);
        assertSubmittedStatus(application, "只有已提交的课程顶替申请可以撤回");
        application.setApplicationStatus(STATUS_WITHDRAWN);
        application.setCurrentNodeCode(STATUS_WITHDRAWN);
        application.setCurrentNodeName("已撤回");
        application.setWithdrawTime(LocalDateTime.now());
        application.setWithdrawReason(dto.getWithdrawReason());
        businessApplicationMapper.updateById(application);
        saveFlowRecord(application, STATUS_SUBMITTED, STATUS_WITHDRAWN, "WITHDRAW", dto.getWithdrawReason());
        return getApplicationDetail(id);
    }

    /**
     * @brief 审核通过课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的课程顶替申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementApplicationVO approveApplication(Long id, ExemptionApplicationAuditDTO dto) {
        return auditApplication(id, dto, STATUS_APPROVED, "APPROVE", "审核通过");
    }

    /**
     * @brief 驳回课程顶替申请。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的课程顶替申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseReplacementApplicationVO rejectApplication(Long id, ExemptionApplicationAuditDTO dto) {
        return auditApplication(id, dto, STATUS_REJECTED, "REJECT", "审核驳回");
    }

    /**
     * @brief 查询课程顶替申请流程记录。
     *
     * @param id 申请 ID。
     * @return 流程记录列表。
     */
    @Override
    public List<AuditRecordVO> listFlowRecords(Long id) {
        getExistingApplication(id);
        return auditRecordMapper.selectList(new LambdaQueryWrapper<AuditRecord>()
                        .eq(AuditRecord::getBusinessType, BUSINESS_TYPE_COURSE_REPLACE)
                        .eq(AuditRecord::getBusinessId, id)
                        .orderByAsc(AuditRecord::getOperationTime)
                        .orderByAsc(AuditRecord::getId))
                .stream()
                .map(AuditRecordVO::fromEntity)
                .toList();
    }

    /**
     * @brief 执行课程顶替审核状态流转。
     *
     * @details
     * 审核入口只接受 SUBMITTED 状态，目标状态只能为 APPROVED 或 REJECTED。
     * 状态、审核人、审核意见写入通用申请表，并同步生成 audit_record 流程记录。
     *
     * @param id 申请 ID。
     * @param dto 审核请求对象。
     * @param targetStatus 目标状态。
     * @param action 流程动作。
     * @param nodeName 当前节点名称。
     * @return 审核后的课程顶替申请。
     */
    private CourseReplacementApplicationVO auditApplication(Long id,
                                                            ExemptionApplicationAuditDTO dto,
                                                            String targetStatus,
                                                            String action,
                                                            String nodeName) {
        BusinessApplication application = getExistingApplication(id);
        assertSubmittedStatus(application, "只有已提交的课程顶替申请可以审核");
        TokenUserVO user = AuthContextHolder.getUser();
        application.setApplicationStatus(targetStatus);
        application.setCurrentNodeCode(targetStatus);
        application.setCurrentNodeName(nodeName);
        application.setAuditUserId(user == null ? null : user.getId());
        application.setAuditUserName(user == null ? null : user.getRealName());
        application.setAuditTime(LocalDateTime.now());
        application.setAuditOpinion(dto.getAuditOpinion());
        businessApplicationMapper.updateById(application);
        saveFlowRecord(application, STATUS_SUBMITTED, targetStatus, action, dto.getAuditOpinion());
        return getApplicationDetail(id);
    }

    private void fillRule(CourseReplacementRule rule, CourseReplacementRuleCreateDTO dto) {
        rule.setSourceCourseCode(dto.getSourceCourseCode());
        rule.setSourceCourseName(dto.getSourceCourseName());
        rule.setTargetCourseCode(dto.getTargetCourseCode());
        rule.setTargetCourseName(dto.getTargetCourseName());
        rule.setMajorCode(dto.getMajorCode());
        rule.setEducationLevel(dto.getEducationLevel());
        rule.setCredit(dto.getCredit());
        rule.setEffectiveDate(dto.getEffectiveDate());
        rule.setExpireDate(dto.getExpireDate());
        rule.setRemark(dto.getRemark());
    }

    private LambdaQueryWrapper<BusinessApplication> buildApplicationQueryWrapper(String keyword,
                                                                                String applicationStatus,
                                                                                Long recordId,
                                                                                Long candidateId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_COURSE_REPLACE);
        if (StringUtils.hasText(keyword)) {
            Set<Long> candidateIds = findCandidateIds(keyword);
            Set<Long> recordIds = findRecordIds(keyword, candidateIds);
            wrapper.and(query -> {
                query.like(BusinessApplication::getApplicationNo, keyword)
                        .or()
                        .like(BusinessApplication::getApplicationTitle, keyword)
                        .or()
                        .like(BusinessApplication::getExtensionDataJson, keyword)
                        .or()
                        .like(BusinessApplication::getRemark, keyword);
                if (!recordIds.isEmpty()) {
                    query.or().in(BusinessApplication::getRecordId, recordIds);
                }
                if (!candidateIds.isEmpty()) {
                    query.or().in(BusinessApplication::getCandidateId, candidateIds);
                }
            });
        }
        if (StringUtils.hasText(applicationStatus)) {
            wrapper.eq(BusinessApplication::getApplicationStatus, applicationStatus);
        }
        if (recordId != null) {
            wrapper.eq(BusinessApplication::getRecordId, recordId);
        }
        if (candidateId != null) {
            wrapper.eq(BusinessApplication::getCandidateId, candidateId);
        }
        wrapper.orderByDesc(BusinessApplication::getSubmitTime).orderByDesc(BusinessApplication::getId);
        return wrapper;
    }

    private Set<Long> findCandidateIds(String keyword) {
        return candidateMapper.selectList(new LambdaQueryWrapper<Candidate>()
                        .like(Candidate::getName, keyword)
                        .or()
                        .like(Candidate::getIdCard, keyword)
                        .or()
                        .like(Candidate::getAdmissionNo, keyword))
                .stream()
                .map(Candidate::getId)
                .collect(Collectors.toSet());
    }

    private Set<Long> findRecordIds(String keyword, Set<Long> candidateIds) {
        LambdaQueryWrapper<StudentRecord> wrapper = new LambdaQueryWrapper<StudentRecord>()
                .like(StudentRecord::getRecordNo, keyword)
                .or()
                .like(StudentRecord::getMajorCode, keyword)
                .or()
                .like(StudentRecord::getMajorName, keyword);
        if (!candidateIds.isEmpty()) {
            wrapper.or().in(StudentRecord::getCandidateId, candidateIds);
        }
        return studentRecordMapper.selectList(wrapper).stream().map(StudentRecord::getId).collect(Collectors.toSet());
    }

    private Map<Long, StudentRecord> loadRecordMap(List<BusinessApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> recordIds = applications.stream().map(BusinessApplication::getRecordId).collect(Collectors.toSet());
        return studentRecordMapper.selectList(new LambdaQueryWrapper<StudentRecord>().in(StudentRecord::getId, recordIds))
                .stream()
                .collect(Collectors.toMap(StudentRecord::getId, Function.identity()));
    }

    private Map<Long, Candidate> loadCandidateMap(List<BusinessApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> candidateIds = applications.stream().map(BusinessApplication::getCandidateId).collect(Collectors.toSet());
        return candidateMapper.selectList(new LambdaQueryWrapper<Candidate>().in(Candidate::getId, candidateIds))
                .stream()
                .collect(Collectors.toMap(Candidate::getId, Function.identity()));
    }

    private CourseReplacementRule getExistingRule(Long id) {
        CourseReplacementRule rule = getById(id);
        if (rule == null) {
            throw new BusinessException(404, "课程顶替规则不存在");
        }
        return rule;
    }

    private CourseReplacementRule getUsableRule(Long id, StudentRecord record) {
        CourseReplacementRule rule = getExistingRule(id);
        if (!RULE_STATUS_ENABLED.equals(rule.getRuleStatus())) {
            throw new BusinessException(400, "课程顶替规则未启用");
        }
        LocalDate today = LocalDate.now();
        if (rule.getEffectiveDate() != null && today.isBefore(rule.getEffectiveDate())) {
            throw new BusinessException(400, "课程顶替规则尚未生效");
        }
        if (rule.getExpireDate() != null && today.isAfter(rule.getExpireDate())) {
            throw new BusinessException(400, "课程顶替规则已失效");
        }
        if (StringUtils.hasText(rule.getMajorCode()) && !rule.getMajorCode().equals(record.getMajorCode())) {
            throw new BusinessException(400, "课程顶替规则不适用于当前考籍专业");
        }
        if (StringUtils.hasText(rule.getEducationLevel()) && !rule.getEducationLevel().equals(record.getEducationLevel())) {
            throw new BusinessException(400, "课程顶替规则不适用于当前考籍层次");
        }
        return rule;
    }

    private BusinessApplication getExistingApplication(Long id) {
        BusinessApplication application = businessApplicationMapper.selectById(id);
        if (application == null || !BUSINESS_TYPE_COURSE_REPLACE.equals(application.getBusinessType())) {
            throw new BusinessException(404, "课程顶替申请不存在");
        }
        return application;
    }

    private StudentRecord getExistingRecord(Long recordId) {
        StudentRecord record = studentRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "考籍档案不存在");
        }
        return record;
    }

    private void validateUniqueRule(String sourceCourseCode, String targetCourseCode, String majorCode, Long excludeId) {
        LambdaQueryWrapper<CourseReplacementRule> wrapper = new LambdaQueryWrapper<CourseReplacementRule>()
                .eq(CourseReplacementRule::getSourceCourseCode, sourceCourseCode)
                .eq(CourseReplacementRule::getTargetCourseCode, targetCourseCode);
        if (StringUtils.hasText(majorCode)) {
            wrapper.eq(CourseReplacementRule::getMajorCode, majorCode);
        } else {
            wrapper.isNull(CourseReplacementRule::getMajorCode);
        }
        if (excludeId != null) {
            wrapper.ne(CourseReplacementRule::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "相同课程和专业范围的顶替规则已存在");
        }
    }

    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && expireDate.isBefore(effectiveDate)) {
            throw new BusinessException(400, "失效日期不能早于生效日期");
        }
    }

    private void validateRuleStatus(String ruleStatus) {
        if (!RULE_STATUS_ENABLED.equals(ruleStatus) && !RULE_STATUS_DISABLED.equals(ruleStatus)) {
            throw new BusinessException(400, "规则状态只能为ENABLED或DISABLED");
        }
    }

    private void validateOptionalRuleStatus(String ruleStatus) {
        if (StringUtils.hasText(ruleStatus)) {
            validateRuleStatus(ruleStatus);
        }
    }

    private void validateOptionalApplicationStatus(String status) {
        if (StringUtils.hasText(status) && !STATUS_SUBMITTED.equals(status)
                && !STATUS_APPROVED.equals(status)
                && !STATUS_REJECTED.equals(status)
                && !STATUS_WITHDRAWN.equals(status)) {
            throw new BusinessException(400, "课程顶替申请状态只能为SUBMITTED、APPROVED、REJECTED或WITHDRAWN");
        }
    }

    private void validateNoActiveApplication(Long recordId, String sourceCourseCode, String targetCourseCode, Long excludeId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_COURSE_REPLACE)
                .eq(BusinessApplication::getRecordId, recordId)
                .like(BusinessApplication::getExtensionDataJson, "\"sourceCourseCode\":\"" + sourceCourseCode + "\"")
                .like(BusinessApplication::getExtensionDataJson, "\"targetCourseCode\":\"" + targetCourseCode + "\"")
                .in(BusinessApplication::getApplicationStatus, List.of(STATUS_SUBMITTED, STATUS_APPROVED));
        if (excludeId != null) {
            wrapper.ne(BusinessApplication::getId, excludeId);
        }
        Long count = businessApplicationMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(409, "该档案的相同课程顶替已有在办或已通过申请");
        }
    }

    private void assertSubmittedStatus(BusinessApplication application, String message) {
        if (!STATUS_SUBMITTED.equals(application.getApplicationStatus())) {
            throw new BusinessException(400, message);
        }
    }

    private List<Long> normalizeMaterialIds(List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return Collections.emptyList();
        }
        return materialIds.stream()
                .peek(materialId -> {
                    if (materialId == null || materialId <= 0) {
                        throw new BusinessException(400, "申请材料ID不合法");
                    }
                })
                .distinct()
                .toList();
    }

    private void validateMaterialsBelongToRecord(Long recordId, List<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return;
        }
        Long count = recordMaterialMapper.selectCount(new LambdaQueryWrapper<RecordMaterial>()
                .eq(RecordMaterial::getRecordId, recordId)
                .in(RecordMaterial::getId, materialIds));
        if (count == null || count != materialIds.size()) {
            throw new BusinessException(400, "申请材料必须属于当前考籍档案");
        }
    }

    private Map<String, String> buildExtensionData(CourseReplacementRule rule, String applyReason) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("ruleId", String.valueOf(rule.getId()));
        data.put("sourceCourseCode", rule.getSourceCourseCode());
        data.put("sourceCourseName", rule.getSourceCourseName());
        data.put("targetCourseCode", rule.getTargetCourseCode());
        data.put("targetCourseName", rule.getTargetCourseName());
        data.put("majorCode", rule.getMajorCode());
        data.put("educationLevel", rule.getEducationLevel());
        data.put("applyReason", applyReason);
        return data;
    }

    private void saveExtensionFields(Long applicationId, Map<String, String> extensionData) {
        extensionFieldMapper.delete(new LambdaQueryWrapper<ApplicationExtensionField>()
                .eq(ApplicationExtensionField::getApplicationId, applicationId));
        int sortOrder = 10;
        for (Map.Entry<String, String> entry : extensionData.entrySet()) {
            ApplicationExtensionField field = new ApplicationExtensionField();
            field.setApplicationId(applicationId);
            field.setBusinessType(BUSINESS_TYPE_COURSE_REPLACE);
            field.setFieldCode(entry.getKey());
            field.setFieldName(getExtensionFieldName(entry.getKey()));
            field.setFieldValue(entry.getValue());
            field.setValueType("ruleId".equals(entry.getKey()) ? "NUMBER" : "STRING");
            field.setRequiredFlag(isRequiredExtensionField(entry.getKey()) ? 1 : 0);
            field.setSortOrder(sortOrder);
            extensionFieldMapper.insert(field);
            sortOrder += 10;
        }
    }

    private String getExtensionFieldName(String fieldCode) {
        return switch (fieldCode) {
            case "ruleId" -> "课程顶替规则ID";
            case "sourceCourseCode" -> "原课程代码";
            case "sourceCourseName" -> "原课程名称";
            case "targetCourseCode" -> "顶替课程代码";
            case "targetCourseName" -> "顶替课程名称";
            case "majorCode" -> "适用专业代码";
            case "educationLevel" -> "适用学历层次";
            case "applyReason" -> "申请原因";
            default -> fieldCode;
        };
    }

    private boolean isRequiredExtensionField(String fieldCode) {
        return List.of("ruleId", "sourceCourseCode", "sourceCourseName", "targetCourseCode", "targetCourseName")
                .contains(fieldCode);
    }

    private String generateApplicationNo() {
        for (int index = 0; index < 5; index++) {
            String applicationNo = "DT" + LocalDate.now().format(APPLICATION_NO_DATE_FORMATTER)
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            Long count = businessApplicationMapper.selectCount(new LambdaQueryWrapper<BusinessApplication>()
                    .eq(BusinessApplication::getApplicationNo, applicationNo));
            if (count == null || count == 0) {
                return applicationNo;
            }
        }
        throw new BusinessException(500, "课程顶替申请编号生成失败");
    }

    private String writeMaterialIds(List<Long> materialIds) {
        try {
            return objectMapper.writeValueAsString(materialIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "申请材料序列化失败：" + exception.getMessage());
        }
    }

    private String writeExtensionData(Map<String, String> extensionData) {
        try {
            return objectMapper.writeValueAsString(extensionData);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "课程顶替扩展字段序列化失败：" + exception.getMessage());
        }
    }

    private List<Long> parseMaterialIds(BusinessApplication application) {
        if (!StringUtils.hasText(application.getMaterialIdsJson())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(application.getMaterialIdsJson(), MATERIAL_ID_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseExtensionData(BusinessApplication application) {
        if (!StringUtils.hasText(application.getExtensionDataJson())) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(application.getExtensionDataJson(), EXTENSION_DATA_TYPE);
        } catch (JsonProcessingException exception) {
            return Collections.emptyMap();
        }
    }

    private void saveFlowRecord(BusinessApplication application,
                                String beforeStatus,
                                String afterStatus,
                                String action,
                                String opinion) {
        TokenUserVO user = AuthContextHolder.getUser();
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setApplicationId(application.getId());
        auditRecord.setBusinessType(BUSINESS_TYPE_COURSE_REPLACE);
        auditRecord.setBusinessId(application.getId());
        auditRecord.setRecordId(application.getRecordId());
        auditRecord.setAuditAction(action);
        auditRecord.setBeforeStatus(beforeStatus);
        auditRecord.setAfterStatus(afterStatus);
        auditRecord.setAuditStatus(afterStatus);
        auditRecord.setAuditOpinion(opinion);
        auditRecord.setAuditorId(user == null ? null : user.getId());
        auditRecord.setAuditorName(user == null ? null : user.getRealName());
        auditRecord.setOperationTime(LocalDateTime.now());
        auditRecord.setCreateTime(LocalDateTime.now());
        auditRecordMapper.insert(auditRecord);
    }
}
