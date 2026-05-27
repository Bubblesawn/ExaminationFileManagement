package com.exam.record.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * @brief 客户端 IP 工具。
 */
public final class ClientIpUtil {
    private static final String UNKNOWN = "unknown";

    private ClientIpUtil() {
    }

    /**
     * @brief 从代理头和请求连接信息中提取客户端 IP。
     *
     * @param request HTTP 请求对象。
     * @return 客户端 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = firstValid(request.getHeader("X-Forwarded-For"));
        if (!StringUtils.hasText(ip)) {
            ip = firstValid(request.getHeader("X-Real-IP"));
        }
        if (!StringUtils.hasText(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static String firstValid(String value) {
        if (!StringUtils.hasText(value) || UNKNOWN.equalsIgnoreCase(value)) {
            return null;
        }
        String first = value.split(",")[0].trim();
        return UNKNOWN.equalsIgnoreCase(first) ? null : first;
    }
}
