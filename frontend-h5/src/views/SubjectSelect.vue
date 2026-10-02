<template>
  <div class="page page-no-tab subject-page">
    <button class="back" @click="switchChild">← 换个档案</button>

    <div class="who" v-if="store.child">
      <AvatarFace :avatar="store.child.avatar" size="lg" />
      <div class="nick">{{ store.child.nickname }}</div>
    </div>

    <h1 class="title">今天想学什么呀？</h1>
    <p class="hint">语文、数学、英语的闯关进度是分开的哦</p>

    <div class="cards">
      <button class="card card-cn" @click="pick('chinese')">
        <span class="card-ico">📖</span>
        <span class="card-name">语文</span>
        <span class="card-sub">四年级上册 · 人教版</span>
      </button>

      <button class="card card-math" @click="pick('math')">
        <span class="card-ico">🧮</span>
        <span class="card-name">数学</span>
        <span class="card-sub">四年级上册 · 北师大版</span>
      </button>

      <button class="card card-en" @click="pick('english')">
        <span class="card-ico">🔤</span>
        <span class="card-name">英语</span>
        <span class="card-sub">四年级上册 · 人教版 PEP</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import AvatarFace from '@/components/AvatarFace.vue'
import { useChildStore } from '@/stores/child'

const router = useRouter()
const store = useChildStore()

function pick(s: 'chinese' | 'math' | 'english') {
  store.setSubject(s)
  router.replace('/map')
}

function switchChild() {
  store.clearSubject()
  store.logout()
  router.replace('/')
}
</script>

<style scoped>
.subject-page {
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
  margin: 6px 0 4px;
}

.nick {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 700;
}

.title {
  font-size: 24px;
  font-weight: 800;
  margin: 20px 0 8px;
}

.hint {
  font-size: 15px;
  color: var(--text-sub);
  margin-bottom: 26px;
}

.cards {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 30px 16px 24px;
  border-radius: 24px;
  border: 1px solid var(--border);
  background: var(--card-2);
  transition: transform 0.12s ease;
}

.card:active {
  transform: scale(0.97);
}

.card-cn {
  background: linear-gradient(160deg, rgba(123, 227, 56, 0.16), var(--card-2) 70%);
}

.card-math {
  background: linear-gradient(160deg, rgba(64, 158, 255, 0.18), var(--card-2) 70%);
}

.card-en {
  background: linear-gradient(160deg, rgba(178, 108, 255, 0.18), var(--card-2) 70%);
}

.card-ico {
  font-size: 52px;
  line-height: 1;
}

.card-name {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-main);
}

.card-sub {
  font-size: 14px;
  color: var(--text-sub);
}
</style>
