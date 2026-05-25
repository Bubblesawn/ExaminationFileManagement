package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.Candidate;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 考生基础信息 Mapper。
 */
@Mapper
public interface CandidateMapper extends BaseMapper<Candidate> {
}

