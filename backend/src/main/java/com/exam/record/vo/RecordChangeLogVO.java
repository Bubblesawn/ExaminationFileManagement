package com.exam.record.vo;

import com.exam.record.entity.RecordChangeLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @brief 档案变更记录响应对象。
 *
 * @details
 * 面向前端变更记录列表和详情展示，保留变更类型、变更字段、变更前后值、
 * 变更原因以及操作人信息，避免直接暴露实体扩展时可能出现的内部字段。
 */
@Data
public class RecordChangeLogVO {
    private Long id;
    private Long recordId;
    private String changeType;
    private String changeField;
    private String beforeValue;
    private String afterValue;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operationTime;

    /**
     * @brief 将档案变更记录实体转换为响应对象。
     *
     * @param changeLog 档案变更记录实体。
     * @return 档案变更记录响应对象。
     */
    public static RecordChangeLogVO fromEntity(RecordChangeLog changeLog) {
        RecordChangeLogVO vo = new RecordChangeLogVO();
        vo.setId(changeLog.getId());
        vo.setRecordId(changeLog.getRecordId());
        vo.setChangeType(changeLog.getChangeType());
        vo.setChangeField(changeLog.getChangeField());
        vo.setBeforeValue(changeLog.getBeforeValue());
        vo.setAfterValue(changeLog.getAfterValue());
        vo.setChangeReason(changeLog.getChangeReason());
        vo.setOperatorId(changeLog.getOperatorId());
        vo.setOperatorName(changeLog.getOperatorName());
        vo.setOperationTime(changeLog.getOperationTime());
        return vo;
    }
}
