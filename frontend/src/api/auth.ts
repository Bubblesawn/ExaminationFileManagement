import { http } from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginUser {
  id: number
  username: string
  realName: string
  avatar: string | null
  roleCodes: string[]
  permissions: string[]
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: LoginUser
}

/**
 * @brief 调用后端登录接口，换取访问 Token 和当前用户信息。
 *
 * @param payload 登录账号和密码。
 * @return 登录成功后的 Token、过期时间和用户资料。
 */
export function login(payload: LoginRequest) {
  return http.post<LoginResponse, LoginResponse>('/auth/login', payload)
}
