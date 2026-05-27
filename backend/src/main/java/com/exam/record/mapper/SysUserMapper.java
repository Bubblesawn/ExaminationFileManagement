package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 系统用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
