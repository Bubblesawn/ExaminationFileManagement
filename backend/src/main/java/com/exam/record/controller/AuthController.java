package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.dto.LoginDTO;
import com.exam.record.service.AuthService;
import com.exam.record.vo.LoginVO;
import com.exam.record.vo.TokenUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @brief 登录认证接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    /**
     * @brief 构造登录认证控制器。
     *
     * @param authService 登录认证业务服务。
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * @brief 登录接口。
     *
     * @param dto 登录请求参数。
     * @param request HTTP 请求对象。
     * @return 登录 Token 和用户信息。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success(authService.login(dto, request));
    }

    /**
     * @brief 退出接口。
     *
     * @param authorization Authorization 请求头。
     * @return 退出结果。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return Result.success();
    }

    /**
     * @brief Token 校验接口。
     *
     * @param authorization Authorization 请求头。
     * @return 当前登录用户信息。
     */
    @GetMapping("/me")
    public Result<TokenUserVO> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(authService.verifyToken(extractToken(authorization)));
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return authorization;
    }
}
