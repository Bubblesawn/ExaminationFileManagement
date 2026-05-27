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
