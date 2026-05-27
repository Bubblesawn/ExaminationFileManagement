package com.exam.record.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @brief Web MVC 基础配置。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final OperationLogInterceptor operationLogInterceptor;

    /**
     * @brief 构造 Web MVC 配置。
     *
     * @param authInterceptor Token 认证拦截器。
     * @param operationLogInterceptor 操作日志拦截器。
     */
    public WebConfig(AuthInterceptor authInterceptor, OperationLogInterceptor operationLogInterceptor) {
        this.authInterceptor = authInterceptor;
        this.operationLogInterceptor = operationLogInterceptor;
    }

    /**
     * @brief 配置跨域访问规则，便于前端开发环境联调。
     *
     * @param registry 跨域注册器。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * @brief 注册 Token 认证拦截规则。
     *
     * @param registry 拦截器注册器。
     */
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/webjars/**",
                        "/favicon.ico"
                );
        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/health",
                        "/api/system/logs/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/webjars/**",
                        "/favicon.ico"
                );
    }
}
