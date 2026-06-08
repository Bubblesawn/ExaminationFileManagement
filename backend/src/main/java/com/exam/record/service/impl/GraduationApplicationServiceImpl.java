package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.GraduationApplicationAuditDTO;
import com.exam.record.dto.GraduationApplicationSubmitDTO;
import com.exam.record.dto.GraduationApplicationUpdateDTO;
import com.exam.record.dto.GraduationApplicationWithdrawDTO;
import com.exam.record.entity.ApplicationExtensionField;
import com.exam.record.entity.AuditRecord;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.RecordStatusLog;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.ApplicationExtensionFieldMapper;
import com.exam.record.mapper.AuditRecordMapper;
import com.exam.record.mapper.BusinessApplicationMapper;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.RecordStatusLogMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.GraduationApplicationService;
import com.exam.record.service.RecordChangeLogService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.GraduationApplicationVO;
import com.exam.record.vo.GraduationEligibilityVO;
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
import java.util.ArrayList;
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
 * @brief 毕业申请业务实现。
 *
 * @details
 * 负责第四阶段 4.5 的毕业申请提交、资格校验、审核、驳回和结果查询。
 * 申请差异字段写入 business_application.extension_data_json 和
 * application_extension_field；审核通过后同步将考籍档案状态更新为 GRADUATED，
 * 并写入流程记录、档案状态记录和档案变更记录。
 */
@Service
public class GraduationApplicationServiceImpl extends ServiceImpl<BusinessApplicationMapper, BusinessApplication>
        implements GraduationApplicationService {
    private static final String BUSINESS_TYPE_GRADUATION = "GRADUATION";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AUDITING = "AUDITING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String ACTION_SUBMIT = "SUBMIT";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_WITHDRAW = "WITHDRAW";
    private static final String RECORD_STATUS_NORMAL = "NORMAL";
    private static final String RECORD_STATUS_GRADUATED = "GRADUATED";
    private static final String MATERIAL_TYPE_GRADUATION = "GRADUATION";
    private static final String MATERIAL_AUDIT_APPROVED = "APPROVED";
    private static final String CHANGE_TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String VALUE_TYPE_STRING = "STRING";
    private static final String VALUE_TYPE_JSON = "JSON";
    private static final DateTimeFormatter APPLICATION_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final TypeReference<List<Long>> MATERIAL_ID_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> EXTENSION_MAP_TYPE = new TypeReference<>() {
    };

    private final StudentRecordMapper studentRecordMapper;
    private final CandidateMapper candidateMapper;
    private final RecordMaterialMapper recordMaterialMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final ApplicationExtensionFieldMapper applicationExtensionFieldMapper;
    private final RecordStatusLogMapper recordStatusLogMapper;
    private final RecordChangeLogService recordChangeLogService;
    private final ObjectMapper objectMapper;

    /**
     * @brief 构造毕业申请业务实现。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param candidateMapper 考生信息 Mapper。
     * @param recordMaterialMapper 档案材料 Mapper。
     * @param auditRecordMapper 审核记录 Mapper。
     * @param applicationExtensionFieldMapper 申请扩展字段 Mapper。
     * @param recordStatusLogMapper 档案状态记录 Mapper。
     * @param recordChangeLogService 档案变更记录业务服务。
     * @param objectMapper JSON 序列化组件。
     */
    public GraduationApplicationServiceImpl(StudentRecordMapper studentRecordMapper,
                                            CandidateMapper candidateMapper,
                                            RecordMaterialMapper recordMaterialMapper,
                                            AuditRecordMapper auditRecordMapper,
                                            ApplicationExtensionFieldMapper applicationExtensionFieldMapper,
                                            RecordStatusLogMapper recordStatusLogMapper,
                                            RecordChangeLogService recordChangeLogService,
                                            ObjectMapper objectMapper) {
        this.studentRecordMapper = studentRecordMapper;
        this.candidateMapper = candidateMapper;
        this.recordMaterialMapper = recordMaterialMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.applicationExtensionFieldMapper = applicationExtensionFieldMapper;
        this.recordStatusLogMapper = recordStatusLogMapper;
        this.recordChangeLogService = recordChangeLogService;
        this.objectMapper = objectMapper;
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
    @Override
    public Page<GraduationApplicationVO> pageGraduationApplications(long pageNo,
                                                                    long pageSize,
                                                                    String keyword,
                                                                    String applicationStatus,
                                                                    Long recordId,
                                                                    Long candidateId) {
        validateOptionalApplicationStatus(applicationStatus);
        LambdaQueryWrapper<BusinessApplication> wrapper = buildApplicationQueryWrapper(
                keyword, applicationStatus, recordId, candidateId);
        Page<BusinessApplication> applicationPage = page(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, StudentRecord> recordMap = loadRecordMap(applicationPage.getRecords());
        Map<Long, Candidate> candidateMap = loadCandidateMap(applicationPage.getRecords());
        Page<GraduationApplicationVO> voPage = new Page<>(
                applicationPage.getCurrent(), applicationPage.getSize(), applicationPage.getTotal());
        voPage.setRecords(applicationPage.getRecords().stream()
                .map(application -> buildVO(application, recordMap, candidateMap))
                .toList());
        return voPage;
    }

    /**
     * @brief 查询毕业申请详情。
     *
     * @param id 毕业申请 ID。
     * @return 毕业申请详情。
     */
    @Override
    public GraduationApplicationVO getGraduationApplicationDetail(Long id) {
        BusinessApplication application = getExistingGraduationApplication(id);
        StudentRecord record = studentRecordMapper.selectById(application.getRecordId());
        Candidate candidate = candidateMapper.selectById(application.getCandidateId());
        return GraduationApplicationVO.fromEntity(
                application, record, candidate, parseMaterialIds(application), parseExtensionData(application));
    }

    /**
     * @brief 校验指定考籍档案是否具备毕业申请资格。
     *
     * @details
     * 校验项优先使用当前系统已具备的数据：考籍状态必须为 NORMAL、考生身份信息需完整、
     * 至少存在一份毕业申请材料，且毕业材料应全部审核通过。
     * 该方法不修改数据库，可被提交、修改和前端预检查接口复用。
     *
     * @param recordId 考籍档案 ID。
     * @return 毕业资格校验结果。
     */
    @Override
    public GraduationEligibilityVO checkGraduationEligibility(Long recordId) {
        StudentRecord record = getExistingRecord(recordId);
        Candidate candidate = candidateMapper.selectById(record.getCandidateId());
        List<RecordMaterial> graduationMaterials = recordMaterialMapper.selectList(new LambdaQueryWrapper<RecordMaterial>()
                .eq(RecordMaterial::getRecordId, recordId)
                .eq(RecordMaterial::getMaterialType, MATERIAL_TYPE_GRADUATION));
        List<String> passedItems = new ArrayList<>();
        List<String> failedItems = new ArrayList<>();
        List<String> warningItems = new ArrayList<>();
        if (RECORD_STATUS_NORMAL.equals(record.getRecordStatus())) {
            passedItems.add("考籍状态正常");
        } else {
            failedItems.add("考籍状态不是NORMAL，当前为" + safeText(record.getRecordStatus()));
        }
        if (candidate != null && StringUtils.hasText(candidate.getName()) && StringUtils.hasText(candidate.getIdCard())) {
            passedItems.add("考生身份信息完整");
        } else {
            failedItems.add("考生姓名或身份证号不完整");
        }
        if (graduationMaterials.isEmpty()) {
            failedItems.add("缺少毕业申请材料");
        } else if (graduationMaterials.stream()
                .allMatch(material -> MATERIAL_AUDIT_APPROVED.equals(material.getAuditStatus()))) {
            passedItems.add("毕业申请材料已审核通过");
        } else {
            failedItems.add("存在未审核通过的毕业申请材料");
        }
        if ("ARCHIVED".equals(record.getArchiveStatus())) {
            warningItems.add("档案已归档，请确认是否允许继续办理毕业申请");
        }
        GraduationEligibilityVO eligibility = new GraduationEligibilityVO();
        eligibility.setEligible(failedItems.isEmpty());
        eligibility.setPassedItems(passedItems);
        eligibility.setFailedItems(failedItems);
        eligibility.setWarningItems(warningItems);
        return eligibility;
    }

    /**
     * @brief 提交毕业申请。
     *
     * @details
     * 提交时校验考籍档案存在性、材料归属、毕业资格和同一档案毕业申请唯一性。
     * 申请保存为 SUBMITTED 状态，并记录一条 SUBMIT 流程记录。
     *
     * @param dto 毕业申请提交请求对象。
     * @return 已提交的毕业申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraduationApplicationVO submitGraduationApplication(GraduationApplicationSubmitDTO dto) {
        StudentRecord record = getExistingRecord(dto.getRecordId());
        validateNoActiveGraduationApplication(record.getId(), null);
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(record.getId(), materialIds);
        GraduationEligibilityVO eligibility = checkGraduationEligibility(record.getId());
        assertEligibilityPassed(eligibility);
        Map<String, String> extensionData = buildExtensionData(
                dto.getGraduationBatch(), dto.getDegreeApplyType(), dto.getApplyReason(), eligibility);
        TokenUserVO user = AuthContextHolder.getUser();
        BusinessApplication application = new BusinessApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setBusinessType(BUSINESS_TYPE_GRADUATION);
        application.setRecordId(record.getId());
        application.setCandidateId(record.getCandidateId());
        application.setApplicationTitle(buildApplicationTitle(record, dto.getGraduationBatch()));
        application.setApplicationStatus(STATUS_SUBMITTED);
        application.setCurrentNodeCode(STATUS_SUBMITTED);
        application.setCurrentNodeName("已提交");
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(extensionData));
        application.setApplyUserId(user == null ? null : user.getId());
        application.setApplyUserName(user == null ? null : user.getRealName());
        application.setSubmitTime(LocalDateTime.now());
        application.setRemark(dto.getRemark());
        save(application);
        saveExtensionFields(application, extensionData);
        saveFlowRecord(application, ACTION_SUBMIT, null, STATUS_SUBMITTED, "毕业申请已提交");
        return getGraduationApplicationDetail(application.getId());
    }

    /**
     * @brief 修改毕业申请。
     *
     * @details
     * 仅允许 SUBMITTED 状态修改毕业批次、学位申请类型、申请原因、材料和备注。
     * 修改时会重新执行资格校验并刷新扩展字段快照。
     *
     * @param id 毕业申请 ID。
     * @param dto 毕业申请修改请求对象。
     * @return 修改后的毕业申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraduationApplicationVO updateGraduationApplication(Long id, GraduationApplicationUpdateDTO dto) {
        BusinessApplication application = getExistingGraduationApplication(id);
        assertSubmittedStatus(application, "只有已提交的毕业申请可以修改");
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(application.getRecordId(), materialIds);
        GraduationEligibilityVO eligibility = checkGraduationEligibility(application.getRecordId());
        assertEligibilityPassed(eligibility);
        StudentRecord record = getExistingRecord(application.getRecordId());
        Map<String, String> extensionData = buildExtensionData(
                dto.getGraduationBatch(), dto.getDegreeApplyType(), dto.getApplyReason(), eligibility);
        application.setApplicationTitle(buildApplicationTitle(record, dto.getGraduationBatch()));
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(extensionData));
        application.setRemark(dto.getRemark());
        updateById(application);
        saveExtensionFields(application, extensionData);
        saveFlowRecord(application, ACTION_UPDATE, STATUS_SUBMITTED, STATUS_SUBMITTED, "毕业申请内容已修改");
        return getGraduationApplicationDetail(id);
    }

    /**
     * @brief 撤回毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的毕业申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraduationApplicationVO withdrawGraduationApplication(Long id, GraduationApplicationWithdrawDTO dto) {
        BusinessApplication application = getExistingGraduationApplication(id);
        assertSubmittedStatus(application, "只有已提交的毕业申请可以撤回");
        String beforeStatus = application.getApplicationStatus();
        application.setApplicationStatus(STATUS_WITHDRAWN);
        application.setCurrentNodeCode(STATUS_WITHDRAWN);
        application.setCurrentNodeName("已撤回");
        application.setWithdrawTime(LocalDateTime.now());
        application.setWithdrawReason(dto.getWithdrawReason());
        updateById(application);
        saveFlowRecord(application, ACTION_WITHDRAW, beforeStatus, STATUS_WITHDRAWN, dto.getWithdrawReason());
        return getGraduationApplicationDetail(id);
    }

    /**
     * @brief 审核通过毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的毕业申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraduationApplicationVO approveGraduationApplication(Long id, GraduationApplicationAuditDTO dto) {
        return auditGraduationApplication(id, dto, STATUS_APPROVED);
    }

    /**
     * @brief 驳回毕业申请。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的毕业申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraduationApplicationVO rejectGraduationApplication(Long id, GraduationApplicationAuditDTO dto) {
        return auditGraduationApplication(id, dto, STATUS_REJECTED);
    }

    /**
     * @brief 查询毕业申请结果。
     *
     * @param id 毕业申请 ID。
     * @return 毕业申请结果。
     */
    @Override
    public GraduationApplicationVO getGraduationApplicationResult(Long id) {
        BusinessApplication application = getExistingGraduationApplication(id);
        if (!STATUS_APPROVED.equals(application.getApplicationStatus())
                && !STATUS_REJECTED.equals(application.getApplicationStatus())) {
            throw new BusinessException(400, "毕业申请尚未形成审核结果");
        }
        return getGraduationApplicationDetail(id);
    }

    /**
     * @brief 查询毕业申请流程记录。
     *
     * @param id 毕业申请 ID。
     * @return 流程记录列表。
     */
    @Override
    public List<AuditRecordVO> listFlowRecords(Long id) {
        getExistingGraduationApplication(id);
        return auditRecordMapper.selectList(new LambdaQueryWrapper<AuditRecord>()
                        .eq(AuditRecord::getBusinessType, BUSINESS_TYPE_GRADUATION)
                        .eq(AuditRecord::getBusinessId, id)
                        .orderByAsc(AuditRecord::getOperationTime)
                        .orderByAsc(AuditRecord::getId))
                .stream()
                .map(AuditRecordVO::fromEntity)
                .toList();
    }

    /**
     * @brief 执行毕业申请审核状态流转。
     *
     * @details
     * 审核入口只接受 SUBMITTED 状态的毕业申请，目标状态限定为 APPROVED 或 REJECTED。
     * 审核通过前会重新执行毕业资格校验，避免申请提交后档案或材料状态发生变化。
     * 通过后将考籍档案状态同步为 GRADUATED，并写入状态日志和档案变更记录。
     *
     * @param id 毕业申请 ID。
     * @param dto 审核请求对象。
     * @param targetStatus 目标状态。
     * @return 审核后的毕业申请。
     */
    private GraduationApplicationVO auditGraduationApplication(Long id,
                                                              GraduationApplicationAuditDTO dto,
                                                              String targetStatus) {
        if (!STATUS_APPROVED.equals(targetStatus) && !STATUS_REJECTED.equals(targetStatus)) {
            throw new BusinessException(400, "毕业审核状态只能为APPROVED或REJECTED");
        }
        BusinessApplication application = getExistingGraduationApplication(id);
        assertSubmittedStatus(application, "只有已提交的毕业申请可以审核");
        if (STATUS_APPROVED.equals(targetStatus)) {
            assertEligibilityPassed(checkGraduationEligibility(application.getRecordId()));
        }
        String beforeStatus = application.getApplicationStatus();
        TokenUserVO user = AuthContextHolder.getUser();
        application.setApplicationStatus(targetStatus);
        application.setCurrentNodeCode(targetStatus);
        application.setCurrentNodeName(STATUS_APPROVED.equals(targetStatus) ? "审核通过" : "审核驳回");
        application.setAuditUserId(user == null ? null : user.getId());
        application.setAuditUserName(user == null ? null : user.getRealName());
        application.setAuditTime(LocalDateTime.now());
        application.setAuditOpinion(dto.getAuditOpinion());
        updateById(application);
        if (STATUS_APPROVED.equals(targetStatus)) {
            markRecordGraduated(application.getRecordId(), dto.getAuditOpinion());
        }
        saveFlowRecord(application,
                STATUS_APPROVED.equals(targetStatus) ? ACTION_APPROVE : ACTION_REJECT,
                beforeStatus,
                targetStatus,
                dto.getAuditOpinion());
        return getGraduationApplicationDetail(id);
    }

    private LambdaQueryWrapper<BusinessApplication> buildApplicationQueryWrapper(String keyword,
                                                                                String applicationStatus,
                                                                                Long recordId,
                                                                                Long candidateId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_GRADUATION);
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
        return studentRecordMapper.selectList(wrapper)
                .stream()
                .map(StudentRecord::getId)
                .collect(Collectors.toSet());
    }

    private Map<Long, StudentRecord> loadRecordMap(List<BusinessApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> recordIds = applications.stream()
                .map(BusinessApplication::getRecordId)
                .collect(Collectors.toSet());
        return studentRecordMapper.selectList(new LambdaQueryWrapper<StudentRecord>().in(StudentRecord::getId, recordIds))
                .stream()
                .collect(Collectors.toMap(StudentRecord::getId, Function.identity()));
    }

    private Map<Long, Candidate> loadCandidateMap(List<BusinessApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> candidateIds = applications.stream()
                .map(BusinessApplication::getCandidateId)
                .collect(Collectors.toSet());
        return candidateMapper.selectList(new LambdaQueryWrapper<Candidate>().in(Candidate::getId, candidateIds))
                .stream()
                .collect(Collectors.toMap(Candidate::getId, Function.identity()));
    }

    private GraduationApplicationVO buildVO(BusinessApplication application,
                                            Map<Long, StudentRecord> recordMap,
                                            Map<Long, Candidate> candidateMap) {
        return GraduationApplicationVO.fromEntity(
                application,
                recordMap.get(application.getRecordId()),
                candidateMap.get(application.getCandidateId()),
                parseMaterialIds(application),
                parseExtensionData(application));
    }

    private BusinessApplication getExistingGraduationApplication(Long id) {
        BusinessApplication application = getById(id);
        if (application == null || !BUSINESS_TYPE_GRADUATION.equals(application.getBusinessType())) {
            throw new BusinessException(404, "毕业申请不存在");
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

    private void validateNoActiveGraduationApplication(Long recordId, Long excludeId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_GRADUATION)
                .eq(BusinessApplication::getRecordId, recordId)
                .in(BusinessApplication::getApplicationStatus, List.of(STATUS_SUBMITTED, STATUS_APPROVED));
        if (excludeId != null) {
            wrapper.ne(BusinessApplication::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "该档案已有在办或已通过毕业申请");
        }
    }

    private void assertEligibilityPassed(GraduationEligibilityVO eligibility) {
        if (!Boolean.TRUE.equals(eligibility.getEligible())) {
            throw new BusinessException(400, "毕业资格校验未通过：" + String.join("；", eligibility.getFailedItems()));
        }
    }

    private void assertSubmittedStatus(BusinessApplication application, String message) {
        if (!STATUS_SUBMITTED.equals(application.getApplicationStatus())) {
            throw new BusinessException(400, message);
        }
    }

    private void validateOptionalApplicationStatus(String status) {
        if (StringUtils.hasText(status) && !STATUS_DRAFT.equals(status)
                && !STATUS_SUBMITTED.equals(status)
                && !STATUS_AUDITING.equals(status)
                && !STATUS_APPROVED.equals(status)
                && !STATUS_REJECTED.equals(status)
                && !STATUS_WITHDRAWN.equals(status)) {
            throw new BusinessException(400, "毕业申请状态只能为DRAFT、SUBMITTED、AUDITING、APPROVED、REJECTED或WITHDRAWN");
        }
    }

    private String generateApplicationNo() {
        for (int index = 0; index < 5; index++) {
            String applicationNo = "BY" + LocalDate.now().format(APPLICATION_NO_DATE_FORMATTER)
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            Long count = count(new LambdaQueryWrapper<BusinessApplication>()
                    .eq(BusinessApplication::getApplicationNo, applicationNo));
            if (count == null || count == 0) {
                return applicationNo;
            }
        }
        throw new BusinessException(500, "毕业申请编号生成失败");
    }

    private String buildApplicationTitle(StudentRecord record, String graduationBatch) {
        String recordNo = record == null ? "" : safeText(record.getRecordNo());
        return safeText(graduationBatch) + "毕业申请：" + recordNo;
    }

    private Map<String, String> buildExtensionData(String graduationBatch,
                                                   String degreeApplyType,
                                                   String applyReason,
                                                   GraduationEligibilityVO eligibility) {
        Map<String, String> extensionData = new LinkedHashMap<>();
        extensionData.put("graduationBatch", safeText(graduationBatch));
        extensionData.put("degreeApplyType", safeText(degreeApplyType));
        extensionData.put("applyReason", safeText(applyReason));
        extensionData.put("eligibilityPassed", String.valueOf(Boolean.TRUE.equals(eligibility.getEligible())));
        extensionData.put("eligibilitySummary", buildEligibilitySummary(eligibility));
        extensionData.put("eligibilityJson", writeEligibility(eligibility));
        return extensionData;
    }

    private String buildEligibilitySummary(GraduationEligibilityVO eligibility) {
        if (Boolean.TRUE.equals(eligibility.getEligible())) {
            return "资格校验通过：" + String.join("；", eligibility.getPassedItems());
        }
        return "资格校验未通过：" + String.join("；", eligibility.getFailedItems());
    }

    private String writeEligibility(GraduationEligibilityVO eligibility) {
        try {
            return objectMapper.writeValueAsString(eligibility);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "毕业资格校验结果序列化失败：" + exception.getMessage());
        }
    }

    private String writeMaterialIds(List<Long> materialIds) {
        try {
            return objectMapper.writeValueAsString(materialIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "申请材料序列化失败：" + exception.getMessage());
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

    private String writeExtensionData(Map<String, String> extensionData) {
        try {
            return objectMapper.writeValueAsString(extensionData);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "毕业申请扩展字段序列化失败：" + exception.getMessage());
        }
    }

    private Map<String, String> parseExtensionData(BusinessApplication application) {
        if (!StringUtils.hasText(application.getExtensionDataJson())) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(application.getExtensionDataJson(), EXTENSION_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            return Collections.emptyMap();
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * @brief 保存毕业申请扩展字段明细。
     *
     * @details
     * 与 extensionDataJson 快照保持一致，便于毕业详情页按固定字段展示，
     * 也方便后续统计资格校验通过率和按毕业批次筛选申请。
     *
     * @param application 毕业申请实体。
     * @param extensionData 扩展字段数据。
     */
    private void saveExtensionFields(BusinessApplication application, Map<String, String> extensionData) {
        upsertExtensionField(application, "graduationBatch", "申请毕业批次", extensionData.get("graduationBatch"), VALUE_TYPE_STRING, true, 10);
        upsertExtensionField(application, "degreeApplyType", "学位申请类型", extensionData.get("degreeApplyType"), VALUE_TYPE_STRING, false, 20);
        upsertExtensionField(application, "applyReason", "毕业申请原因", extensionData.get("applyReason"), VALUE_TYPE_STRING, true, 30);
        upsertExtensionField(application, "eligibilityPassed", "资格校验是否通过", extensionData.get("eligibilityPassed"), VALUE_TYPE_STRING, true, 40);
        upsertExtensionField(application, "eligibilitySummary", "资格校验摘要", extensionData.get("eligibilitySummary"), VALUE_TYPE_STRING, true, 50);
        upsertExtensionField(application, "eligibilityJson", "资格校验明细", extensionData.get("eligibilityJson"), VALUE_TYPE_JSON, true, 60);
    }

    private void upsertExtensionField(BusinessApplication application,
                                      String fieldCode,
                                      String fieldName,
                                      String fieldValue,
                                      String valueType,
                                      boolean required,
                                      int sortOrder) {
        ApplicationExtensionField extensionField = applicationExtensionFieldMapper.selectOne(
                new LambdaQueryWrapper<ApplicationExtensionField>()
                        .eq(ApplicationExtensionField::getApplicationId, application.getId())
                        .eq(ApplicationExtensionField::getFieldCode, fieldCode));
        if (extensionField == null) {
            extensionField = new ApplicationExtensionField();
            extensionField.setApplicationId(application.getId());
            extensionField.setBusinessType(application.getBusinessType());
            extensionField.setFieldCode(fieldCode);
        }
        extensionField.setFieldName(fieldName);
        extensionField.setFieldValue(fieldValue);
        extensionField.setValueType(valueType);
        extensionField.setRequiredFlag(required ? 1 : 0);
        extensionField.setSortOrder(sortOrder);
        if (extensionField.getId() == null) {
            applicationExtensionFieldMapper.insert(extensionField);
        } else {
            applicationExtensionFieldMapper.updateById(extensionField);
        }
    }

    private void markRecordGraduated(Long recordId, String changeReason) {
        StudentRecord record = getExistingRecord(recordId);
        String beforeStatus = record.getRecordStatus();
        if (RECORD_STATUS_GRADUATED.equals(beforeStatus)) {
            return;
        }
        record.setRecordStatus(RECORD_STATUS_GRADUATED);
        studentRecordMapper.updateById(record);
        TokenUserVO user = AuthContextHolder.getUser();
        RecordStatusLog statusLog = new RecordStatusLog();
        statusLog.setRecordId(recordId);
        statusLog.setBeforeStatus(beforeStatus);
        statusLog.setAfterStatus(RECORD_STATUS_GRADUATED);
        statusLog.setChangeReason(changeReason);
        statusLog.setOperatorId(user == null ? null : user.getId());
        statusLog.setOperatorName(user == null ? null : user.getRealName());
        statusLog.setOperationTime(LocalDateTime.now());
        recordStatusLogMapper.insert(statusLog);
        recordChangeLogService.recordChange(
                recordId,
                CHANGE_TYPE_STATUS_CHANGE,
                "recordStatus",
                beforeStatus,
                RECORD_STATUS_GRADUATED,
                changeReason);
    }

    private void saveFlowRecord(BusinessApplication application,
                                String action,
                                String beforeStatus,
                                String afterStatus,
                                String opinion) {
        TokenUserVO user = AuthContextHolder.getUser();
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setApplicationId(application.getId());
        auditRecord.setBusinessType(BUSINESS_TYPE_GRADUATION);
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
