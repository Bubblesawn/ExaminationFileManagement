package com.exam.record.config;

import com.exam.record.entity.SysOperationLog;
import com.exam.record.service.SysLogService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.util.ClientIpUtil;
import com.exam.record.vo.TokenUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * @brief 系统操作日志拦截器。
 *
 * @details
 * 对已认证的内部接口请求记录操作模块、请求方法、操作人、业务结果和耗时。
 * 登录日志由认证业务单独记录，本拦截器会跳过日志查询接口以避免审计数据自增噪音。
 */
@Component
public class OperationLogInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTRIBUTE = "operationLogStartTime";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";
    private static final int MAX_PARAM_LENGTH = 2000;
    private static final int MAX_ERROR_LENGTH = 2000;

    private final SysLogService sysLogService;

    /**
     * @brief 构造系统操作日志拦截器。
     *
     * @param sysLogService 系统日志业务服务。
     */
    public OperationLogInterceptor(SysLogService sysLogService) {
        this.sysLogService = sysLogService;
    }

    /**
     * @brief 请求进入控制器前记录开始时间。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @param handler 处理器对象。
     * @return 是否继续执行后续处理。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    /**
     * @brief 请求完成后写入操作日志。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @param handler 处理器对象。
     * @param ex 执行异常。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            sysLogService.recordOperationLog(buildOperationLog(request, response, ex));
        } catch (Exception ignored) {
            // 操作日志写入失败不能影响主业务响应。
        }
    }

    private SysOperationLog buildOperationLog(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        SysOperationLog operationLog = new SysOperationLog();
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long now = System.currentTimeMillis();
        operationLog.setModuleName(resolveModuleName(request.getRequestURI()));
        operationLog.setOperationType(resolveOperationType(request.getMethod()));
        operationLog.setOperationDesc(resolveOperationDesc(request.getMethod(), request.getRequestURI()));
        operationLog.setRequestMethod(request.getMethod());
        operationLog.setRequestUri(request.getRequestURI());
        operationLog.setRequestParam(clip(buildRequestParam(request), MAX_PARAM_LENGTH));
        operationLog.setResponseResult((String) request.getAttribute(OperationLogAdvice.RESPONSE_BODY_ATTRIBUTE));
        operationLog.setOperationStatus(resolveOperationStatus(request, response, ex));
        operationLog.setErrorMessage(clip(ex == null ? null : ex.getMessage(), MAX_ERROR_LENGTH));
        fillOperator(operationLog);
        operationLog.setOperationIp(ClientIpUtil.getClientIp(request));
        operationLog.setOperationTime(LocalDateTime.now());
        operationLog.setCostTime(startTime == null ? 0L : now - startTime);
        return operationLog;
    }

    private void fillOperator(SysOperationLog operationLog) {
        TokenUserVO currentUser = AuthContextHolder.getUser();
        if (currentUser != null) {
            operationLog.setOperatorId(currentUser.getId());
            operationLog.setOperatorName(StringUtils.hasText(currentUser.getRealName())
                    ? currentUser.getRealName()
                    : currentUser.getUsername());
        }
    }

    private String resolveOperationStatus(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        Object businessSuccess = request.getAttribute(OperationLogAdvice.BUSINESS_SUCCESS_ATTRIBUTE);
        if (businessSuccess instanceof Boolean success) {
            return success ? STATUS_SUCCESS : STATUS_FAIL;
        }
        return ex == null && response.getStatus() < HttpServletResponse.SC_BAD_REQUEST ? STATUS_SUCCESS : STATUS_FAIL;
    }

    private String buildRequestParam(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return StringUtils.hasText(queryString) ? queryString : null;
    }

    private String resolveModuleName(String uri) {
        if (uri.startsWith("/api/system/users")) {
            return "用户管理";
        }
        if (uri.startsWith("/api/system/roles")) {
            return "角色管理";
        }
        if (uri.startsWith("/api/system/menus")) {
            return "菜单管理";
        }
        if (uri.startsWith("/api/auth")) {
            return "登录认证";
        }
        if (uri.startsWith("/api/candidates")) {
            return "考生管理";
        }
        return "系统接口";
    }

    private String resolveOperationType(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            case "GET" -> "QUERY";
            default -> method;
        };
    }

    private String resolveOperationDesc(String method, String uri) {
        return method + " " + uri;
    }

    private String clip(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
