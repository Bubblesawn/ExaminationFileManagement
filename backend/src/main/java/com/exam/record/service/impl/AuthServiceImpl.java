package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.LoginDTO;
import com.exam.record.entity.SysLoginLog;
import com.exam.record.entity.SysUser;
import com.exam.record.mapper.SysLoginLogMapper;
import com.exam.record.mapper.SysUserMapper;
import com.exam.record.service.AuthService;
import com.exam.record.util.AuthTokenUtil;
import com.exam.record.util.ClientIpUtil;
import com.exam.record.vo.LoginUserVO;
import com.exam.record.vo.LoginVO;
import com.exam.record.vo.TokenUserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @brief 登录认证业务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String LOGIN_SUCCESS = "SUCCESS";
    private static final String LOGIN_FAIL = "FAIL";
    private static final String NOOP_PREFIX = "{noop}";
    private static final String SHA256_PREFIX = "{sha256}";

    private final SysUserMapper sysUserMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final AuthTokenUtil authTokenUtil;
    private final Set<String> invalidTokens = ConcurrentHashMap.newKeySet();

    /**
     * @brief 构造登录认证业务实现。
     *
     * @param sysUserMapper 系统用户 Mapper。
     * @param sysLoginLogMapper 登录日志 Mapper。
     * @param authTokenUtil Token 工具。
     */
    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           SysLoginLogMapper sysLoginLogMapper,
                           AuthTokenUtil authTokenUtil) {
        this.sysUserMapper = sysUserMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.authTokenUtil = authTokenUtil;
    }

    /**
     * @brief 用户登录。
     *
     * @details
     * 登录时按账号查询用户，依次校验账号存在、账号状态和密码；任何失败都会记录登录失败日志。
     * 密码兼容第一阶段测试数据中的 {noop} 明文占位，同时支持 {sha256} 哈希。
     *
     * @param dto 登录请求参数。
     * @param request HTTP 请求对象。
     * @return 登录成功响应。
     */
    @Override
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())
                .last("LIMIT 1"));
        if (user == null) {
            recordLoginLog(dto.getUsername(), null, LOGIN_FAIL, "账号不存在", request);
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            recordLoginLog(dto.getUsername(), user.getId(), LOGIN_FAIL, "账号已禁用", request);
            throw new BusinessException(403, "账号已禁用，请联系管理员");
        }
        if (!matchesPassword(dto.getPassword(), user.getPassword())) {
            recordLoginLog(dto.getUsername(), user.getId(), LOGIN_FAIL, "密码错误", request);
            throw new BusinessException(401, "账号或密码错误");
        }

        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        recordLoginLog(dto.getUsername(), user.getId(), LOGIN_SUCCESS, null, request);

        String token = authTokenUtil.generateToken(user);
        LoginUserVO loginUser = new LoginUserVO(user.getId(), user.getUsername(), user.getRealName(), user.getAvatar());
        return new LoginVO(token, "Bearer", authTokenUtil.getExpireSeconds(), loginUser);
    }

    /**
     * @brief 用户退出登录。
     *
     * @param token 当前访问 Token。
     */
    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            invalidTokens.add(token);
        }
    }

    /**
     * @brief 校验 Token 并返回当前登录用户。
     *
     * @param token 当前访问 Token。
     * @return 当前登录用户。
     */
    @Override
    public TokenUserVO verifyToken(String token) {
        if (invalidTokens.contains(token)) {
            throw new BusinessException(401, "登录凭证已退出");
        }
        return authTokenUtil.parseToken(token);
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith(NOOP_PREFIX)) {
            return rawPassword.equals(storedPassword.substring(NOOP_PREFIX.length()));
        }
        if (storedPassword.startsWith(SHA256_PREFIX)) {
            return sha256(rawPassword).equalsIgnoreCase(storedPassword.substring(SHA256_PREFIX.length()));
        }
        return rawPassword.equals(storedPassword);
    }

    private String sha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("密码摘要算法不可用", exception);
        }
    }

    private void recordLoginLog(String username,
                                Long userId,
                                String loginStatus,
                                String failureReason,
                                HttpServletRequest request) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setUserId(userId);
        loginLog.setLoginStatus(loginStatus);
        loginLog.setFailureReason(failureReason);
        loginLog.setLoginIp(ClientIpUtil.getClientIp(request));
        loginLog.setUserAgent(request.getHeader("User-Agent"));
        loginLog.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(loginLog);
    }
}
