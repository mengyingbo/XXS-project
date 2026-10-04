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

/** 会话失效：清 token 并回档案选择页（后端过期返回 HTTP 200 + code 401，需两处都处理） */
function handleUnauthorized() {
  clearToken()
  // hash 路由下路径在 location.hash；避免在主页/档案选择/PIN 页循环跳转
  const routePath = location.hash ? location.hash.replace(/^#/, '') : location.pathname
  if (routePath !== '/pin' && routePath !== '/profiles' && routePath !== '/' && routePath !== '') {
    location.replace('/#/profiles')
  }
}

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0 && body.code !== 200) {
      // 业务码 401 = token 过期/无效（PIN 错误是 403，不在此列）
      if (body.code === 401) {
        handleUnauthorized()
      }
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
      handleUnauthorized()
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
