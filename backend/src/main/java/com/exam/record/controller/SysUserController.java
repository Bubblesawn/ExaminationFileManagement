package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.SysUserCreateDTO;
import com.exam.record.dto.SysUserResetPasswordDTO;
import com.exam.record.dto.SysUserUpdateDTO;
import com.exam.record.service.SysUserService;
import com.exam.record.vo.SysUserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @brief 系统用户管理接口。
 */
@RestController
@RequestMapping("/api/system/users")
public class SysUserController {
    private final SysUserService sysUserService;

    /**
     * @brief 构造系统用户管理控制器。
     *
     * @param sysUserService 系统用户管理业务服务。
     */
    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * @brief 分页查询系统用户。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param status 用户状态。
     * @return 系统用户分页数据。
     */
    @GetMapping("/page")
    public Result<Page<SysUserVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(sysUserService.pageUsers(pageNo, pageSize, keyword, status));
    }

    /**
     * @brief 查询系统用户详情。
     *
     * @param id 用户ID。
     * @return 系统用户详情。
     */
    @GetMapping("/{id}")
    public Result<SysUserVO> detail(@PathVariable Long id) {
        return Result.success(sysUserService.getUserDetail(id));
    }

    /**
     * @brief 新增系统用户。
     *
     * @param dto 新增系统用户请求对象。
     * @return 新增后的系统用户。
     */
    @PostMapping
    public Result<SysUserVO> create(@Valid @RequestBody SysUserCreateDTO dto) {
        return Result.success(sysUserService.createUser(dto));
    }

    /**
     * @brief 修改系统用户。
     *
     * @param id 用户ID。
     * @param dto 修改系统用户请求对象。
     * @return 修改后的系统用户。
     */
    @PutMapping("/{id}")
    public Result<SysUserVO> update(@PathVariable Long id, @Valid @RequestBody SysUserUpdateDTO dto) {
        return Result.success(sysUserService.updateUser(id, dto));
    }

    /**
     * @brief 删除系统用户。
     *
     * @param id 用户ID。
     * @return 删除结果。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    /**
     * @brief 启用系统用户。
     *
     * @param id 用户ID。
     * @return 启用后的系统用户。
     */
    @PutMapping("/{id}/enable")
    public Result<SysUserVO> enable(@PathVariable Long id) {
        return Result.success(sysUserService.enableUser(id));
    }

    /**
     * @brief 禁用系统用户。
     *
     * @param id 用户ID。
     * @return 禁用后的系统用户。
     */
    @PutMapping("/{id}/disable")
    public Result<SysUserVO> disable(@PathVariable Long id) {
        return Result.success(sysUserService.disableUser(id));
    }

    /**
     * @brief 重置系统用户密码。
     *
     * @param id 用户ID。
     * @param dto 重置密码请求对象。
     * @return 重置结果。
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody SysUserResetPasswordDTO dto) {
        sysUserService.resetPassword(id, dto);
        return Result.success();
    }
}
