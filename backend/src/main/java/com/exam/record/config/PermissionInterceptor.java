package com.exam.record.config;

import com.exam.record.common.Result;
import com.exam.record.service.UserPermissionService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.TokenUserVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @brief 接口权限拦截器。
 *
 * @details
 * Token 认证通过后，根据请求路径匹配菜单权限码；未配置权限码的接口只要求登录，
 * 已配置权限码的接口要求当前用户拥有对应菜单权限。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, String> pathPermissions = new LinkedHashMap<>();
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    /**
     * @brief 构造接口权限拦截器。
     *
     * @param userPermissionService 用户权限查询服务。
     * @param objectMapper JSON 序列化组件。
     */
    public PermissionInterceptor(UserPermissionService userPermissionService, ObjectMapper objectMapper) {
        this.userPermissionService = userPermissionService;
        this.objectMapper = objectMapper;
        initPathPermissions();
    }

    /**
     * @brief 请求进入控制器前校验菜单权限。
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

        String requiredPermission = resolveRequiredPermission(request.getRequestURI());
        if (requiredPermission == null) {
            return true;
        }

        TokenUserVO user = AuthContextHolder.getUser();
        Set<String> permissions = userPermissionService.listPermissions(user == null ? null : user.getId());
        if (permissions.contains(requiredPermission)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(403, "无权访问该功能")));
        return false;
    }

    private String resolveRequiredPermission(String requestUri) {
        for (Map.Entry<String, String> entry : pathPermissions.entrySet()) {
            if (pathMatcher.match(entry.getKey(), requestUri)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void initPathPermissions() {
        pathPermissions.put("/api/system/users/**", "system:user:view");
        pathPermissions.put("/api/system/roles/**", "system:role:view");
        pathPermissions.put("/api/system/menus/**", "system:menu:view");
        pathPermissions.put("/api/system/logs/**", "system:log:view");
        pathPermissions.put("/api/dashboard/**", "dashboard:view");
        pathPermissions.put("/api/candidates/**", "candidate:view");
        pathPermissions.put("/api/records/**", "record:view");
        pathPermissions.put("/api/materials/**", "material:audit:view");
        pathPermissions.put("/api/material-types/**", "material:audit:view");
        pathPermissions.put("/api/exemptions/**", "exemption:view");
        pathPermissions.put("/api/course-replacements/**", "course-replace:view");
        pathPermissions.put("/api/transfers/**", "transfer:view");
        pathPermissions.put("/api/graduations/**", "graduation:view");
        pathPermissions.put("/api/ai/**", "ai:view");
    }
}
