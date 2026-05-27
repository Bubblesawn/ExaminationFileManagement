package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.entity.RecordChangeLog;
import com.exam.record.vo.RecordChangeLogVO;

/**
 * @brief 档案变更记录业务接口。
 */
public interface RecordChangeLogService extends IService<RecordChangeLog> {

    /**
     * @brief 分页查询指定档案的变更记录。
     *
     * @param recordId 考籍档案ID。
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param changeType 变更类型，可为空。
     * @return 档案变更记录分页数据。
     */
    Page<RecordChangeLogVO> pageRecordChangeLogs(Long recordId, long pageNo, long pageSize, String changeType);

    /**
     * @brief 自动保存档案变更记录。
     *
     * @details
     * 由档案创建、编辑、状态更新、归档和材料维护等业务入口调用，统一补齐操作人、
     * 操作时间与变更原因，保证前端查询到的变更轨迹口径一致。
     *
     * @param recordId 考籍档案ID。
     * @param changeType 变更类型。
     * @param changeField 变更字段。
     * @param beforeValue 变更前内容。
     * @param afterValue 变更后内容。
     * @param changeReason 变更原因。
     */
    void recordChange(Long recordId,
                      String changeType,
                      String changeField,
                      String beforeValue,
                      String afterValue,
                      String changeReason);
}
