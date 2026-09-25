<template>
  <div class="page page-no-tab pin-page">
    <button class="back" @click="router.push('/')">← 换个档案</button>

    <div class="who" v-if="target">
      <AvatarFace :avatar="target.avatar" size="lg" />
      <div class="nick">{{ target.nickname }}</div>
    </div>
    <div class="who" v-else-if="!loading">
      <div class="nick">档案不存在</div>
    </div>

    <h1 class="title">输入 4 位 PIN</h1>

    <div class="dots" :class="{ shake: shake }">
      <span v-for="i in 4" :key="i" class="dot" :class="{ on: pin.length >= i }"></span>
    </div>

    <p v-if="locked" class="msg msg-lock">🔒 {{ lockMessage }}</p>
    <p v-else-if="message" class="msg msg-err">{{ message }}</p>
    <p v-else class="msg msg-hint">只有答对 PIN 才能进入哦</p>

    <div class="keypad" v-if="!locked">
      <button
        v-for="k in keys"
        :key="k"
        class="key"
        :class="{ 'key-fn': k === 'del', 'key-blank': k === '' }"
        :disabled="submitting"
        @click="tap(k)"
      >
        <template v-if="k === 'del'">⌫</template>
        <template v-else>{{ k }}</template>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AvatarFace from '@/components/AvatarFace.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import type { ChildBrief } from '@/types/api'

const route = useRoute()
const router = useRouter()
const store = useChildStore()

const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del']

const target = ref<ChildBrief | null>(null)
const loading = ref(true)
const pin = ref('')
const message = ref('')
const shake = ref(false)
const submitting = ref(false)
const locked = ref(false)
const lockMessage = ref('')
let lockTimer: number | undefined
let remainSeconds = 0

async function loadTarget() {
  const childId = Number(route.query.childId)
  if (!childId) {
    loading.value = false
    return
  }
  try {
    const list = await childApi.list()
    target.value = list.find((c) => c.id === childId) ?? null
  } catch (e) {
    message.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function tap(k: string | number) {
  if (submitting.value || locked.value) return
  if (k === 'del') {
    pin.value = pin.value.slice(0, -1)
    message.value = ''
    return
  }
  if (k === '') return
  if (pin.value.length >= 4) return
  pin.value += String(k)
  message.value = ''
  if (pin.value.length === 4) {
    void submit()
  }
}

function fail(msg: string) {
  message.value = msg
  pin.value = ''
  shake.value = true
  window.setTimeout(() => (shake.value = false), 400)
  // 消息含"锁定"时进入锁定态（后端：连续 5 次锁 1 分钟）
  if (msg.includes('锁定') || msg.includes('请等')) {
    startLock(msg)
  }
}

function startLock(msg: string) {
  locked.value = true
  const match = /等\s*(\d+)\s*秒/.exec(msg)
  remainSeconds = match ? Number(match[1]) : 60
  renderLock()
  window.clearInterval(lockTimer)
  lockTimer = window.setInterval(() => {
    remainSeconds -= 1
    if (remainSeconds <= 0) {
      window.clearInterval(lockTimer)
      locked.value = false
      pin.value = ''
      message.value = ''
    } else {
      renderLock()
    }
  }, 1000)
}

function renderLock() {
  lockMessage.value = `先休息一下，${remainSeconds} 秒后再试吧`
}

async function submit() {
  if (!target.value) return
  submitting.value = true
  try {
    const data = await childApi.login({ childId: target.value.id, pin: pin.value })
    store.saveLogin(data.token, data.child)
    router.replace('/map')
  } catch (e) {
    fail((e as Error).message)
  } finally {
    submitting.value = false
  }
}

onMounted(loadTarget)
onBeforeUnmount(() => window.clearInterval(lockTimer))
</script>

<style scoped>
.pin-page {
  max-width: 480px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 24px;
}

.back {
  align-self: flex-start;
  min-height: var(--min-tap);
  color: var(--text-sub);
  font-size: 16px;
  font-weight: 600;
}

.who {
  text-align: center;
  margin: 10px 0 6px;
}

.nick {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin: 18px 0 18px;
}

.dots {
  display: flex;
  gap: 22px;
  margin-bottom: 14px;
}

.dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 3px solid #c4cde4;
  background: transparent;
  transition: all 0.12s ease;
}

.dot.on {
  background: var(--primary);
  border-color: var(--primary);
  transform: scale(1.08);
}

.shake {
  animation: shake 0.4s ease;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-8px);
  }
  75% {
    transform: translateX(8px);
  }
}

.msg {
  min-height: 28px;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  padding: 0 16px;
}

.msg-hint {
  color: var(--text-sub);
}

.msg-err {
  color: var(--danger);
}

.msg-lock {
  color: #c97c00;
}

.keypad {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  width: 100%;
  max-width: 340px;
}

.key {
  min-height: 64px;
  border-radius: 18px;
  background: #fff;
  box-shadow: var(--shadow-card);
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
}

.key:active {
  background: var(--primary-soft);
  transform: scale(0.96);
}

.key-fn {
  font-size: 24px;
  color: var(--text-sub);
  box-shadow: none;
  background: transparent;
}

.key-blank {
  box-shadow: none;
  background: transparent;
  visibility: hidden;
}

.key:disabled {
  opacity: 0.6;
}
</style>
