package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 审核记录 Mapper。
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}
