import { reactive } from 'vue'

export type ToastType = 'info' | 'success' | 'error'

interface ToastItem {
  id: number
  message: string
  type: ToastType
}

export const toasts = reactive<ToastItem[]>([])

let seed = 0

export function showToast(message: string, type: ToastType = 'info', durationMs = 2200) {
  const id = ++seed
  toasts.push({ id, message, type })
  window.setTimeout(() => {
    const idx = toasts.findIndex((t) => t.id === id)
    if (idx >= 0) toasts.splice(idx, 1)
  }, durationMs)
}
