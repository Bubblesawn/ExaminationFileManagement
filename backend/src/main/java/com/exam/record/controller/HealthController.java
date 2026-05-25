package com.exam.record.controller;

import com.exam.record.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @brief 系统健康检查接口。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * @brief 获取服务健康状态。
     *
     * @return 服务状态信息。
     */
    @GetMapping
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "UP", "service", "考籍管理系统后端"));
    }
}

