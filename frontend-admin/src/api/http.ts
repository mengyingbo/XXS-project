import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

const TOKEN_KEY = 'xxs_admin_token'

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
  baseURL: '/api/admin',
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
      const err = new Error(body.message || '请求失败') as Error & { bizCode?: number }
      err.bizCode = body.code
      // 后端业务错误统一 HTTP 200 + code≠0：全局弹出提示（登录页自己内联展示，跳过避免重复）
      const reqUrl: string = resp.config?.url ?? ''
      if (reqUrl !== '/login') {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(err)
    }
    return resp
  },
  (error) => {
    const status = error?.response?.status
    const body = error?.response?.data as ApiResult<unknown> | undefined
    const reqUrl: string = error?.config?.url ?? ''
    if (status === 401 && reqUrl !== '/login') {
      // 仅会话过期/未授权时回登录页；登录接口本身的 401 是密码错误，留在登录页提示
      clearToken()
      if (!location.hash.startsWith('#/login')) {
        location.href = '/admin/'
      }
    }
    const message = body?.message || error.message || '网络异常，请稍后再试'
    const err = new Error(message) as Error & { bizCode?: number; status?: number }
    err.status = status
    err.bizCode = body?.code
    // 统一弹出错误提示（调用方无需重复提示）
    ElMessage.error(message)
    return Promise.reject(err)
  }
)

/** 对齐后端统一响应体，返回 data 部分 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const resp = await http.request(config)
  return (resp.data as ApiResult<T>).data
}

export default http
