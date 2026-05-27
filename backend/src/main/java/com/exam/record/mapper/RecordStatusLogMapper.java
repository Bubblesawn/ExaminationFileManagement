package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.RecordStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 档案状态记录 Mapper。
 */
@Mapper
public interface RecordStatusLogMapper extends BaseMapper<RecordStatusLog> {
}
