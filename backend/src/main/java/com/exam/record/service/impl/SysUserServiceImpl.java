package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.SysUserCreateDTO;
import com.exam.record.dto.SysUserResetPasswordDTO;
import com.exam.record.dto.SysUserUpdateDTO;
import com.exam.record.entity.SysUser;
import com.exam.record.mapper.SysUserMapper;
import com.exam.record.service.SysUserService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.util.PasswordUtil;
import com.exam.record.vo.SysUserVO;
import com.exam.record.vo.TokenUserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @brief 系统用户管理业务实现。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    /**
     * @brief 分页查询系统用户。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配账号、姓名、手机号或邮箱。
     * @param status 用户状态。
     * @return 系统用户分页数据。
     */
    @Override
    public Page<SysUserVO> pageUsers(long pageNo, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = page(new Page<>(pageNo, pageSize), wrapper);
        Page<SysUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(SysUserVO::fromEntity).toList());
        return voPage;
    }

    /**
     * @brief 查询系统用户详情。
     *
     * @param id 用户ID。
     * @return 系统用户详情。
     */
    @Override
    public SysUserVO getUserDetail(Long id) {
        return SysUserVO.fromEntity(getExistingUser(id));
    }

    /**
     * @brief 新增系统用户。
     *
     * @param dto 新增系统用户请求对象。
     * @return 新增后的系统用户。
     */
    @Override
    public SysUserVO createUser(SysUserCreateDTO dto) {
        if (isUsernameExists(dto.getUsername(), null)) {
            throw new BusinessException(409, "登录账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED);
        save(user);
        return SysUserVO.fromEntity(user);
    }

    /**
     * @brief 修改系统用户。
     *
     * @param id 用户ID。
     * @param dto 修改系统用户请求对象。
     * @return 修改后的系统用户。
     */
    @Override
    public SysUserVO updateUser(Long id, SysUserUpdateDTO dto) {
        SysUser user = getExistingUser(id);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        if (StringUtils.hasText(dto.getStatus())) {
            validateStatus(dto.getStatus());
            user.setStatus(dto.getStatus());
        }
        updateById(user);
        return SysUserVO.fromEntity(getById(id));
    }

    /**
     * @brief 删除系统用户。
     *
     * @details
     * 删除前会校验用户存在性，并禁止当前登录用户删除自己的账号，避免误删后当前会话失去管理入口。
     *
     * @param id 用户ID。
     */
    @Override
    public void deleteUser(Long id) {
        getExistingUser(id);
        if (isCurrentUser(id)) {
            throw new BusinessException(400, "不能删除当前登录用户");
        }
        removeById(id);
    }

    /**
     * @brief 启用系统用户。
     *
     * @param id 用户ID。
     * @return 启用后的系统用户。
     */
    @Override
    public SysUserVO enableUser(Long id) {
        return updateStatus(id, STATUS_ENABLED);
    }

    /**
     * @brief 禁用系统用户。
     *
     * @param id 用户ID。
     * @return 禁用后的系统用户。
     */
    @Override
    public SysUserVO disableUser(Long id) {
        if (isCurrentUser(id)) {
            throw new BusinessException(400, "不能禁用当前登录用户");
        }
        return updateStatus(id, STATUS_DISABLED);
    }

    /**
     * @brief 重置系统用户密码。
     *
     * @param id 用户ID。
     * @param dto 重置密码请求对象。
     */
    @Override
    public void resetPassword(Long id, SysUserResetPasswordDTO dto) {
        SysUser user = getExistingUser(id);
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        updateById(user);
    }

    private SysUserVO updateStatus(Long id, String status) {
        SysUser user = getExistingUser(id);
        user.setStatus(status);
        updateById(user);
        return SysUserVO.fromEntity(getById(id));
    }

    private SysUser getExistingUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private boolean isUsernameExists(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "用户状态只能为ENABLED或DISABLED");
        }
    }

    private boolean isCurrentUser(Long id) {
        TokenUserVO currentUser = AuthContextHolder.getUser();
        return currentUser != null && id != null && id.equals(currentUser.getId());
    }
}
