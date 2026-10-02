import { defineStore } from 'pinia'
import { ref } from 'vue'
import { childApi } from '@/api/child'
import { clearToken, setToken } from '@/api/http'
import type { ChildBrief, SessionSubmit, TodayUsage } from '@/types/api'

const CHILD_KEY = 'xxs_child_info'
const SUBJECT_KEY = 'xxs_subject'

export type Subject = '' | 'chinese' | 'math' | 'english'

function loadChild(): ChildBrief | null {
  const raw = localStorage.getItem(CHILD_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as ChildBrief
  } catch {
    return null
  }
}

function loadSubject(): Subject {
  const raw = localStorage.getItem(SUBJECT_KEY)
  if (raw === 'math' || raw === 'chinese' || raw === 'english') return raw
  return ''
}

export const useChildStore = defineStore('child', () => {
  const child = ref<ChildBrief | null>(loadChild())
  // 当前科目（v2.0）：'' 表示尚未选择；选择后两科地图/进度/每日限额互相独立
  const subject = ref<Subject>(loadSubject())
  const points = ref(0)
  const streakDays = ref(0)
  const today = ref<TodayUsage | null>(null)
  // 最近一次关卡结算（供结算页使用；刷新即失效，回地图重新拉取）
  const lastResult = ref<SessionSubmit | null>(null)
  const lastLevelName = ref('')
  // 最近一局总用时（含错题回炉，前端计时；仅用于结算页展示）
  const lastDurationMs = ref(0)

  function saveLogin(token: string, info: ChildBrief) {
    setToken(token)
    child.value = info
    localStorage.setItem(CHILD_KEY, JSON.stringify(info))
  }

  function setSubject(s: Exclude<Subject, ''>) {
    subject.value = s
    localStorage.setItem(SUBJECT_KEY, s)
  }

  function clearSubject() {
    subject.value = ''
    localStorage.removeItem(SUBJECT_KEY)
  }

  function logout() {
    clearToken()
    localStorage.removeItem(CHILD_KEY)
    child.value = null
    points.value = 0
    streakDays.value = 0
    today.value = null
    lastResult.value = null
    clearSubject()
  }

  async function fetchProfile(sub?: Exclude<Subject, ''>) {
    const data = await childApi.profile(sub ?? (subject.value || 'chinese'))
    child.value = data.child
    points.value = data.points
    streakDays.value = data.streakDays
    today.value = data.today
    localStorage.setItem(CHILD_KEY, JSON.stringify(data.child))
    return data
  }

  function setToday(usage: TodayUsage) {
    today.value = usage
  }

  function setLastResult(result: SessionSubmit, levelName: string, durationMs = 0) {
    lastResult.value = result
    lastLevelName.value = levelName
    lastDurationMs.value = durationMs
    points.value = result.points
    today.value = result.today
  }

  return {
    child,
    subject,
    points,
    streakDays,
    today,
    lastResult,
    lastLevelName,
    lastDurationMs,
    saveLogin,
    setSubject,
    clearSubject,
    logout,
    fetchProfile,
    setToday,
    setLastResult
  }
})
