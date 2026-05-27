package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 登录日志 Mapper。
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
