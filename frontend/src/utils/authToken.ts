import type { LoginResponse } from '../api/auth'

const TOKEN_KEY = 'exam-record-token'
const TOKEN_TYPE_KEY = 'exam-record-token-type'
const USER_KEY = 'exam-record-user'

/**
 * @brief 保存登录返回的 Token 和用户资料。
 *
 * @param loginResult 后端登录接口返回的数据。
 */
export function saveLoginResult(loginResult: LoginResponse) {
  localStorage.setItem(TOKEN_KEY, loginResult.token)
  localStorage.setItem(TOKEN_TYPE_KEY, loginResult.tokenType || 'Bearer')
  localStorage.setItem(USER_KEY, JSON.stringify(loginResult.user))
}

/**
 * @brief 读取当前访问 Token。
 *
 * @return 本地保存的 Token；未登录时返回 null。
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * @brief 读取当前登录用户资料。
 *
 * @return 当前登录用户资料；未登录或本地数据异常时返回 null。
 */
export function getLoginUser() {
  const userText = localStorage.getItem(USER_KEY)
  if (!userText) {
    return null
  }

  try {
    return JSON.parse(userText) as LoginResponse['user']
  } catch {
    return null
  }
}

/**
 * @brief 判断当前登录用户是否拥有指定菜单权限。
 *
 * @param permission 菜单权限码。
 * @return 是否拥有该权限。
 */
export function hasPermission(permission?: string) {
  if (!permission) {
    return true
  }

  const user = getLoginUser()
  return Boolean(user?.permissions?.includes(permission))
}

/**
 * @brief 读取带 Token 类型的认证请求头内容。
 *
 * @return Authorization 请求头值；未登录时返回 null。
 */
export function getAuthorizationHeader() {
  const token = getToken()
  if (!token) {
    return null
  }

  const tokenType = localStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'
  return `${tokenType} ${token}`
}

/**
 * @brief 清理本地登录凭证和用户资料。
 */
export function clearLoginResult() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_TYPE_KEY)
  localStorage.removeItem(USER_KEY)
}
