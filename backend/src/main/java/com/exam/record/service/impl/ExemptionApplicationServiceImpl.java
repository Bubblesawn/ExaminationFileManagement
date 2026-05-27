package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.ExemptionApplicationAuditDTO;
import com.exam.record.dto.ExemptionApplicationSubmitDTO;
import com.exam.record.dto.ExemptionApplicationUpdateDTO;
import com.exam.record.dto.ExemptionApplicationWithdrawDTO;
import com.exam.record.entity.ApplicationExtensionField;
import com.exam.record.entity.AuditRecord;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.ApplicationExtensionFieldMapper;
import com.exam.record.mapper.AuditRecordMapper;
import com.exam.record.mapper.BusinessApplicationMapper;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.ExemptionApplicationService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.ExemptionApplicationVO;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @brief 免考申请业务实现。
 *
 * @details
 * 负责第四阶段 4.2 的免考申请提交、修改、撤回、审核通过、审核驳回和查询。
 * 状态流转统一限制在 SUBMITTED、APPROVED、REJECTED、WITHDRAWN 等流程状态内，
 * 并将每一次关键操作写入 audit_record 表作为流程记录。
 */
@Service
public class ExemptionApplicationServiceImpl extends ServiceImpl<BusinessApplicationMapper, BusinessApplication>
        implements ExemptionApplicationService {
    private static final String BUSINESS_TYPE_EXEMPTION = "EXEMPTION";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AUDITING = "AUDITING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String AUDIT_ACTION_SUBMIT = "SUBMIT";
    private static final String AUDIT_ACTION_UPDATE = "UPDATE";
    private static final String AUDIT_ACTION_APPROVE = "APPROVE";
    private static final String AUDIT_ACTION_REJECT = "REJECT";
    private static final String AUDIT_ACTION_WITHDRAW = "WITHDRAW";
    private static final String FIELD_COURSE_CODE = "courseCode";
    private static final String FIELD_COURSE_NAME = "courseName";
    private static final String FIELD_SOURCE_COURSE_CODE = "sourceCourseCode";
    private static final String FIELD_SOURCE_COURSE_NAME = "sourceCourseName";
    private static final String FIELD_EXEMPTION_REASON = "exemptionReason";
    private static final String VALUE_TYPE_STRING = "STRING";
    private static final DateTimeFormatter APPLICATION_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final TypeReference<List<Long>> MATERIAL_ID_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> EXTENSION_DATA_TYPE = new TypeReference<>() {
    };

    private final StudentRecordMapper studentRecordMapper;
    private final CandidateMapper candidateMapper;
    private final RecordMaterialMapper recordMaterialMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final ApplicationExtensionFieldMapper applicationExtensionFieldMapper;
    private final ObjectMapper objectMapper;

    /**
     * @brief 构造免考申请业务实现。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param candidateMapper 考生信息 Mapper。
     * @param recordMaterialMapper 档案材料 Mapper。
     * @param auditRecordMapper 审核记录 Mapper。
     * @param applicationExtensionFieldMapper 申请扩展字段 Mapper。
     * @param objectMapper JSON 序列化组件。
     */
    public ExemptionApplicationServiceImpl(StudentRecordMapper studentRecordMapper,
                                           CandidateMapper candidateMapper,
                                           RecordMaterialMapper recordMaterialMapper,
                                           AuditRecordMapper auditRecordMapper,
                                           ApplicationExtensionFieldMapper applicationExtensionFieldMapper,
                                           ObjectMapper objectMapper) {
        this.studentRecordMapper = studentRecordMapper;
        this.candidateMapper = candidateMapper;
        this.recordMaterialMapper = recordMaterialMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.applicationExtensionFieldMapper = applicationExtensionFieldMapper;
        this.objectMapper = objectMapper;
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
    @Override
    public Page<ExemptionApplicationVO> pageExemptionApplications(long pageNo,
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
        Page<ExemptionApplicationVO> voPage = new Page<>(
                applicationPage.getCurrent(), applicationPage.getSize(), applicationPage.getTotal());
        voPage.setRecords(applicationPage.getRecords().stream()
                .map(application -> buildVO(application, recordMap, candidateMap))
                .toList());
        return voPage;
    }

    /**
     * @brief 查询免考申请详情。
     *
     * @param id 免考申请 ID。
     * @return 免考申请详情。
     */
    @Override
    public ExemptionApplicationVO getExemptionApplicationDetail(Long id) {
        BusinessApplication application = getExistingExemptionApplication(id);
        StudentRecord record = studentRecordMapper.selectById(application.getRecordId());
        Candidate candidate = candidateMapper.selectById(application.getCandidateId());
        return ExemptionApplicationVO.fromEntity(
                application,
                record,
                candidate,
                parseMaterialIds(application),
                parseExtensionData(application));
    }

    /**
     * @brief 提交免考申请。
     *
     * @details
     * 提交时校验考籍档案存在性、材料归属和同一档案同一课程的在办申请唯一性，
     * 成功后将申请置为 SUBMITTED，并写入一条流程记录。
     *
     * @param dto 免考申请提交请求对象。
     * @return 已提交的免考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExemptionApplicationVO submitExemptionApplication(ExemptionApplicationSubmitDTO dto) {
        StudentRecord record = getExistingRecord(dto.getRecordId());
        validateNoActiveCourseApplication(dto.getRecordId(), dto.getCourseCode(), null);
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(dto.getRecordId(), materialIds);
        Map<String, String> extensionData = buildExtensionData(
                dto.getCourseCode(),
                dto.getCourseName(),
                dto.getSourceCourseCode(),
                dto.getSourceCourseName(),
                dto.getExemptionReason());
        TokenUserVO user = AuthContextHolder.getUser();
        BusinessApplication application = new BusinessApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setBusinessType(BUSINESS_TYPE_EXEMPTION);
        application.setRecordId(record.getId());
        application.setCandidateId(record.getCandidateId());
        application.setApplicationTitle(buildApplicationTitle(dto.getCourseName()));
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
        saveFlowRecord(application.getId(), null, STATUS_SUBMITTED, AUDIT_ACTION_SUBMIT, "免考申请已提交");
        return getExemptionApplicationDetail(application.getId());
    }

    /**
     * @brief 修改免考申请。
     *
     * @details
     * 仅允许处于 SUBMITTED 状态的免考申请修改业务字段；已通过、已驳回或已撤回的
     * 申请属于终态，不能再变更申请内容。
     *
     * @param id 免考申请 ID。
     * @param dto 免考申请修改请求对象。
     * @return 修改后的免考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExemptionApplicationVO updateExemptionApplication(Long id, ExemptionApplicationUpdateDTO dto) {
        BusinessApplication application = getExistingExemptionApplication(id);
        assertSubmittedStatus(application, "只有已提交的免考申请可以修改");
        validateNoActiveCourseApplication(application.getRecordId(), dto.getCourseCode(), id);
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(application.getRecordId(), materialIds);
        Map<String, String> extensionData = buildExtensionData(
                dto.getCourseCode(),
                dto.getCourseName(),
                dto.getSourceCourseCode(),
                dto.getSourceCourseName(),
                dto.getExemptionReason());
        application.setApplicationTitle(buildApplicationTitle(dto.getCourseName()));
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(extensionData));
        application.setRemark(dto.getRemark());
        updateById(application);
        saveExtensionFields(application, extensionData);
        saveFlowRecord(id, STATUS_SUBMITTED, STATUS_SUBMITTED, AUDIT_ACTION_UPDATE, "免考申请内容已修改");
        return getExemptionApplicationDetail(id);
    }

    /**
     * @brief 撤回免考申请。
     *
     * @details
     * 只有 SUBMITTED 状态可以撤回，撤回后状态进入 WITHDRAWN 终态并保留撤回原因。
     *
     * @param id 免考申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的免考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExemptionApplicationVO withdrawExemptionApplication(Long id, ExemptionApplicationWithdrawDTO dto) {
        BusinessApplication application = getExistingExemptionApplication(id);
        assertSubmittedStatus(application, "只有已提交的免考申请可以撤回");
        String beforeStatus = application.getApplicationStatus();
        application.setApplicationStatus(STATUS_WITHDRAWN);
        application.setCurrentNodeCode(STATUS_WITHDRAWN);
        application.setCurrentNodeName("已撤回");
        application.setWithdrawTime(LocalDateTime.now());
        application.setWithdrawReason(dto.getWithdrawReason());
        updateById(application);
        saveFlowRecord(id, beforeStatus, STATUS_WITHDRAWN, AUDIT_ACTION_WITHDRAW, dto.getWithdrawReason());
        return getExemptionApplicationDetail(id);
    }

    /**
     * @brief 审核通过免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的免考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExemptionApplicationVO approveExemptionApplication(Long id, ExemptionApplicationAuditDTO dto) {
        return auditExemptionApplication(id, dto, STATUS_APPROVED);
    }

    /**
     * @brief 驳回免考申请。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的免考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExemptionApplicationVO rejectExemptionApplication(Long id, ExemptionApplicationAuditDTO dto) {
        return auditExemptionApplication(id, dto, STATUS_REJECTED);
    }

    /**
     * @brief 查询免考申请流程记录。
     *
     * @param id 免考申请 ID。
     * @return 流程记录列表。
     */
    @Override
    public List<AuditRecordVO> listFlowRecords(Long id) {
        getExistingExemptionApplication(id);
        return auditRecordMapper.selectList(new LambdaQueryWrapper<AuditRecord>()
                        .eq(AuditRecord::getBusinessType, BUSINESS_TYPE_EXEMPTION)
                        .eq(AuditRecord::getBusinessId, id)
                        .orderByAsc(AuditRecord::getCreateTime)
                        .orderByAsc(AuditRecord::getId))
                .stream()
                .map(AuditRecordVO::fromEntity)
                .toList();
    }

    /**
     * @brief 执行免考审核状态流转。
     *
     * @details
     * 审核入口只接受 SUBMITTED 状态的申请，目标状态限定为 APPROVED 或 REJECTED。
     * 状态落库后同步写入 audit_record，保证审核动作可追溯。
     *
     * @param id 免考申请 ID。
     * @param dto 审核请求对象。
     * @param targetStatus 目标状态。
     * @return 审核后的免考申请。
     */
    private ExemptionApplicationVO auditExemptionApplication(Long id,
                                                            ExemptionApplicationAuditDTO dto,
                                                            String targetStatus) {
        if (!STATUS_APPROVED.equals(targetStatus) && !STATUS_REJECTED.equals(targetStatus)) {
            throw new BusinessException(400, "免考审核状态只能为APPROVED或REJECTED");
        }
        BusinessApplication application = getExistingExemptionApplication(id);
        assertSubmittedStatus(application, "只有已提交的免考申请可以审核");
        TokenUserVO user = AuthContextHolder.getUser();
        String beforeStatus = application.getApplicationStatus();
        application.setApplicationStatus(targetStatus);
        application.setCurrentNodeCode(targetStatus);
        application.setCurrentNodeName(STATUS_APPROVED.equals(targetStatus) ? "审核通过" : "审核驳回");
        application.setAuditUserId(user == null ? null : user.getId());
        application.setAuditUserName(user == null ? null : user.getRealName());
        application.setAuditTime(LocalDateTime.now());
        application.setAuditOpinion(dto.getAuditOpinion());
        updateById(application);
        saveFlowRecord(
                id,
                beforeStatus,
                targetStatus,
                STATUS_APPROVED.equals(targetStatus) ? AUDIT_ACTION_APPROVE : AUDIT_ACTION_REJECT,
                dto.getAuditOpinion());
        return getExemptionApplicationDetail(id);
    }

    private LambdaQueryWrapper<BusinessApplication> buildApplicationQueryWrapper(String keyword,
                                                                                String applicationStatus,
                                                                                Long recordId,
                                                                                Long candidateId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_EXEMPTION);
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

    private ExemptionApplicationVO buildVO(BusinessApplication application,
                                           Map<Long, StudentRecord> recordMap,
                                           Map<Long, Candidate> candidateMap) {
        fillExemptionFieldsFromSnapshot(application);
        return ExemptionApplicationVO.fromEntity(
                application,
                recordMap.get(application.getRecordId()),
                candidateMap.get(application.getCandidateId()),
                parseMaterialIds(application),
                parseExtensionData(application));
    }

    private BusinessApplication getExistingExemptionApplication(Long id) {
        BusinessApplication application = getById(id);
        if (application == null || !BUSINESS_TYPE_EXEMPTION.equals(application.getBusinessType())) {
            throw new BusinessException(404, "免考申请不存在");
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

    private void validateNoActiveCourseApplication(Long recordId, String courseCode, Long excludeId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_EXEMPTION)
                .eq(BusinessApplication::getRecordId, recordId)
                .in(BusinessApplication::getApplicationStatus, List.of(STATUS_SUBMITTED, STATUS_APPROVED));
        if (excludeId != null) {
            wrapper.ne(BusinessApplication::getId, excludeId);
        }
        boolean exists = list(wrapper).stream()
                .map(this::parseExtensionData)
                .anyMatch(extensionData -> courseCode.equals(extensionData.get(FIELD_COURSE_CODE)));
        if (exists) {
            throw new BusinessException(409, "该档案的当前课程已有在办或已通过免考申请");
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
            throw new BusinessException(400, "免考申请状态只能为DRAFT、SUBMITTED、AUDITING、APPROVED、REJECTED或WITHDRAWN");
        }
    }

    private String generateApplicationNo() {
        for (int index = 0; index < 5; index++) {
            String applicationNo = "MK" + LocalDate.now().format(APPLICATION_NO_DATE_FORMATTER)
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            Long count = count(new LambdaQueryWrapper<BusinessApplication>()
                    .eq(BusinessApplication::getApplicationNo, applicationNo));
            if (count == null || count == 0) {
                return applicationNo;
            }
        }
        throw new BusinessException(500, "免考申请编号生成失败");
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

    private Map<String, String> buildExtensionData(String courseCode,
                                                   String courseName,
                                                   String sourceCourseCode,
                                                   String sourceCourseName,
                                                   String exemptionReason) {
        Map<String, String> extensionData = new LinkedHashMap<>();
        extensionData.put(FIELD_COURSE_CODE, courseCode);
        extensionData.put(FIELD_COURSE_NAME, courseName);
        extensionData.put(FIELD_SOURCE_COURSE_CODE, sourceCourseCode);
        extensionData.put(FIELD_SOURCE_COURSE_NAME, sourceCourseName);
        extensionData.put(FIELD_EXEMPTION_REASON, exemptionReason);
        return extensionData;
    }

    private String writeExtensionData(Map<String, String> extensionData) {
        try {
            return objectMapper.writeValueAsString(extensionData);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "免考申请扩展字段序列化失败：" + exception.getMessage());
        }
    }

    private void fillExemptionFieldsFromSnapshot(BusinessApplication application) {
        Map<String, String> extensionData = parseExtensionData(application);
        application.setCourseCode(extensionData.get(FIELD_COURSE_CODE));
        application.setCourseName(extensionData.get(FIELD_COURSE_NAME));
        application.setSourceCourseCode(extensionData.get(FIELD_SOURCE_COURSE_CODE));
        application.setSourceCourseName(extensionData.get(FIELD_SOURCE_COURSE_NAME));
        application.setExemptionReason(extensionData.get(FIELD_EXEMPTION_REASON));
    }

    private String buildApplicationTitle(String courseName) {
        return StringUtils.hasText(courseName) ? "免考" + courseName + "申请" : "免考申请";
    }

    /**
     * @brief 保存免考申请扩展字段明细。
     *
     * @details
     * 该方法与 extensionDataJson 快照保持一致，后续详情页可按字段顺序展示，
     * 统计或审核规则也可直接按 field_code 查询对应业务字段。
     *
     * @param application 免考申请实体。
     */
    private void saveExtensionFields(BusinessApplication application, Map<String, String> extensionData) {
        upsertExtensionField(application, FIELD_COURSE_CODE, "免考课程代码", extensionData.get(FIELD_COURSE_CODE), true, 10);
        upsertExtensionField(application, FIELD_COURSE_NAME, "免考课程名称", extensionData.get(FIELD_COURSE_NAME), true, 20);
        upsertExtensionField(application, FIELD_SOURCE_COURSE_CODE, "证明来源课程代码", extensionData.get(FIELD_SOURCE_COURSE_CODE), false, 30);
        upsertExtensionField(application, FIELD_SOURCE_COURSE_NAME, "证明来源课程名称", extensionData.get(FIELD_SOURCE_COURSE_NAME), false, 40);
        upsertExtensionField(application, FIELD_EXEMPTION_REASON, "免考原因", extensionData.get(FIELD_EXEMPTION_REASON), true, 50);
    }

    private void upsertExtensionField(BusinessApplication application,
                                      String fieldCode,
                                      String fieldName,
                                      String fieldValue,
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
        extensionField.setValueType(VALUE_TYPE_STRING);
        extensionField.setRequiredFlag(required ? 1 : 0);
        extensionField.setSortOrder(sortOrder);
        if (extensionField.getId() == null) {
            applicationExtensionFieldMapper.insert(extensionField);
        } else {
            applicationExtensionFieldMapper.updateById(extensionField);
        }
    }

    private void saveFlowRecord(Long applicationId,
                                String beforeStatus,
                                String afterStatus,
                                String auditAction,
                                String opinion) {
        TokenUserVO user = AuthContextHolder.getUser();
        BusinessApplication application = getById(applicationId);
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setApplicationId(applicationId);
        auditRecord.setBusinessType(BUSINESS_TYPE_EXEMPTION);
        auditRecord.setBusinessId(applicationId);
        auditRecord.setRecordId(application == null ? null : application.getRecordId());
        auditRecord.setAuditAction(auditAction);
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
