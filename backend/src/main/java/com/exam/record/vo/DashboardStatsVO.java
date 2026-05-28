package com.exam.record.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief 工作台统计数据视图对象。
 *
 * @details
 * 汇总首页面板需要展示的核心业务数量，避免前端分别调用多个分页接口后再自行聚合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVO {
    /** @brief 考籍档案总数。 */
    private Long recordCount;

    /** @brief 待审核材料数量。 */
    private Long pendingMaterialCount;

    /** @brief 免考申请总数。 */
    private Long exemptionApplicationCount;

    /** @brief 毕业申请总数。 */
    private Long graduationApplicationCount;
}
