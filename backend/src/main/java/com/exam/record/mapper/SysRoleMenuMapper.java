package com.exam.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.record.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * @brief 角色菜单关联 Mapper。
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
