package com.exam.record.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * @brief Web MVC 基础配置。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final PermissionInterceptor permissionInterceptor;
    private final OperationLogInterceptor operationLogInterceptor;
    private final Path materialUploadPath;

    /**
     * @brief 构造 Web MVC 配置。
     *
     * @param authInterceptor Token 认证拦截器。
     * @param permissionInterceptor 接口权限拦截器。
     * @param operationLogInterceptor 操作日志拦截器。
     */
    public WebConfig(
            AuthInterceptor authInterceptor,
            PermissionInterceptor permissionInterceptor,
            OperationLogInterceptor operationLogInterceptor,
            @Value("${material.upload.root:uploads/materials}") String materialUploadRoot) {
        this.authInterceptor = authInterceptor;
        this.permissionInterceptor = permissionInterceptor;
        this.operationLogInterceptor = operationLogInterceptor;
        this.materialUploadPath = Path.of(materialUploadRoot).toAbsolutePath().normalize();
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
     * @brief 暴露材料上传目录的静态访问路径。
     *
     * @param registry 静态资源注册器。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/materials/**")
                .addResourceLocations(materialUploadPath.toUri().toString() + "/");
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
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/me",
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
