import { defineStore } from 'pinia'
import { ref } from 'vue'
import { childApi } from '@/api/child'
import { clearToken, setToken } from '@/api/http'
import type { ChildBrief, SessionSubmit, TodayUsage } from '@/types/api'

const CHILD_KEY = 'xxs_child_info'

function loadChild(): ChildBrief | null {
  const raw = localStorage.getItem(CHILD_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as ChildBrief
  } catch {
    return null
  }
}

export const useChildStore = defineStore('child', () => {
  const child = ref<ChildBrief | null>(loadChild())
  const points = ref(0)
  const today = ref<TodayUsage | null>(null)
  // 最近一次关卡结算（供结算页使用；刷新即失效，回地图重新拉取）
  const lastResult = ref<SessionSubmit | null>(null)
  const lastLevelName = ref('')

  function saveLogin(token: string, info: ChildBrief) {
    setToken(token)
    child.value = info
    localStorage.setItem(CHILD_KEY, JSON.stringify(info))
  }

  function logout() {
    clearToken()
    localStorage.removeItem(CHILD_KEY)
    child.value = null
    points.value = 0
    today.value = null
    lastResult.value = null
  }

  async function fetchProfile() {
    const data = await childApi.profile()
    child.value = data.child
    points.value = data.points
    today.value = data.today
    localStorage.setItem(CHILD_KEY, JSON.stringify(data.child))
    return data
  }

  function setToday(usage: TodayUsage) {
    today.value = usage
  }

  function setLastResult(result: SessionSubmit, levelName: string) {
    lastResult.value = result
    lastLevelName.value = levelName
    points.value = result.points
    today.value = result.today
  }

  return {
    child,
    points,
    today,
    lastResult,
    lastLevelName,
    saveLogin,
    logout,
    fetchProfile,
    setToday,
    setLastResult
  }
})
