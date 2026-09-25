import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiResult } from '@/types/api'

const TOKEN_KEY = 'xxs_child_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0 && body.code !== 200) {
      // 业务错误：保留 http 状态码语义，直接抛业务消息
      const err = new Error(body.message || '请求失败') as Error & { bizCode?: number }
      err.bizCode = body.code
      return Promise.reject(err)
    }
    return resp
  },
  (error) => {
    // HTTP 层错误（401/403/409/500 等），后端统一体在 error.response.data
    const status = error?.response?.status
    const body = error?.response?.data as ApiResult<unknown> | undefined
    if (status === 401) {
      clearToken()
      // 避免在登录页循环跳转
      if (!location.pathname.startsWith('/pin') && location.pathname !== '/') {
        location.replace('/')
      }
    }
    const message = body?.message || error.message || '网络异常，请稍后再试'
    const err = new Error(message) as Error & { bizCode?: number; status?: number }
    err.status = status
    err.bizCode = body?.code
    return Promise.reject(err)
  }
)

/** 对齐后端统一响应体，返回 data 部分 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const resp = await http.request(config)
  return (resp.data as ApiResult<T>).data
}

export default http
