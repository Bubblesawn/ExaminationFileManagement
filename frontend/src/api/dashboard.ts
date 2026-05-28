import { http } from './http'

export interface DashboardStats {
  recordCount: number
  pendingMaterialCount: number
  exemptionApplicationCount: number
  graduationApplicationCount: number
}

/**
 * @brief 查询工作台面板统计数据。
 *
 * @return 工作台核心业务统计数据。
 */
export function getDashboardStats() {
  return http.get('/dashboard/stats') as unknown as Promise<DashboardStats>
}
