package com.exam.record.service;

import com.exam.record.vo.DashboardStatsVO;

/**
 * @brief 工作台统计服务。
 */
public interface DashboardService {
    /**
     * @brief 查询工作台面板统计数据。
     *
     * @return 工作台统计数据。
     */
    DashboardStatsVO getStats();
}
