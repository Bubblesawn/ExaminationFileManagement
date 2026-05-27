import { http } from './http'

export type UserStatus = 'ENABLED' | 'DISABLED'

export interface SystemUser {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  status: UserStatus
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UserPageParams {
  pageNo: number
  pageSize: number
  keyword?: string
  status?: UserStatus | ''
}

export interface UserCreatePayload {
  username: string
  password: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  status?: UserStatus
}

export interface UserUpdatePayload {
  realName: string
  phone?: string
  email?: string
  avatar?: string
  status?: UserStatus
}

/**
 * @brief 分页查询系统用户列表。
 *
 * @param params 用户分页查询条件。
 * @return 用户分页数据。
 */
export function pageSystemUsers(params: UserPageParams) {
  return http.get<PageResult<SystemUser>, PageResult<SystemUser>>('/system/users/page', { params })
}

/**
 * @brief 创建系统用户。
 *
 * @param data 新增用户表单数据。
 * @return 创建后的用户资料。
 */
export function createSystemUser(data: UserCreatePayload) {
  return http.post<SystemUser, SystemUser>('/system/users', data)
}

/**
 * @brief 修改系统用户资料。
 *
 * @param id 用户主键。
 * @param data 用户编辑表单数据。
 * @return 修改后的用户资料。
 */
export function updateSystemUser(id: number, data: UserUpdatePayload) {
  return http.put<SystemUser, SystemUser>(`/system/users/${id}`, data)
}

/**
 * @brief 启用系统用户。
 *
 * @param id 用户主键。
 * @return 启用后的用户资料。
 */
export function enableSystemUser(id: number) {
  return http.put<SystemUser, SystemUser>(`/system/users/${id}/enable`)
}

/**
 * @brief 禁用系统用户。
 *
 * @param id 用户主键。
 * @return 禁用后的用户资料。
 */
export function disableSystemUser(id: number) {
  return http.put<SystemUser, SystemUser>(`/system/users/${id}/disable`)
}

/**
 * @brief 重置系统用户登录密码。
 *
 * @param id 用户主键。
 * @param password 新登录密码。
 */
export function resetSystemUserPassword(id: number, password: string) {
  return http.put<void, void>(`/system/users/${id}/reset-password`, { password })
}
