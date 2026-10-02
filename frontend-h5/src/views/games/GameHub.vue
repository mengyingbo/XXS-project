<template>
  <div class="page page-no-tab hub-page">
    <header class="hub-hero">
      <button class="back" @click="goBack">← 返回</button>
      <div class="hero-emoji">🕹️</div>
      <h1 class="hero-title">游戏乐园</h1>
      <p class="hero-sub">动动脑筋，休息一下吧！</p>
    </header>

    <div class="game-list">
      <button class="game-card" @click="go('games-klotski')">
        <span class="g-ico" style="background: rgba(123, 227, 56, 0.14)">🚂</span>
        <span class="g-info">
          <span class="g-name">华容道经典闯关</span>
          <span class="g-desc">传统华容道 + 路径规划 · 空间想象</span>
          <span class="g-prog">{{ store.cleared('klotski') }} 关 · ⭐ {{ store.stars('klotski') }}</span>
        </span>
        <span class="g-arrow">›</span>
      </button>

      <button class="game-card" @click="go('games-puzzle')">
        <span class="g-ico" style="background: rgba(64, 158, 255, 0.16)">🧩</span>
        <span class="g-info">
          <span class="g-name">拼图大师</span>
          <span class="g-desc">课本插画 · 自传照片 · 拖拽拼图</span>
          <span class="g-prog">{{ store.cleared('puzzle') }} 档 · ⭐ {{ store.stars('puzzle') }}</span>
        </span>
        <span class="g-arrow">›</span>
      </button>

      <button class="game-card" @click="go('games-snake')">
        <span class="g-ico" style="background: rgba(0, 221, 0, 0.14)">🐍</span>
        <span class="g-info">
          <span class="g-name">贪吃蛇</span>
          <span class="g-desc">Canvas 像素风 · 吃食物长大 · 越吃越快</span>
          <span class="g-prog">最高分 {{ store.data.snake.best }} 分</span>
        </span>
        <span class="g-arrow">›</span>
      </button>

      <button class="game-card" @click="go('games-sudoku')">
        <span class="g-ico" style="background: rgba(255, 193, 7, 0.14)">🔢</span>
        <span class="g-info">
          <span class="g-name">数独王者</span>
          <span class="g-desc">四宫 · 六宫 · 九宫 · 智能提示</span>
          <span class="g-prog">完成 {{ store.data.sudoku.played }} 局</span>
        </span>
        <span class="g-arrow">›</span>
      </button>

      <button class="game-card" @click="go('games-idiom')">
        <span class="g-ico" style="background: rgba(156, 100, 255, 0.16)">🐉</span>
        <span class="g-info">
          <span class="g-name">成语接龙</span>
          <span class="g-desc">接龙 · 填空 · 看图猜词 · 典故</span>
          <span class="g-prog">{{ store.cleared('idiom') }} 关 · ⭐ {{ store.stars('idiom') }}</span>
        </span>
        <span class="g-arrow">›</span>
      </button>
    </div>

    <button class="reset-link" @click="askReset">{{ resetArmed ? '再点一次确认清空' : '重置游戏进度' }}</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGamesStore } from '@/stores/games'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'

const router = useRouter()
const store = useGamesStore()
const childStore = useChildStore()
const resetArmed = ref(false)
let resetTimer = 0

function go(name: string) {
  router.push({ name })
}

function goBack() {
  // 免登录入口在档案选择页；已登录则回地图
  router.replace(childStore.child ? '/map' : '/')
}

function askReset() {
  if (!resetArmed.value) {
    resetArmed.value = true
    window.clearTimeout(resetTimer)
    resetTimer = window.setTimeout(() => (resetArmed.value = false), 3000)
    return
  }
  resetArmed.value = false
  window.clearTimeout(resetTimer)
  store.reset()
  showToast('已重置', 'success')
}
</script>

<style scoped>
.hub-page {
  max-width: 560px;
  margin: 0 auto;
}

.hub-hero {
  text-align: center;
  padding: 26px 0 20px;
  position: relative;
}

.back {
  position: absolute;
  left: 4px;
  top: 18px;
  min-height: var(--min-tap);
  color: var(--text-sub);
  font-size: 16px;
  font-weight: 600;
}

.hero-emoji {
  font-size: 44px;
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  margin-top: 4px;
}

.hero-sub {
  color: var(--text-sub);
  margin-top: 4px;
  font-size: 15px;
}

.game-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.game-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 20px;
  background: var(--card-2);
  border: 1px solid var(--border);
  text-align: left;
  transition: transform 0.12s ease;
}

.game-card:active {
  transform: scale(0.98);
}

.g-ico {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  flex: none;
}

.g-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.g-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
}

.g-desc {
  font-size: 13px;
  color: var(--text-sub);
}

.g-prog {
  font-size: 13px;
  font-weight: 700;
  color: var(--primary);
  margin-top: 2px;
}

.g-arrow {
  font-size: 26px;
  color: var(--text-sub);
  flex: none;
}

.reset-link {
  display: block;
  margin: 26px auto 8px;
  color: var(--text-sub);
  font-size: 14px;
  text-decoration: underline;
  min-height: var(--min-tap);
}
</style>
