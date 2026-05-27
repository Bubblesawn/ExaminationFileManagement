package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.StudentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 考籍档案 Mapper。
 */
@Mapper
public interface StudentRecordMapper extends BaseMapper<StudentRecord> {
}
