package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.RecordChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 档案变更记录 Mapper。
 */
@Mapper
public interface RecordChangeLogMapper extends BaseMapper<RecordChangeLog> {
}
