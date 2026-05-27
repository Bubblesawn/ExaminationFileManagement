package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.ProcessStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 流程状态 Mapper。
 */
@Mapper
public interface ProcessStatusMapper extends BaseMapper<ProcessStatus> {
}
