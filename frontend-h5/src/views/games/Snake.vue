<template>
  <div class="page page-no-tab snake-page">
    <header class="sn-top">
      <button class="back" @click="goBack">← 返回</button>
      <h1 class="sn-title">🐍 贪吃蛇</h1>
    </header>

    <!-- 信息栏：得分 + 最高分 -->
    <div class="info-bar">
      <span class="info-item">当前得分: {{ score }}</span>
      <span class="info-sep">|</span>
      <span class="info-item">最高分: {{ best }}</span>
    </div>

    <!-- 画布区域（绝对定位遮罩层叠在 Canvas 上） -->
    <div class="cv-outer">
      <div class="cv-wrap">
        <canvas ref="canvasRef"></canvas>

        <!-- 未开始：点击开始 -->
        <div
          v-if="state === 'waiting'"
          class="cv-overlay"
          @touchstart.prevent="start"
          @mousedown="start"
        >
          <div class="ov-title">点击开始</div>
          <div class="ov-sub">方向键 / WASD / 下方按钮控制</div>
        </div>

        <!-- 暂停中 -->
        <div
          v-else-if="state === 'paused'"
          class="cv-overlay"
          @touchstart.prevent="togglePause"
          @mousedown="togglePause"
        >
          <div class="ov-title">已暂停</div>
          <div class="ov-sub">点击画布或按「继续」恢复</div>
        </div>

        <!-- 游戏结束 -->
        <div v-else-if="state === 'over'" class="cv-overlay over-mask">
          <h2 class="ov-go-title">游戏结束</h2>
          <p class="ov-go-score">最终得分: {{ score }}</p>
          <button class="sn-btn" @touchstart.prevent="start" @mousedown="start">重新开始</button>
        </div>
      </div>
    </div>

    <!-- 功能按钮：开始 / 暂停-继续 -->
    <div class="btn-row">
      <button
        v-if="state === 'running' || state === 'paused'"
        class="sn-btn wide"
        @touchstart.prevent="togglePause"
        @mousedown="togglePause"
      >
        {{ state === 'paused' ? '继续' : '暂停' }}
      </button>
      <button v-else class="sn-btn wide" @touchstart.prevent="start" @mousedown="start">
        开始游戏
      </button>
    </div>

    <!-- 触摸方向键：绝对定位十字布局（60×60，间距 10px） -->
    <div class="dpad-outer">
      <div class="dpad">
        <button class="d-btn up" @touchstart.prevent="setDir('up')" @mousedown="setDir('up')">↑</button>
        <button class="d-btn left" @touchstart.prevent="setDir('left')" @mousedown="setDir('left')">←</button>
        <button class="d-btn right" @touchstart.prevent="setDir('right')" @mousedown="setDir('right')">→</button>
        <button class="d-btn down" @touchstart.prevent="setDir('down')" @mousedown="setDir('down')">↓</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useGamesStore } from '@/stores/games'
import createGame from '@/games/snakeCore'

const router = useRouter()
const store = useGamesStore()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const score = ref(0)
const state = ref<'waiting' | 'running' | 'paused' | 'over'>('waiting')
// 最高分来自 store（localStorage 持久化），破纪录后自动刷新
const best = computed(() => store.data.snake.best)

let game: ReturnType<typeof createGame> | null = null

onMounted(() => {
  game = createGame({
    canvas: canvasRef.value as HTMLCanvasElement,
    hooks: {
      onScore: function (s: number) {
        score.value = s
      },
      onOver: function (s: number) {
        store.recordSnakeBest(s) // 游戏结束时更新最高分
      },
      onStateChange: function (s: 'waiting' | 'running' | 'paused' | 'over') {
        state.value = s
      }
    }
  })
})

onBeforeUnmount(() => {
  if (game) {
    game.destroy()
    game = null
  }
})

function start() {
  if (game && state.value !== 'running') game.start()
}

function togglePause() {
  if (game) game.togglePause()
}

function setDir(d: 'up' | 'down' | 'left' | 'right') {
  if (game) game.setDirection(d)
}

function goBack() {
  router.replace('/games')
}
</script>

<style scoped>
/* 页面布局：禁用 Flex/Grid，用 text-align 居中 + 绝对定位 */
.snake-page {
  max-width: 620px;
  margin: 0 auto;
  -webkit-user-select: none;
  user-select: none;
}

.sn-top {
  text-align: center;
  padding: 22px 0 8px;
  position: relative;
}

.back {
  position: absolute;
  left: 4px;
  top: 16px;
  min-height: var(--min-tap);
  color: var(--text-sub);
  font-size: 16px;
  font-weight: 600;
}

.sn-title {
  font-size: 26px;
  font-weight: 800;
}

/* 信息栏 */
.info-bar {
  text-align: center;
  padding: 8px 0 12px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-main);
}

.info-sep {
  margin: 0 14px;
  color: var(--text-sub);
  font-weight: 400;
}

/* 画布：inline-block 收缩包裹 canvas，便于遮罩层与画布等宽 */
.cv-outer {
  text-align: center;
}

.cv-wrap {
  position: relative;
  display: inline-block;
  line-height: 0;
  border-radius: 12px;
  overflow: hidden;
  touch-action: none; /* 防止画布上触发浏览器滚动/缩放 */
}

.cv-wrap canvas {
  display: block;
  background: #333333;
  border-radius: 12px;
}

/* 画布遮罩层 */
.cv-overlay {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55);
  line-height: 1.4;
  display: block;
}

/* 游戏结束遮罩：透明度 70% */
.over-mask {
  background: rgba(0, 0, 0, 0.7);
}

.ov-title {
  margin-top: 34%;
  font-size: 30px;
  font-weight: 800;
  color: #ffffff;
}

.ov-sub {
  margin-top: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
}

.ov-go-title {
  margin-top: 26%;
  font-size: 28px;
  color: #ffffff;
  font-weight: 800;
}

.ov-go-score {
  margin: 10px 0 18px;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

/* 统一按钮样式：深灰背景、白字、8px 圆角、点击变深 */
.sn-btn {
  display: inline-block;
  background: #444444;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 12px 22px;
  font-size: 17px;
  font-weight: 700;
  min-height: 48px;
  -webkit-touch-callout: none;
}

.sn-btn:active {
  background: #222222; /* 点击视觉反馈：颜色变深 */
}

.wide {
  min-width: 160px;
}

.btn-row {
  text-align: center;
  padding: 14px 0 4px;
}

/* 触摸方向键：相对容器 + 绝对定位十字布局 */
.dpad-outer {
  text-align: center;
  padding: 12px 0 30px;
}

.dpad {
  position: relative;
  display: inline-block;
  width: 200px;
  height: 200px;
  touch-action: none;
}

.d-btn {
  position: absolute;
  width: 60px;
  height: 60px;
  border: none;
  border-radius: 12px;
  background: #444444;
  color: #ffffff;
  font-size: 26px;
  font-weight: 800;
  line-height: 60px;
  padding: 0;
  -webkit-touch-callout: none;
}

.d-btn:active {
  background: #222222;
}

/* 十字位置：60px 按钮 + 10px 间距（列位 0/70/140，行位同） */
.d-btn.up {
  left: 70px;
  top: 0;
}

.d-btn.left {
  left: 0;
  top: 70px;
}

.d-btn.right {
  left: 140px;
  top: 70px;
}

.d-btn.down {
  left: 70px;
  top: 140px;
}
</style>
