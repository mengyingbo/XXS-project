import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminApi } from '@/api/admin'
import { clearToken, setToken } from '@/api/http'
import type { AdminInfo } from '@/types/api'

const ADMIN_KEY = 'xxs_admin_info'

function loadAdmin(): AdminInfo | null {
  const raw = localStorage.getItem(ADMIN_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AdminInfo
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const admin = ref<AdminInfo | null>(loadAdmin())

  const mustChangePwd = () => admin.value?.mustChangePwd === true

  async function login(username: string, password: string) {
    const data = await adminApi.login({ username, password })
    setToken(data.token)
    admin.value = data.admin
    localStorage.setItem(ADMIN_KEY, JSON.stringify(data.admin))
    return data
  }

  function logout() {
    clearToken()
    localStorage.removeItem(ADMIN_KEY)
    admin.value = null
  }

  /** 改密成功后更新本地标记 */
  function markPwdChanged() {
    if (admin.value) {
      admin.value.mustChangePwd = false
      localStorage.setItem(ADMIN_KEY, JSON.stringify(admin.value))
    }
  }

  return { admin, mustChangePwd, login, logout, markPwdChanged }
})
