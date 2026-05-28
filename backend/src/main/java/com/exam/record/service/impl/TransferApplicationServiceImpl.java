package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.TransferApplicationAuditDTO;
import com.exam.record.dto.TransferApplicationSubmitDTO;
import com.exam.record.dto.TransferApplicationUpdateDTO;
import com.exam.record.dto.TransferApplicationWithdrawDTO;
import com.exam.record.entity.AuditRecord;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.AuditRecordMapper;
import com.exam.record.mapper.BusinessApplicationMapper;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.RecordStatusLinkageService;
import com.exam.record.service.TransferApplicationService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.AuditRecordVO;
import com.exam.record.vo.TokenUserVO;
import com.exam.record.vo.TransferApplicationVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @brief 考籍转入转出申请业务实现。
 *
 * @details
 * 负责第四阶段 4.4 的考籍转入、考籍转出申请提交、修改、撤回、审核通过、审核驳回和
 * 查询。转考差异字段统一写入 business_application.extension_data_json，关键流程动作
 * 写入 audit_record；转出审核通过后同步将考籍档案状态更新为 TRANSFERRED_OUT。
 */
@Service
public class TransferApplicationServiceImpl extends ServiceImpl<BusinessApplicationMapper, BusinessApplication>
        implements TransferApplicationService {
    private static final String BUSINESS_TYPE_TRANSFER_IN = "TRANSFER_IN";
    private static final String BUSINESS_TYPE_TRANSFER_OUT = "TRANSFER_OUT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_UPDATED = "UPDATED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String ACTION_SUBMIT = "SUBMIT";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_WITHDRAW = "WITHDRAW";
    private static final String RECORD_STATUS_TRANSFERRED_OUT = "TRANSFERRED_OUT";
    private static final DateTimeFormatter APPLICATION_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final TypeReference<List<Long>> MATERIAL_ID_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> EXTENSION_MAP_TYPE = new TypeReference<>() {
    };

    private final StudentRecordMapper studentRecordMapper;
    private final CandidateMapper candidateMapper;
    private final RecordMaterialMapper recordMaterialMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final RecordStatusLinkageService recordStatusLinkageService;
    private final ObjectMapper objectMapper;

    /**
     * @brief 构造考籍转入转出申请业务实现。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param candidateMapper 考生信息 Mapper。
     * @param recordMaterialMapper 档案材料 Mapper。
     * @param auditRecordMapper 审核记录 Mapper。
     * @param recordStatusLinkageService 档案状态联动服务。
     * @param objectMapper JSON 序列化组件。
     */
    public TransferApplicationServiceImpl(StudentRecordMapper studentRecordMapper,
                                          CandidateMapper candidateMapper,
                                          RecordMaterialMapper recordMaterialMapper,
                                          AuditRecordMapper auditRecordMapper,
                                          RecordStatusLinkageService recordStatusLinkageService,
                                          ObjectMapper objectMapper) {
        this.studentRecordMapper = studentRecordMapper;
        this.candidateMapper = candidateMapper;
        this.recordMaterialMapper = recordMaterialMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.recordStatusLinkageService = recordStatusLinkageService;
        this.objectMapper = objectMapper;
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
    @Override
    public Page<TransferApplicationVO> pageTransferApplications(long pageNo,
                                                                long pageSize,
                                                                String keyword,
                                                                String transferType,
                                                                String applicationStatus,
                                                                Long recordId,
                                                                Long candidateId) {
        validateOptionalTransferType(transferType);
        validateOptionalApplicationStatus(applicationStatus);
        LambdaQueryWrapper<BusinessApplication> wrapper = buildApplicationQueryWrapper(
                keyword, transferType, applicationStatus, recordId, candidateId);
        Page<BusinessApplication> applicationPage = page(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, StudentRecord> recordMap = loadRecordMap(applicationPage.getRecords());
        Map<Long, Candidate> candidateMap = loadCandidateMap(applicationPage.getRecords());
        Page<TransferApplicationVO> voPage = new Page<>(
                applicationPage.getCurrent(), applicationPage.getSize(), applicationPage.getTotal());
        voPage.setRecords(applicationPage.getRecords().stream()
                .map(application -> buildVO(application, recordMap, candidateMap))
                .toList());
        return voPage;
    }

    /**
     * @brief 查询考籍转入转出申请详情。
     *
     * @param id 转考申请 ID。
     * @return 转考申请详情。
     */
    @Override
    public TransferApplicationVO getTransferApplicationDetail(Long id) {
        BusinessApplication application = getExistingTransferApplication(id);
        StudentRecord record = studentRecordMapper.selectById(application.getRecordId());
        Candidate candidate = candidateMapper.selectById(application.getCandidateId());
        return TransferApplicationVO.fromEntity(
                application, record, candidate, parseMaterialIds(application), parseExtensionData(application));
    }

    /**
     * @brief 提交考籍转入转出申请。
     *
     * @details
     * 提交时校验考籍档案存在性、转考类型、申请材料归属和同一档案同类型在办申请唯一性。
     * 申请保存为 SUBMITTED 状态，并记录一条 SUBMIT 流程记录。
     *
     * @param dto 转考申请提交请求对象。
     * @return 已提交的转考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferApplicationVO submitTransferApplication(TransferApplicationSubmitDTO dto) {
        String transferType = normalizeTransferType(dto.getTransferType());
        StudentRecord record = getExistingRecord(dto.getRecordId());
        validateTransferFields(transferType, dto.getSourceProvince(), dto.getTargetProvince());
        validateNoActiveTransferApplication(record.getId(), transferType, null);
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(record.getId(), materialIds);
        TokenUserVO user = AuthContextHolder.getUser();
        BusinessApplication application = new BusinessApplication();
        application.setApplicationNo(generateApplicationNo(transferType));
        application.setBusinessType(transferType);
        application.setRecordId(record.getId());
        application.setCandidateId(record.getCandidateId());
        application.setApplicationTitle(buildApplicationTitle(transferType, dto.getSourceProvince(), dto.getTargetProvince()));
        application.setApplicationStatus(STATUS_SUBMITTED);
        application.setCurrentNodeCode(STATUS_SUBMITTED);
        application.setCurrentNodeName("已提交");
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(buildExtensionData(dto)));
        application.setApplyUserId(user == null ? null : user.getId());
        application.setApplyUserName(user == null ? null : user.getRealName());
        application.setSubmitTime(LocalDateTime.now());
        application.setRemark(dto.getRemark());
        save(application);
        saveFlowRecord(application, ACTION_SUBMIT, null, STATUS_SUBMITTED, "转考申请已提交");
        return getTransferApplicationDetail(application.getId());
    }

    /**
     * @brief 修改考籍转入转出申请。
     *
     * @details
     * 仅允许 SUBMITTED 状态修改转考扩展字段、材料和备注，申请类型、考籍档案和考生归属
     * 不允许通过修改接口调整，避免审核链路失真。
     *
     * @param id 转考申请 ID。
     * @param dto 转考申请修改请求对象。
     * @return 修改后的转考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferApplicationVO updateTransferApplication(Long id, TransferApplicationUpdateDTO dto) {
        BusinessApplication application = getExistingTransferApplication(id);
        assertSubmittedStatus(application, "只有已提交的转考申请可以修改");
        validateTransferFields(application.getBusinessType(), dto.getSourceProvince(), dto.getTargetProvince());
        List<Long> materialIds = normalizeMaterialIds(dto.getMaterialIds());
        validateMaterialsBelongToRecord(application.getRecordId(), materialIds);
        application.setApplicationTitle(buildApplicationTitle(
                application.getBusinessType(), dto.getSourceProvince(), dto.getTargetProvince()));
        application.setMaterialIdsJson(writeMaterialIds(materialIds));
        application.setExtensionDataJson(writeExtensionData(buildExtensionData(dto)));
        application.setRemark(dto.getRemark());
        updateById(application);
        saveFlowRecord(application, ACTION_UPDATE, STATUS_SUBMITTED, STATUS_SUBMITTED, "转考申请内容已修改");
        return getTransferApplicationDetail(id);
    }

    /**
     * @brief 撤回考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 撤回请求对象。
     * @return 撤回后的转考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferApplicationVO withdrawTransferApplication(Long id, TransferApplicationWithdrawDTO dto) {
        BusinessApplication application = getExistingTransferApplication(id);
        assertSubmittedStatus(application, "只有已提交的转考申请可以撤回");
        String beforeStatus = application.getApplicationStatus();
        application.setApplicationStatus(STATUS_WITHDRAWN);
        application.setCurrentNodeCode(STATUS_WITHDRAWN);
        application.setCurrentNodeName("已撤回");
        application.setWithdrawTime(LocalDateTime.now());
        application.setWithdrawReason(dto.getWithdrawReason());
        updateById(application);
        saveFlowRecord(application, ACTION_WITHDRAW, beforeStatus, STATUS_WITHDRAWN, dto.getWithdrawReason());
        return getTransferApplicationDetail(id);
    }

    /**
     * @brief 审核通过考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @return 审核通过后的转考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferApplicationVO approveTransferApplication(Long id, TransferApplicationAuditDTO dto) {
        return auditTransferApplication(id, dto, STATUS_APPROVED);
    }

    /**
     * @brief 驳回考籍转入转出申请。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @return 驳回后的转考申请。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferApplicationVO rejectTransferApplication(Long id, TransferApplicationAuditDTO dto) {
        return auditTransferApplication(id, dto, STATUS_REJECTED);
    }

    /**
     * @brief 查询考籍转入转出申请流程记录。
     *
     * @param id 转考申请 ID。
     * @return 流程记录列表。
     */
    @Override
    public List<AuditRecordVO> listFlowRecords(Long id) {
        BusinessApplication application = getExistingTransferApplication(id);
        return auditRecordMapper.selectList(new LambdaQueryWrapper<AuditRecord>()
                        .eq(AuditRecord::getBusinessType, application.getBusinessType())
                        .eq(AuditRecord::getBusinessId, id)
                        .orderByAsc(AuditRecord::getOperationTime)
                        .orderByAsc(AuditRecord::getId))
                .stream()
                .map(AuditRecordVO::fromEntity)
                .toList();
    }

    /**
     * @brief 执行转考审核状态流转。
     *
     * @details
     * 审核入口只接受 SUBMITTED 状态的转考申请，目标状态限定为 APPROVED 或 REJECTED。
     * 审核通过转出申请时，会同步把考籍档案状态改为 TRANSFERRED_OUT，并写入状态日志和
     * 档案变更记录，保证申请审核结果与档案状态保持一致。
     *
     * @param id 转考申请 ID。
     * @param dto 审核请求对象。
     * @param targetStatus 目标状态。
     * @return 审核后的转考申请。
     */
    private TransferApplicationVO auditTransferApplication(Long id,
                                                          TransferApplicationAuditDTO dto,
                                                          String targetStatus) {
        if (!STATUS_APPROVED.equals(targetStatus) && !STATUS_REJECTED.equals(targetStatus)) {
            throw new BusinessException(400, "转考审核状态只能为APPROVED或REJECTED");
        }
        BusinessApplication application = getExistingTransferApplication(id);
        assertSubmittedStatus(application, "只有已提交的转考申请可以审核");
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
        if (STATUS_APPROVED.equals(targetStatus) && BUSINESS_TYPE_TRANSFER_OUT.equals(application.getBusinessType())) {
            recordStatusLinkageService.linkRecordStatus(
                    application.getRecordId(),
                    RECORD_STATUS_TRANSFERRED_OUT,
                    dto.getAuditOpinion(),
                    application.getBusinessType(),
                    application.getId());
        }
        saveFlowRecord(application,
                STATUS_APPROVED.equals(targetStatus) ? ACTION_APPROVE : ACTION_REJECT,
                beforeStatus,
                targetStatus,
                dto.getAuditOpinion());
        return getTransferApplicationDetail(id);
    }

    private LambdaQueryWrapper<BusinessApplication> buildApplicationQueryWrapper(String keyword,
                                                                                String transferType,
                                                                                String applicationStatus,
                                                                                Long recordId,
                                                                                Long candidateId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(transferType)) {
            wrapper.eq(BusinessApplication::getBusinessType, transferType);
        } else {
            wrapper.in(BusinessApplication::getBusinessType, List.of(BUSINESS_TYPE_TRANSFER_IN, BUSINESS_TYPE_TRANSFER_OUT));
        }
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

    private TransferApplicationVO buildVO(BusinessApplication application,
                                          Map<Long, StudentRecord> recordMap,
                                          Map<Long, Candidate> candidateMap) {
        return TransferApplicationVO.fromEntity(
                application,
                recordMap.get(application.getRecordId()),
                candidateMap.get(application.getCandidateId()),
                parseMaterialIds(application),
                parseExtensionData(application));
    }

    private BusinessApplication getExistingTransferApplication(Long id) {
        BusinessApplication application = getById(id);
        if (application == null || !isTransferType(application.getBusinessType())) {
            throw new BusinessException(404, "转考申请不存在");
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

    private void validateNoActiveTransferApplication(Long recordId, String transferType, Long excludeId) {
        LambdaQueryWrapper<BusinessApplication> wrapper = new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, transferType)
                .eq(BusinessApplication::getRecordId, recordId)
                .in(BusinessApplication::getApplicationStatus, List.of(STATUS_SUBMITTED, STATUS_APPROVED));
        if (excludeId != null) {
            wrapper.ne(BusinessApplication::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "该档案已有同类型在办或已通过转考申请");
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
                && !STATUS_APPROVED.equals(status)
                && !STATUS_REJECTED.equals(status)
                && !STATUS_WITHDRAWN.equals(status)) {
            throw new BusinessException(400, "转考申请状态只能为DRAFT、SUBMITTED、APPROVED、REJECTED或WITHDRAWN");
        }
    }

    private void validateOptionalTransferType(String transferType) {
        if (StringUtils.hasText(transferType)) {
            normalizeTransferType(transferType);
        }
    }

    private String normalizeTransferType(String transferType) {
        if (!StringUtils.hasText(transferType)) {
            throw new BusinessException(400, "转考类型不能为空");
        }
        String normalizedType = transferType.trim().toUpperCase(Locale.ROOT);
        if (!isTransferType(normalizedType)) {
            throw new BusinessException(400, "转考类型只能为TRANSFER_IN或TRANSFER_OUT");
        }
        return normalizedType;
    }

    private boolean isTransferType(String transferType) {
        return BUSINESS_TYPE_TRANSFER_IN.equals(transferType) || BUSINESS_TYPE_TRANSFER_OUT.equals(transferType);
    }

    private void validateTransferFields(String transferType, String sourceProvince, String targetProvince) {
        if (BUSINESS_TYPE_TRANSFER_IN.equals(transferType) && !StringUtils.hasText(sourceProvince)) {
            throw new BusinessException(400, "考籍转入申请必须填写原考籍省份");
        }
        if (BUSINESS_TYPE_TRANSFER_OUT.equals(transferType) && !StringUtils.hasText(targetProvince)) {
            throw new BusinessException(400, "考籍转出申请必须填写目标省份");
        }
    }

    private String generateApplicationNo(String transferType) {
        String prefix = BUSINESS_TYPE_TRANSFER_IN.equals(transferType) ? "ZR" : "ZC";
        for (int index = 0; index < 5; index++) {
            String applicationNo = prefix + LocalDate.now().format(APPLICATION_NO_DATE_FORMATTER)
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            Long count = count(new LambdaQueryWrapper<BusinessApplication>()
                    .eq(BusinessApplication::getApplicationNo, applicationNo));
            if (count == null || count == 0) {
                return applicationNo;
            }
        }
        throw new BusinessException(500, "转考申请编号生成失败");
    }

    private String buildApplicationTitle(String transferType, String sourceProvince, String targetProvince) {
        if (BUSINESS_TYPE_TRANSFER_IN.equals(transferType)) {
            return "考籍转入申请：" + safeText(sourceProvince);
        }
        return "考籍转出申请：" + safeText(targetProvince);
    }

    private Map<String, String> buildExtensionData(TransferApplicationSubmitDTO dto) {
        Map<String, String> extensionData = new HashMap<>();
        extensionData.put("sourceProvince", safeText(dto.getSourceProvince()));
        extensionData.put("sourceSchool", safeText(dto.getSourceSchool()));
        extensionData.put("sourceRecordNo", safeText(dto.getSourceRecordNo()));
        extensionData.put("targetProvince", safeText(dto.getTargetProvince()));
        extensionData.put("targetSchool", safeText(dto.getTargetSchool()));
        extensionData.put("transferReason", safeText(dto.getTransferReason()));
        return extensionData;
    }

    private Map<String, String> buildExtensionData(TransferApplicationUpdateDTO dto) {
        Map<String, String> extensionData = new HashMap<>();
        extensionData.put("sourceProvince", safeText(dto.getSourceProvince()));
        extensionData.put("sourceSchool", safeText(dto.getSourceSchool()));
        extensionData.put("sourceRecordNo", safeText(dto.getSourceRecordNo()));
        extensionData.put("targetProvince", safeText(dto.getTargetProvince()));
        extensionData.put("targetSchool", safeText(dto.getTargetSchool()));
        extensionData.put("transferReason", safeText(dto.getTransferReason()));
        return extensionData;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
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
            throw new BusinessException(500, "转考扩展字段序列化失败：" + exception.getMessage());
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

    private void saveFlowRecord(BusinessApplication application,
                                String action,
                                String beforeStatus,
                                String afterStatus,
                                String opinion) {
        TokenUserVO user = AuthContextHolder.getUser();
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setApplicationId(application.getId());
        auditRecord.setBusinessType(application.getBusinessType());
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
        auditRecordMapper.insert(auditRecord);
    }
}
