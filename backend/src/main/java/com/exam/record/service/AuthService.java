package com.exam.record.service;

import com.exam.record.dto.LoginDTO;
import com.exam.record.vo.LoginVO;
import com.exam.record.vo.TokenUserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @brief 登录认证业务接口。
 */
public interface AuthService {

    /**
     * @brief 用户登录。
     *
     * @param dto 登录请求参数。
     * @param request HTTP 请求对象。
     * @return 登录成功响应。
     */
    LoginVO login(LoginDTO dto, HttpServletRequest request);

    /**
     * @brief 用户退出登录。
     *
     * @param token 当前访问 Token。
     */
    void logout(String token);

    /**
     * @brief 校验 Token 并返回当前登录用户。
     *
     * @param token 当前访问 Token。
     * @return 当前登录用户。
     */
    TokenUserVO verifyToken(String token);
}
