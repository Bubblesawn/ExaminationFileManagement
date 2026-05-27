package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.SysUserCreateDTO;
import com.exam.record.dto.SysUserResetPasswordDTO;
import com.exam.record.dto.SysUserUpdateDTO;
import com.exam.record.entity.SysUser;
import com.exam.record.vo.SysUserVO;

/**
 * @brief 系统用户管理业务接口。
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * @brief 分页查询系统用户。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配账号、姓名、手机号或邮箱。
     * @param status 用户状态。
     * @return 系统用户分页数据。
     */
    Page<SysUserVO> pageUsers(long pageNo, long pageSize, String keyword, String status);

    /**
     * @brief 查询系统用户详情。
     *
     * @param id 用户ID。
     * @return 系统用户详情。
     */
    SysUserVO getUserDetail(Long id);

    /**
     * @brief 新增系统用户。
     *
     * @param dto 新增系统用户请求对象。
     * @return 新增后的系统用户。
     */
    SysUserVO createUser(SysUserCreateDTO dto);

    /**
     * @brief 修改系统用户。
     *
     * @param id 用户ID。
     * @param dto 修改系统用户请求对象。
     * @return 修改后的系统用户。
     */
    SysUserVO updateUser(Long id, SysUserUpdateDTO dto);

    /**
     * @brief 删除系统用户。
     *
     * @param id 用户ID。
     */
    void deleteUser(Long id);

    /**
     * @brief 启用系统用户。
     *
     * @param id 用户ID。
     * @return 启用后的系统用户。
     */
    SysUserVO enableUser(Long id);

    /**
     * @brief 禁用系统用户。
     *
     * @param id 用户ID。
     * @return 禁用后的系统用户。
     */
    SysUserVO disableUser(Long id);

    /**
     * @brief 重置系统用户密码。
     *
     * @param id 用户ID。
     * @param dto 重置密码请求对象。
     */
    void resetPassword(Long id, SysUserResetPasswordDTO dto);
}
