package com.exam.record.config;

import com.exam.record.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @brief 操作日志响应采集器。
 *
 * @details
 * 在响应写出前提取统一响应对象的业务状态和摘要，供操作日志拦截器在请求结束时落库。
 */
@ControllerAdvice
public class OperationLogAdvice implements ResponseBodyAdvice<Object> {
    public static final String RESPONSE_BODY_ATTRIBUTE = "operationLogResponseBody";
    public static final String BUSINESS_SUCCESS_ATTRIBUTE = "operationLogBusinessSuccess";
    private static final int MAX_RESPONSE_LENGTH = 2000;

    /**
     * @brief 判断当前响应是否需要采集。
     *
     * @param returnType 控制器返回类型。
     * @param converterType 响应转换器类型。
     * @return 是否启用响应采集。
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * @brief 响应写出前保存业务结果摘要。
     *
     * @param body 响应体。
     * @param returnType 控制器返回类型。
     * @param selectedContentType 响应内容类型。
     * @param selectedConverterType 响应转换器类型。
     * @param request 请求对象。
     * @param response 响应对象。
     * @return 原始响应体。
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            if (body instanceof Result<?> result) {
                httpRequest.setAttribute(BUSINESS_SUCCESS_ATTRIBUTE, Integer.valueOf(200).equals(result.getCode()));
                httpRequest.setAttribute(RESPONSE_BODY_ATTRIBUTE, clip(result.getMessage()));
            }
        }
        return body;
    }

    private String clip(String value) {
        if (value == null || value.length() <= MAX_RESPONSE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_RESPONSE_LENGTH);
    }
}
