package com.exam.record.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.exam.record.common.BusinessException;
import com.exam.record.common.Result;
import com.exam.record.service.AuthService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.TokenUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * @brief Token 认证拦截器。
 *
 * @details
 * 统一拦截内部业务接口，提取 Authorization 请求头中的 Bearer Token，
 * 校验通过后写入当前请求用户上下文，校验失败时返回统一错误响应。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * @brief 构造 Token 认证拦截器。
     *
     * @param authService 登录认证业务服务。
     * @param objectMapper JSON 序列化组件。
     */
    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * @brief 请求进入控制器前校验 Token。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @param handler 处理器对象。
     * @return 是否继续执行后续处理。
     * @throws Exception JSON 写出异常。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        try {
            TokenUserVO user = authService.verifyToken(extractToken(request.getHeader("Authorization")));
            AuthContextHolder.setUser(user);
            return true;
        } catch (BusinessException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(exception.getCode(), exception.getMessage())));
            return false;
        }
    }

    /**
     * @brief 请求完成后清理当前用户上下文。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @param handler 处理器对象。
     * @param ex 执行异常。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return authorization;
    }
}
