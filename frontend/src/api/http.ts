import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAuthorizationHeader } from '../utils/authToken'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const authorization = getAuthorizationHeader()
  if (authorization) {
    config.headers.Authorization = authorization
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (typeof result?.code === 'number' && result.code !== 200) {
      return Promise.reject(new Error(result.message || '请求处理失败'))
    }
    return result?.data ?? response.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    if (error.config?.url !== '/auth/login') {
      ElMessage.error(message)
    }
    return Promise.reject(new Error(message))
  }
)
