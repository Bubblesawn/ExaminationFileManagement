package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.service.DashboardService;
import com.exam.record.vo.DashboardStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @brief 工作台接口。
 *
 * @details
 * 提供首页面板统计数据，供前端工作台展示当前业务概览。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * @brief 构造工作台控制器。
     *
     * @param dashboardService 工作台统计服务。
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * @brief 查询工作台面板统计数据。
     *
     * @return 工作台统计数据。
     */
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
