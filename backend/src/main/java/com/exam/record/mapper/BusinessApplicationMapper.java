package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.BusinessApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 通用业务申请 Mapper。
 */
@Mapper
public interface BusinessApplicationMapper extends BaseMapper<BusinessApplication> {
}
