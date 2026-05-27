import { http } from './http'

export type LogStatus = 'SUCCESS' | 'FAIL'

export interface PageResult<T> {
  records?: T[]
  total?: number
  size?: number
  current?: number
  pages?: number
}

export interface LoginLogQuery {
  pageNo: number
  pageSize: number
  username?: string
  loginStatus?: LogStatus | ''
}

export interface OperationLogQuery {
  pageNo: number
  pageSize: number
  moduleName?: string
  operatorName?: string
  operationStatus?: LogStatus | ''
}

export interface LoginLogRecord {
  id: number
  username: string
  userId?: number
  loginStatus: LogStatus
  failureReason?: string
  loginIp?: string
  userAgent?: string
  loginTime?: string
}

export interface OperationLogRecord {
  id: number
  moduleName?: string
  operationType?: string
  operationDesc?: string
  requestMethod?: string
  requestUri?: string
  requestParam?: string
  responseResult?: string
  operationStatus: LogStatus
  errorMessage?: string
  operatorId?: number
  operatorName?: string
  operationIp?: string
  operationTime?: string
  costTime?: number
}

/**
 * @brief 分页查询登录日志。
 *
 * @param params 登录日志分页查询条件。
 * @return 登录日志分页结果。
 */
export function pageLoginLogs(params: LoginLogQuery) {
  return http.get<PageResult<LoginLogRecord>, PageResult<LoginLogRecord>>('/system/logs/login/page', { params })
}

/**
 * @brief 查询登录日志详情。
 *
 * @param id 登录日志主键。
 * @return 登录日志详情。
 */
export function getLoginLogDetail(id: number) {
  return http.get<LoginLogRecord, LoginLogRecord>(`/system/logs/login/${id}`)
}

/**
 * @brief 分页查询操作日志。
 *
 * @param params 操作日志分页查询条件。
 * @return 操作日志分页结果。
 */
export function pageOperationLogs(params: OperationLogQuery) {
  return http.get<PageResult<OperationLogRecord>, PageResult<OperationLogRecord>>('/system/logs/operation/page', { params })
}

/**
 * @brief 查询操作日志详情。
 *
 * @param id 操作日志主键。
 * @return 操作日志详情。
 */
export function getOperationLogDetail(id: number) {
  return http.get<OperationLogRecord, OperationLogRecord>(`/system/logs/operation/${id}`)
}
