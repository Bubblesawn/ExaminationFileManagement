package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.StudentRecordArchiveDTO;
import com.exam.record.dto.StudentRecordCreateDTO;
import com.exam.record.dto.StudentRecordStatusUpdateDTO;
import com.exam.record.dto.StudentRecordUpdateDTO;
import com.exam.record.entity.Candidate;
import com.exam.record.entity.RecordStatusLog;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.mapper.RecordStatusLogMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.RecordChangeLogService;
import com.exam.record.service.StudentRecordService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.StudentRecordVO;
import com.exam.record.vo.TokenUserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @brief 考籍档案业务实现。
 *
 * @details
 * 负责考籍档案创建、编辑、查询、状态维护和归档操作。状态流转会同步写入
 * record_status_log 表，保证后续档案变更追溯和业务流程联动有稳定的数据来源。
 */
@Service
public class StudentRecordServiceImpl extends ServiceImpl<StudentRecordMapper, StudentRecord>
        implements StudentRecordService {
    private static final String CHANGE_TYPE_CREATE = "CREATE";
    private static final String CHANGE_TYPE_UPDATE = "UPDATE";
    private static final String CHANGE_TYPE_ARCHIVE = "ARCHIVE";
    private static final String CHANGE_TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_GRADUATED = "GRADUATED";
    private static final String ARCHIVE_STATUS_UNARCHIVED = "UNARCHIVED";
    private static final String ARCHIVE_STATUS_ARCHIVED = "ARCHIVED";

    private final CandidateMapper candidateMapper;
    private final RecordStatusLogMapper recordStatusLogMapper;
    private final RecordChangeLogService recordChangeLogService;

    /**
     * @brief 构造考籍档案业务实现。
     *
     * @param candidateMapper 考生信息 Mapper。
     * @param recordStatusLogMapper 档案状态记录 Mapper。
     * @param recordChangeLogService 档案变更记录业务服务。
     */
    public StudentRecordServiceImpl(CandidateMapper candidateMapper,
                                    RecordStatusLogMapper recordStatusLogMapper,
                                    RecordChangeLogService recordChangeLogService) {
        this.candidateMapper = candidateMapper;
        this.recordStatusLogMapper = recordStatusLogMapper;
        this.recordChangeLogService = recordChangeLogService;
    }

    /**
     * @brief 分页查询考籍档案。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param recordStatus 考籍状态。
     * @param archiveStatus 归档状态。
     * @param candidateId 考生ID。
     * @return 考籍档案分页数据。
     */
    @Override
    public Page<StudentRecordVO> pageRecords(long pageNo,
                                             long pageSize,
                                             String keyword,
                                             String recordStatus,
                                             String archiveStatus,
                                             Long candidateId) {
        validateOptionalRecordStatus(recordStatus);
        validateOptionalArchiveStatus(archiveStatus);
        LambdaQueryWrapper<StudentRecord> wrapper = buildRecordQueryWrapper(keyword, recordStatus, archiveStatus, candidateId);
        Page<StudentRecord> recordPage = page(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, Candidate> candidateMap = loadCandidateMap(recordPage.getRecords());
        Page<StudentRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(recordPage.getRecords().stream()
                .map(record -> StudentRecordVO.fromEntity(record, candidateMap.get(record.getCandidateId())))
                .toList());
        return voPage;
    }

    /**
     * @brief 查询考籍档案详情。
     *
     * @param id 档案ID。
     * @return 考籍档案详情。
     */
    @Override
    public StudentRecordVO getRecordDetail(Long id) {
        StudentRecord record = getExistingRecord(id);
        Candidate candidate = candidateMapper.selectById(record.getCandidateId());
        return StudentRecordVO.fromEntity(record, candidate);
    }

    /**
     * @brief 创建考籍档案。
     *
     * @details
     * 创建前校验考生存在性和考籍号唯一性，默认考籍状态为 NORMAL、归档状态为
     * UNARCHIVED，并记录一条初始状态日志。
     *
     * @param dto 新增考籍档案请求对象。
     * @return 新增后的考籍档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentRecordVO createRecord(StudentRecordCreateDTO dto) {
        validateCandidateExists(dto.getCandidateId());
        if (isRecordNoExists(dto.getRecordNo(), null)) {
            throw new BusinessException(409, "考籍号已存在");
        }
        String recordStatus = StringUtils.hasText(dto.getRecordStatus()) ? dto.getRecordStatus() : STATUS_NORMAL;
        validateRecordStatus(recordStatus);
        StudentRecord record = new StudentRecord();
        record.setCandidateId(dto.getCandidateId());
        record.setRecordNo(dto.getRecordNo());
        record.setEnrollBatch(dto.getEnrollBatch());
        record.setEducationLevel(dto.getEducationLevel());
        record.setMajorCode(dto.getMajorCode());
        record.setMajorName(dto.getMajorName());
        record.setRecordStatus(recordStatus);
        record.setArchiveStatus(ARCHIVE_STATUS_UNARCHIVED);
        record.setRemark(dto.getRemark());
        save(record);
        saveStatusLog(record.getId(), null, recordStatus, "新建考籍档案");
        recordChangeLogService.recordChange(
                record.getId(),
                CHANGE_TYPE_CREATE,
                "record",
                null,
                buildRecordSummary(record),
                "新建考籍档案");
        return getRecordDetail(record.getId());
    }

    /**
     * @brief 编辑考籍档案基础信息。
     *
     * @param id 档案ID。
     * @param dto 修改考籍档案请求对象。
     * @return 修改后的考籍档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentRecordVO updateRecord(Long id, StudentRecordUpdateDTO dto) {
        StudentRecord record = getExistingRecord(id);
        StudentRecord beforeRecord = copyRecord(record);
        validateCandidateExists(dto.getCandidateId());
        record.setCandidateId(dto.getCandidateId());
        record.setEnrollBatch(dto.getEnrollBatch());
        record.setEducationLevel(dto.getEducationLevel());
        record.setMajorCode(dto.getMajorCode());
        record.setMajorName(dto.getMajorName());
        record.setRemark(dto.getRemark());
        updateById(record);
        recordFieldChange(id, "candidateId", beforeRecord.getCandidateId(), record.getCandidateId(), "编辑考籍档案");
        recordFieldChange(id, "enrollBatch", beforeRecord.getEnrollBatch(), record.getEnrollBatch(), "编辑考籍档案");
        recordFieldChange(id, "educationLevel", beforeRecord.getEducationLevel(), record.getEducationLevel(), "编辑考籍档案");
        recordFieldChange(id, "majorCode", beforeRecord.getMajorCode(), record.getMajorCode(), "编辑考籍档案");
        recordFieldChange(id, "majorName", beforeRecord.getMajorName(), record.getMajorName(), "编辑考籍档案");
        recordFieldChange(id, "remark", beforeRecord.getRemark(), record.getRemark(), "编辑考籍档案");
        return getRecordDetail(id);
    }

    /**
     * @brief 更新考籍档案状态。
     *
     * @details
     * 仅允许流转到 NORMAL、SUSPENDED、CANCELLED、GRADUATED 四种业务状态。
     * 状态未发生变化时直接返回详情，不额外写入状态记录。
     *
     * @param id 档案ID。
     * @param dto 状态更新请求对象。
     * @return 状态更新后的考籍档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentRecordVO updateRecordStatus(Long id, StudentRecordStatusUpdateDTO dto) {
        StudentRecord record = getExistingRecord(id);
        validateRecordStatus(dto.getRecordStatus());
        String beforeStatus = record.getRecordStatus();
        if (dto.getRecordStatus().equals(beforeStatus)) {
            return getRecordDetail(id);
        }
        record.setRecordStatus(dto.getRecordStatus());
        updateById(record);
        saveStatusLog(id, beforeStatus, dto.getRecordStatus(), dto.getChangeReason());
        recordChangeLogService.recordChange(
                id,
                CHANGE_TYPE_STATUS_CHANGE,
                "recordStatus",
                beforeStatus,
                dto.getRecordStatus(),
                dto.getChangeReason());
        return getRecordDetail(id);
    }

    /**
     * @brief 将考籍档案归档。
     *
     * @details
     * 归档只允许执行一次，成功后记录归档时间和操作人ID，并写入一条归档状态记录。
     *
     * @param id 档案ID。
     * @param dto 归档请求对象。
     * @return 归档后的考籍档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentRecordVO archiveRecord(Long id, StudentRecordArchiveDTO dto) {
        StudentRecord record = getExistingRecord(id);
        if (ARCHIVE_STATUS_ARCHIVED.equals(record.getArchiveStatus())) {
            throw new BusinessException(400, "档案已归档，不能重复归档");
        }
        TokenUserVO user = AuthContextHolder.getUser();
        record.setArchiveStatus(ARCHIVE_STATUS_ARCHIVED);
        record.setArchiveTime(LocalDateTime.now());
        record.setArchiveOperatorId(user == null ? null : user.getId());
        updateById(record);
        String reason = dto == null || !StringUtils.hasText(dto.getArchiveReason())
                ? "考籍档案归档"
                : dto.getArchiveReason();
        saveStatusLog(id, ARCHIVE_STATUS_UNARCHIVED, ARCHIVE_STATUS_ARCHIVED, reason);
        recordChangeLogService.recordChange(
                id,
                CHANGE_TYPE_ARCHIVE,
                "archiveStatus",
                ARCHIVE_STATUS_UNARCHIVED,
                ARCHIVE_STATUS_ARCHIVED,
                reason);
        return getRecordDetail(id);
    }

    private LambdaQueryWrapper<StudentRecord> buildRecordQueryWrapper(String keyword,
                                                                      String recordStatus,
                                                                      String archiveStatus,
                                                                      Long candidateId) {
        LambdaQueryWrapper<StudentRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            Set<Long> candidateIds = findCandidateIds(keyword);
            wrapper.and(query -> {
                query.like(StudentRecord::getRecordNo, keyword)
                        .or()
                        .like(StudentRecord::getMajorName, keyword)
                        .or()
                        .like(StudentRecord::getMajorCode, keyword)
                        .or()
                        .like(StudentRecord::getRemark, keyword);
                if (!candidateIds.isEmpty()) {
                    query.or().in(StudentRecord::getCandidateId, candidateIds);
                }
            });
        }
        if (StringUtils.hasText(recordStatus)) {
            wrapper.eq(StudentRecord::getRecordStatus, recordStatus);
        }
        if (StringUtils.hasText(archiveStatus)) {
            wrapper.eq(StudentRecord::getArchiveStatus, archiveStatus);
        }
        if (candidateId != null) {
            wrapper.eq(StudentRecord::getCandidateId, candidateId);
        }
        wrapper.orderByDesc(StudentRecord::getCreateTime);
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

    private Map<Long, Candidate> loadCandidateMap(List<StudentRecord> records) {
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> candidateIds = records.stream()
                .map(StudentRecord::getCandidateId)
                .collect(Collectors.toSet());
        return candidateMapper.selectList(new LambdaQueryWrapper<Candidate>()
                        .in(Candidate::getId, candidateIds))
                .stream()
                .collect(Collectors.toMap(Candidate::getId, Function.identity()));
    }

    private StudentRecord getExistingRecord(Long id) {
        StudentRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "考籍档案不存在");
        }
        return record;
    }

    private void validateCandidateExists(Long candidateId) {
        if (candidateMapper.selectById(candidateId) == null) {
            throw new BusinessException(404, "考生不存在");
        }
    }

    private boolean isRecordNoExists(String recordNo, Long excludeId) {
        LambdaQueryWrapper<StudentRecord> wrapper = new LambdaQueryWrapper<StudentRecord>()
                .eq(StudentRecord::getRecordNo, recordNo);
        if (excludeId != null) {
            wrapper.ne(StudentRecord::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void validateOptionalRecordStatus(String status) {
        if (StringUtils.hasText(status)) {
            validateRecordStatus(status);
        }
    }

    private void validateRecordStatus(String status) {
        if (!STATUS_NORMAL.equals(status)
                && !STATUS_SUSPENDED.equals(status)
                && !STATUS_CANCELLED.equals(status)
                && !STATUS_GRADUATED.equals(status)) {
            throw new BusinessException(400, "考籍状态只能为NORMAL、SUSPENDED、CANCELLED或GRADUATED");
        }
    }

    private void validateOptionalArchiveStatus(String status) {
        if (StringUtils.hasText(status)
                && !ARCHIVE_STATUS_UNARCHIVED.equals(status)
                && !ARCHIVE_STATUS_ARCHIVED.equals(status)) {
            throw new BusinessException(400, "归档状态只能为UNARCHIVED或ARCHIVED");
        }
    }

    private void saveStatusLog(Long recordId, String beforeStatus, String afterStatus, String reason) {
        TokenUserVO user = AuthContextHolder.getUser();
        RecordStatusLog statusLog = new RecordStatusLog();
        statusLog.setRecordId(recordId);
        statusLog.setBeforeStatus(beforeStatus);
        statusLog.setAfterStatus(afterStatus);
        statusLog.setChangeReason(reason);
        statusLog.setOperatorId(user == null ? null : user.getId());
        statusLog.setOperatorName(user == null ? null : user.getRealName());
        statusLog.setOperationTime(LocalDateTime.now());
        recordStatusLogMapper.insert(statusLog);
    }

    private StudentRecord copyRecord(StudentRecord source) {
        StudentRecord target = new StudentRecord();
        target.setCandidateId(source.getCandidateId());
        target.setEnrollBatch(source.getEnrollBatch());
        target.setEducationLevel(source.getEducationLevel());
        target.setMajorCode(source.getMajorCode());
        target.setMajorName(source.getMajorName());
        target.setRemark(source.getRemark());
        return target;
    }

    private void recordFieldChange(Long recordId,
                                   String changeField,
                                   Object beforeValue,
                                   Object afterValue,
                                   String changeReason) {
        String beforeText = stringifyValue(beforeValue);
        String afterText = stringifyValue(afterValue);
        if (Objects.equals(beforeText, afterText)) {
            return;
        }
        recordChangeLogService.recordChange(
                recordId,
                CHANGE_TYPE_UPDATE,
                changeField,
                beforeText,
                afterText,
                changeReason);
    }

    private String stringifyValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String buildRecordSummary(StudentRecord record) {
        return "考籍号：" + record.getRecordNo()
                + "；考生ID：" + record.getCandidateId()
                + "；专业：" + stringifyValue(record.getMajorName())
                + "；状态：" + record.getRecordStatus();
    }
}
