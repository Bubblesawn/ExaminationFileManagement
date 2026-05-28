package com.exam.record.service.impl;

import com.exam.record.common.BusinessException;
import com.exam.record.entity.RecordStatusLog;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.RecordStatusLogMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.RecordChangeLogService;
import com.exam.record.service.RecordStatusLinkageService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.TokenUserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @brief 档案状态联动服务实现。
 *
 * @details
 * 统一处理业务流程审核结果与考籍档案状态之间的联动。该实现会在状态真正变化时同时写入
 * 档案状态记录和档案变更记录，保证流程记录、档案当前状态和追溯日志三者一致。
 */
@Service
public class RecordStatusLinkageServiceImpl implements RecordStatusLinkageService {
    private static final String CHANGE_TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    private static final List<String> SUPPORTED_RECORD_STATUSES = List.of(
            "NORMAL", "SUSPENDED", "CANCELLED", "TRANSFERRED_OUT", "GRADUATED");

    private final StudentRecordMapper studentRecordMapper;
    private final RecordStatusLogMapper recordStatusLogMapper;
    private final RecordChangeLogService recordChangeLogService;

    /**
     * @brief 构造档案状态联动服务。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param recordStatusLogMapper 档案状态记录 Mapper。
     * @param recordChangeLogService 档案变更记录业务服务。
     */
    public RecordStatusLinkageServiceImpl(StudentRecordMapper studentRecordMapper,
                                          RecordStatusLogMapper recordStatusLogMapper,
                                          RecordChangeLogService recordChangeLogService) {
        this.studentRecordMapper = studentRecordMapper;
        this.recordStatusLogMapper = recordStatusLogMapper;
        this.recordChangeLogService = recordChangeLogService;
    }

    /**
     * @brief 根据业务流程结果联动考籍档案状态。
     *
     * @details
     * 当目标状态与当前档案状态一致时直接返回，不重复写入日志；否则更新档案状态并记录
     * 操作人、业务来源和申请 ID，便于后续从档案详情反查触发状态变化的业务流程。
     *
     * @param recordId 考籍档案 ID。
     * @param targetStatus 目标考籍状态。
     * @param changeReason 状态变更原因。
     * @param businessType 触发联动的业务类型。
     * @param applicationId 触发联动的申请 ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkRecordStatus(Long recordId,
                                 String targetStatus,
                                 String changeReason,
                                 String businessType,
                                 Long applicationId) {
        validateTargetStatus(targetStatus);
        StudentRecord record = studentRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "考籍档案不存在");
        }
        String beforeStatus = record.getRecordStatus();
        if (targetStatus.equals(beforeStatus)) {
            return;
        }
        record.setRecordStatus(targetStatus);
        studentRecordMapper.updateById(record);
        String reason = buildLinkageReason(changeReason, businessType, applicationId);
        saveStatusLog(recordId, beforeStatus, targetStatus, reason);
        recordChangeLogService.recordChange(
                recordId,
                CHANGE_TYPE_STATUS_CHANGE,
                "recordStatus",
                beforeStatus,
                targetStatus,
                reason);
    }

    private void validateTargetStatus(String targetStatus) {
        if (!StringUtils.hasText(targetStatus) || !SUPPORTED_RECORD_STATUSES.contains(targetStatus)) {
            throw new BusinessException(400, "联动目标档案状态只能为NORMAL、SUSPENDED、CANCELLED、TRANSFERRED_OUT或GRADUATED");
        }
    }

    private String buildLinkageReason(String changeReason, String businessType, Long applicationId) {
        String reason = StringUtils.hasText(changeReason) ? changeReason.trim() : "业务流程审核通过后联动档案状态";
        String source = StringUtils.hasText(businessType) ? businessType.trim() : "UNKNOWN";
        return reason + "（来源：" + source + "，申请ID：" + applicationId + "）";
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
}
