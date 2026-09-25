<template>
  <div class="page page-no-tab select-page">
    <header class="hero">
      <div class="hero-emoji">📖✨</div>
      <h1 class="hero-title">语文知识闯关</h1>
      <p class="hero-sub">选一个你的档案，开始今天的冒险吧！</p>
    </header>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      正在打开档案册…
    </div>

    <template v-else>
      <div class="grid">
        <button
          v-for="c in profiles"
          :key="c.id"
          class="profile-card card"
          @click="pick(c)"
        >
          <AvatarFace :avatar="c.avatar" size="lg" />
          <span class="nick">{{ c.nickname }}</span>
          <span class="go">点我进入 →</span>
        </button>

        <button class="profile-card add-card" @click="openCreate">
          <span class="add-ico">＋</span>
          <span class="nick">新建档案</span>
          <span class="go">第一次来？</span>
        </button>
      </div>
    </template>

    <!-- 新建档案弹窗 -->
    <div v-if="creating" class="mask" @click.self="closeCreate">
      <div class="modal">
        <h2 class="modal-title">建立我的档案</h2>

        <div class="field-label">选一个头像</div>
        <div class="avatar-pick">
          <button
            v-for="a in AVATARS"
            :key="a"
            class="avatar-opt"
            :class="{ on: form.avatar === a }"
            @click="form.avatar = a"
          >
            {{ a }}
          </button>
        </div>

        <div class="field-label" style="margin-top: 18px">我的昵称</div>
        <input
          v-model.trim="form.nickname"
          class="text-input"
          maxlength="10"
          placeholder="1～10 个字符"
        />

        <div class="field-label" style="margin-top: 18px">设置 4 位数字 PIN</div>
        <input
          v-model="form.pin"
          class="text-input pin-input"
          type="password"
          inputmode="numeric"
          maxlength="4"
          placeholder="4 位数字"
        />
        <input
          v-model="form.pin2"
          class="text-input pin-input"
          type="password"
          inputmode="numeric"
          maxlength="4"
          placeholder="再输一遍确认"
        />

        <p v-if="formError" class="form-error">{{ formError }}</p>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="closeCreate">取消</button>
          <button class="btn btn-primary" :disabled="submitting" @click="submit">
            {{ submitting ? '创建中…' : '创建并进入' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AvatarFace from '@/components/AvatarFace.vue'
import { childApi } from '@/api/child'
import { showToast } from '@/composables/useToast'
import type { ChildBrief } from '@/types/api'

const AVATARS = ['🦊', '🐼', '🦁', '🐯', '🐸', '🐵', '🦄', '🐰', '🐨', '🐷']

const router = useRouter()
const profiles = ref<ChildBrief[]>([])
const loading = ref(true)
const creating = ref(false)
const submitting = ref(false)
const formError = ref('')

const form = reactive({
  avatar: AVATARS[0],
  nickname: '',
  pin: '',
  pin2: ''
})

async function load() {
  loading.value = true
  try {
    profiles.value = await childApi.list()
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    loading.value = false
  }
}

function pick(c: ChildBrief) {
  router.push({ name: 'pin', query: { childId: String(c.id) } })
}

function openCreate() {
  form.avatar = AVATARS[0]
  form.nickname = ''
  form.pin = ''
  form.pin2 = ''
  formError.value = ''
  creating.value = true
}

function closeCreate() {
  creating.value = false
}

async function submit() {
  formError.value = ''
  if (!form.nickname) {
    formError.value = '先写一下昵称吧'
    return
  }
  if (form.nickname.length > 10) {
    formError.value = '昵称最长 10 个字符'
    return
  }
  if (!/^\d{4}$/.test(form.pin)) {
    formError.value = 'PIN 必须是 4 位数字'
    return
  }
  if (form.pin !== form.pin2) {
    formError.value = '两遍 PIN 输入不一致'
    return
  }
  submitting.value = true
  try {
    const created = await childApi.register({
      nickname: form.nickname,
      avatar: form.avatar,
      pin: form.pin
    })
    showToast('档案建好啦！', 'success')
    creating.value = false
    await load()
    router.push({ name: 'pin', query: { childId: String(created.id) } })
  } catch (e) {
    formError.value = (e as Error).message
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 26px 0 22px;
}

.hero-emoji {
  font-size: 44px;
}

.hero-title {
  font-size: 30px;
  font-weight: 800;
  margin-top: 6px;
}

.hero-sub {
  color: var(--text-sub);
  margin-top: 6px;
  font-size: 16px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  max-width: 720px;
  margin: 0 auto;
}

.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 16px;
  text-decoration: none;
  border: 3px solid transparent;
  transition: transform 0.1s ease, border-color 0.15s ease;
}

.profile-card:active {
  transform: scale(0.97);
}

.nick {
  font-size: 19px;
  font-weight: 700;
}

.go {
  font-size: 14px;
  color: var(--primary);
  font-weight: 600;
}

.add-card {
  border-style: dashed;
  border-color: var(--border);
  box-shadow: none;
  background: #fbfcff;
}

.add-ico {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-pick {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.avatar-opt {
  min-height: var(--min-tap);
  border-radius: 14px;
  background: #f3f6fd;
  font-size: 26px;
  border: 2px solid transparent;
}

.avatar-opt.on {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.pin-input {
  margin-bottom: 12px;
  letter-spacing: 8px;
  text-align: center;
  font-size: 22px;
}

.form-error {
  color: var(--danger);
  font-size: 15px;
  font-weight: 600;
  margin: 4px 0 10px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.modal-actions .btn {
  flex: 1;
}
</style>
