package com.exam.record.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * @brief 后端 HTTP 客户端配置。
 *
 * @details
 * 为智能辅助服务调用提供统一的 RestTemplate 实例，并设置基础连接和读取超时时间。
 */
@Configuration
public class RestClientConfig {

    /**
     * @brief 构造 RestTemplate 客户端。
     *
     * @param builder Spring Boot RestTemplate 构造器。
     * @return 已配置超时时间的 RestTemplate 实例。
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
