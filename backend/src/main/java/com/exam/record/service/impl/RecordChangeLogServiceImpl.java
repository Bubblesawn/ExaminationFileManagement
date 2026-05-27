package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.entity.RecordChangeLog;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.RecordChangeLogMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.RecordChangeLogService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.RecordChangeLogVO;
import com.exam.record.vo.TokenUserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * @brief 档案变更记录业务实现。
 *
 * @details
 * 统一处理 record_change_log 的自动写入和分页查询，确保档案主业务、
 * 状态流转和材料维护产生的变更记录具有一致的操作人、时间和排序规则。
 */
@Service
public class RecordChangeLogServiceImpl extends ServiceImpl<RecordChangeLogMapper, RecordChangeLog>
        implements RecordChangeLogService {
    private static final String TYPE_CREATE = "CREATE";
    private static final String TYPE_UPDATE = "UPDATE";
    private static final String TYPE_ARCHIVE = "ARCHIVE";
    private static final String TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String TYPE_MATERIAL_CHANGE = "MATERIAL_CHANGE";

    private final StudentRecordMapper studentRecordMapper;

    /**
     * @brief 构造档案变更记录业务实现。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     */
    public RecordChangeLogServiceImpl(StudentRecordMapper studentRecordMapper) {
        this.studentRecordMapper = studentRecordMapper;
    }

    /**
     * @brief 分页查询指定档案的变更记录。
     *
     * @param recordId 考籍档案ID。
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param changeType 变更类型，可为空。
     * @return 档案变更记录分页数据。
     */
    @Override
    public Page<RecordChangeLogVO> pageRecordChangeLogs(Long recordId, long pageNo, long pageSize, String changeType) {
        validateRecordExists(recordId);
        validateOptionalChangeType(changeType);
        LambdaQueryWrapper<RecordChangeLog> wrapper = new LambdaQueryWrapper<RecordChangeLog>()
                .eq(RecordChangeLog::getRecordId, recordId);
        if (StringUtils.hasText(changeType)) {
            wrapper.eq(RecordChangeLog::getChangeType, changeType);
        }
        wrapper.orderByDesc(RecordChangeLog::getOperationTime)
                .orderByDesc(RecordChangeLog::getId);

        Page<RecordChangeLog> changeLogPage = page(new Page<>(pageNo, pageSize), wrapper);
        Page<RecordChangeLogVO> voPage = new Page<>(
                changeLogPage.getCurrent(),
                changeLogPage.getSize(),
                changeLogPage.getTotal());
        voPage.setRecords(changeLogPage.getRecords().stream()
                .map(RecordChangeLogVO::fromEntity)
                .toList());
        return voPage;
    }

    /**
     * @brief 自动保存档案变更记录。
     *
     * @details
     * 保存前校验档案存在性和变更类型合法性，并从当前登录上下文提取操作人信息。
     * 该方法与主业务事务共同提交，主业务回滚时变更记录也会随之回滚。
     *
     * @param recordId 考籍档案ID。
     * @param changeType 变更类型。
     * @param changeField 变更字段。
     * @param beforeValue 变更前内容。
     * @param afterValue 变更后内容。
     * @param changeReason 变更原因。
     */
    @Override
    public void recordChange(Long recordId,
                             String changeType,
                             String changeField,
                             String beforeValue,
                             String afterValue,
                             String changeReason) {
        validateRecordExists(recordId);
        validateChangeType(changeType);
        TokenUserVO user = AuthContextHolder.getUser();
        RecordChangeLog changeLog = new RecordChangeLog();
        changeLog.setRecordId(recordId);
        changeLog.setChangeType(changeType);
        changeLog.setChangeField(changeField);
        changeLog.setBeforeValue(beforeValue);
        changeLog.setAfterValue(afterValue);
        changeLog.setChangeReason(changeReason);
        changeLog.setOperatorId(user == null ? null : user.getId());
        changeLog.setOperatorName(user == null ? null : user.getRealName());
        changeLog.setOperationTime(LocalDateTime.now());
        save(changeLog);
    }

    private void validateRecordExists(Long recordId) {
        StudentRecord record = studentRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "考籍档案不存在");
        }
    }

    private void validateOptionalChangeType(String changeType) {
        if (StringUtils.hasText(changeType)) {
            validateChangeType(changeType);
        }
    }

    private void validateChangeType(String changeType) {
        if (!TYPE_CREATE.equals(changeType)
                && !TYPE_UPDATE.equals(changeType)
                && !TYPE_ARCHIVE.equals(changeType)
                && !TYPE_STATUS_CHANGE.equals(changeType)
                && !TYPE_MATERIAL_CHANGE.equals(changeType)) {
            throw new BusinessException(400, "变更类型只能为CREATE、UPDATE、ARCHIVE、STATUS_CHANGE或MATERIAL_CHANGE");
        }
    }
}
