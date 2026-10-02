<template>
  <div class="page page-no-tab kl-page">
    <header class="kl-top">
      <button class="back" @click="back">←</button>
      <div class="kl-title">
        <h1>华容道</h1>
        <p v-if="state === 'playing'">{{ cur.name }} · {{ cur.focus }}</p>
      </div>
      <span class="kl-moves">👣 {{ moves }}</span>
    </header>

    <!-- 模式选择 -->
    <template v-if="state === 'mode'">
      <div class="mode-cards">
        <button class="mode-card" @click="state = 'list-classic'">
          <span class="m-ico">🎎</span>
          <span class="m-name">传统华容道</span>
          <span class="m-sub">送曹操逃出华容道</span>
        </button>
        <button class="mode-card" @click="state = 'list-rush'">
          <span class="m-ico">🚗</span>
          <span class="m-name">路径规划</span>
          <span class="m-sub">让红车冲出停车场</span>
        </button>
      </div>
    </template>

    <!-- 关卡列表 -->
    <template v-else-if="state === 'list-classic' || state === 'list-rush'">
      <div class="lv-list">
        <button
          v-for="(lv, i) in levels"
          :key="lv.name"
          class="lv-item"
          :class="{ locked: !unlocked(i) }"
          @click="unlocked(i) && startLevel(i)"
        >
          <span class="lv-no">{{ unlocked(i) ? i + 1 : '🔒' }}</span>
          <span class="lv-info">
            <span class="lv-name">{{ lv.name }}</span>
            <span class="lv-focus">🧠 {{ lv.focus }}</span>
          </span>
          <span class="lv-stars">{{ starText('klotski', modeKey(), i) }}</span>
        </button>
      </div>
    </template>

    <!-- 棋盘 -->
    <template v-else-if="state === 'playing'">
      <div class="board-wrap">
        <div
          class="board"
          :style="{ width: boardPx + 'px', height: cellPx * nRows + 'px' }"
        >
          <!-- 出口标记 -->
          <div v-if="cur.mode === 'classic'" class="exit exit-classic"></div>
          <div v-else class="exit exit-rush" :style="{ top: cellPx * 2 + 'px' }"></div>

          <div
            v-for="(p, i) in pieces"
            :key="i"
            class="piece"
            :class="pieceClass(p) + (hintIdx === i ? ' hint' : '')"
            :style="pieceStyle(p)"
            @touchstart.stop.prevent="onTDown($event, i)"
            @touchmove.stop.prevent="onTMove"
            @touchend="onTUp"
            @touchcancel="onTUp"
          >
            <span v-if="p.w * p.h > 1 || cur.mode === 'rush'" class="p-face">{{ pieceFace(p) }}</span>
          </div>
        </div>
      </div>

      <div class="kl-actions">
        <button class="act" @click="undo" :disabled="!history.length">↩️ 撤销</button>
        <button class="act" @click="restart">🔄 重开</button>
        <button class="act" @click="hint" :disabled="hintsLeft <= 0">💡 提示 {{ hintsLeft }}</button>
      </div>
      <button class="lv-back" @click="state = modeKey() === 'classic' ? 'list-classic' : 'list-rush'">← 关卡列表</button>
    </template>

    <!-- 胜利弹层 -->
    <div v-if="state === 'won'" class="mask" @click="state = modeKey() === 'classic' ? 'list-classic' : 'list-rush'">
      <div class="win-card">
        <div class="win-emoji">🎉</div>
        <h2>{{ cur.name }} 通关！</h2>
        <p class="win-stars">{{ '⭐'.repeat(winStars) }}{{ '☆'.repeat(3 - winStars) }}</p>
        <p class="win-sub">用了 {{ moves }} 步{{ optimal ? '（最少 ' + optimal + ' 步）' : '' }}</p>
        <button class="btn btn-primary" @click="nextLevel">下一关 →</button>
        <button class="btn btn-ghost" @click="state = modeKey() === 'classic' ? 'list-classic' : 'list-rush'">关卡列表</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CLASSIC_LEVELS,
  RUSH_LEVELS,
  boardSize,
  isWin,
  parseClassic,
  parseRush,
  slideOnce,
  solve,
  type KlotskiLevel,
  type Piece
} from '@/games/klotski'
import { useGamesStore } from '@/stores/games'
import { showToast } from '@/composables/useToast'

const router = useRouter()
const store = useGamesStore()

type State = 'mode' | 'list-classic' | 'list-rush' | 'playing' | 'won'
const state = ref<State>('mode')
const listMode = ref<'classic' | 'rush'>('classic')
const lvIndex = ref(0)
const pieces = ref<Piece[]>([])
const moves = ref(0)
const history = ref<Piece[][]>([])
const hintsLeft = ref(3)
const hintIdx = ref(-1)
const winStars = ref(0)
const optimal = ref(0)

const cur = computed<KlotskiLevel>(() =>
  (listMode.value === 'classic' ? CLASSIC_LEVELS : RUSH_LEVELS)[lvIndex.value]
)
const levels = computed(() => (listMode.value === 'classic' ? CLASSIC_LEVELS : RUSH_LEVELS))
const boardDim = computed(() => boardSize(cur.value.mode))
const cols = computed(() => boardDim.value.cols)
const nRows = computed(() => boardDim.value.rows)
const boardPx = computed(() => Math.min(340, window.innerWidth - 70))
const cellPx = computed(() => boardPx.value / cols.value)

function modeKey(): 'classic' | 'rush' {
  return listMode.value
}

function levelKey(mode: 'classic' | 'rush', index: number) {
  return mode + '-' + index
}

function unlocked(index: number) {
  if (index === 0) return true
  const map = store.data.klotski
  return levelKey(modeKey(), index - 1) in map
}

function starText(g: 'klotski', mode: 'classic' | 'rush', i: number) {
  const s = store.data[g][levelKey(mode, i)]
  return s ? '⭐'.repeat(s) : ''
}

function startLevel(i: number) {
  lvIndex.value = i
  const lv = levels.value[i]
  listMode.value = lv.mode
  pieces.value = lv.mode === 'classic' ? parseClassic(lv.rows) : parseRush(lv.rows)
  moves.value = 0
  history.value = []
  hintsLeft.value = 3
  hintIdx.value = -1
  state.value = 'playing'
}

function restart() {
  startLevel(lvIndex.value)
}

function undo() {
  const prev = history.value.pop()
  if (prev) {
    pieces.value = prev
    moves.value = Math.max(0, moves.value - 1)
  }
}

function back() {
  if (state.value === 'playing' || state.value === 'won') {
    state.value = modeKey() === 'classic' ? 'list-classic' : 'list-rush'
  } else if (state.value.startsWith('list')) {
    state.value = 'mode'
  } else {
    router.replace('/games')
  }
}

function hint() {
  if (hintsLeft.value <= 0) return
  const path = solve(cur.value.mode, pieces.value)
  if (!path || !path.length) {
    showToast('提示用完啦，再想想办法！', 'info')
    return
  }
  const [idx, dx, dy] = path[0]
  hintIdx.value = idx
  const dir = dy < 0 ? '上' : dy > 0 ? '下' : dx < 0 ? '左' : '右'
  showToast(`提示：把 ${pieceName(idx)} 往 ${dir} 移`, 'info')
  hintsLeft.value--
  window.setTimeout(() => (hintIdx.value = -1), 2000)
}

function pieceName(idx: number) {
  const p = pieces.value[idx]
  if (cur.value.mode === 'rush') return p.id === 'a' ? '红车' : '挡路车'
  if (p.id === 'C') return '曹操'
  if (p.w === 2 && p.h === 1) return '横将'
  if (p.w === 1 && p.h === 2) return '竖将'
  return '小兵'
}

function checkWin() {
  if (!isWin(cur.value.mode, pieces.value)) return
  optimal.value = solve(cur.value.mode, pieces.value)?.length ?? 0
  if (!optimal.value) optimal.value = moves.value
  winStars.value = moves.value <= optimal.value ? 3 : moves.value <= optimal.value * 1.5 ? 2 : 1
  store.complete('klotski', levelKey(cur.value.mode, lvIndex.value), winStars.value)
  state.value = 'won'
}

function nextLevel() {
  if (lvIndex.value + 1 < levels.value.length) {
    startLevel(lvIndex.value + 1)
  } else {
    showToast('全部通关，太厉害了！', 'success')
    state.value = modeKey() === 'classic' ? 'list-classic' : 'list-rush'
  }
}

/* ---------- 滑动交互（touch 事件，兼容 iOS 11）：按下棋子 → 滑动超阈值 → 移动一格 ---------- */
let downId = -1
let downX = 0
let downY = 0
let moved = false

function onTDown(e: TouchEvent, idx: number) {
  downId = idx
  downX = e.touches[0].clientX
  downY = e.touches[0].clientY
  moved = false
}

function onTMove(e: TouchEvent) {
  if (downId < 0 || moved) return
  const dx = e.touches[0].clientX - downX
  const dy = e.touches[0].clientY - downY
  const TH = 22
  if (Math.abs(dx) < TH && Math.abs(dy) < TH) return
  const sdx = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : -1) : 0
  const sdy = Math.abs(dy) >= Math.abs(dx) ? (dy > 0 ? 1 : -1) : 0
  tryMove(downId, sdx, sdy)
  moved = true
}

function onTUp() {
  downId = -1
}

function tryMove(idx: number, dx: number, dy: number) {
  if (state.value !== 'playing') return
  const next = slideOnce(pieces.value, idx, dx, dy, cur.value.mode)
  if (!next) return
  history.value.push(pieces.value)
  if (history.value.length > 200) history.value.shift()
  pieces.value = next
  moves.value++
  hintIdx.value = -1
  checkWin()
}

/* ---------- 渲染 ---------- */
function pieceStyle(p: Piece) {
  return {
    left: p.x * cellPx.value + 2 + 'px',
    top: p.y * cellPx.value + 2 + 'px',
    width: p.w * cellPx.value - 4 + 'px',
    height: p.h * cellPx.value - 4 + 'px'
  }
}

function pieceClass(p: Piece) {
  if (cur.value.mode === 'rush') {
    if (p.id === 'a') return 'pc-target'
    return p.w > p.h ? 'pc-hbar' : p.h > p.w ? 'pc-vbar' : 'pc-cell'
  }
  if (p.id === 'C') return 'pc-caocao'
  if (p.w === 2 && p.h === 1) return 'pc-hgen'
  if (p.w === 1 && p.h === 2) return 'pc-vgen'
  return 'pc-soldier'
}

function pieceFace(p: Piece) {
  if (cur.value.mode === 'rush') {
    if (p.id === 'a') return '🚗'
    return p.w > p.h ? '🚌' : p.h > p.w ? '🚕' : '🛵'
  }
  if (p.id === 'C') return '👑'
  if (p.w === 2 && p.h === 1) return '🗡️'
  if (p.w === 1 && p.h === 2) return '🛡️'
  return '⚫'
}
</script>

<style scoped>
.kl-page {
  max-width: 560px;
  margin: 0 auto;
}

.kl-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 4px 10px;
}

.back {
  min-width: 44px;
  min-height: 44px;
  color: var(--text-sub);
  font-size: 20px;
  flex: none;
}

.kl-title {
  flex: 1;
}

.kl-title h1 {
  font-size: 20px;
  font-weight: 800;
}

.kl-title p {
  font-size: 13px;
  color: var(--text-sub);
}

.kl-moves {
  font-weight: 800;
  font-size: 16px;
}

.mode-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 18px;
}

.mode-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 26px 12px;
  border-radius: 20px;
  background: var(--card-2);
  border: 1px solid var(--border);
}

.mode-card:active {
  transform: scale(0.97);
}

.m-ico {
  font-size: 44px;
}

.m-name {
  font-size: 17px;
  font-weight: 800;
}

.m-sub {
  font-size: 13px;
  color: var(--text-sub);
}

.lv-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.lv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  border-radius: 16px;
  background: var(--card-2);
  border: 1px solid var(--border);
  text-align: left;
}

.lv-item.locked {
  opacity: 0.45;
}

.lv-no {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.lv-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.lv-name {
  font-size: 16px;
  font-weight: 700;
}

.lv-focus {
  font-size: 12px;
  color: var(--text-sub);
}

.lv-stars {
  font-size: 13px;
  flex: none;
}

.board-wrap {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.board {
  position: relative;
  background: var(--card-2);
  border: 2px solid var(--border-light);
  border-radius: 12px;
  touch-action: none;
}

.exit {
  position: absolute;
  background: var(--primary-soft);
}

.exit-classic {
  bottom: -2px;
  left: 25%;
  width: 50%;
  height: 10px;
  border-radius: 0 0 10px 10px;
}

.exit-rush {
  right: -2px;
  width: 10px;
  height: 16.66%;
  border-radius: 0 10px 10px 0;
}

.piece {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 22px;
  touch-action: none;
  transition: left 0.12s ease, top 0.12s ease;
  user-select: none;
  -webkit-user-select: none;
}

.pc-caocao {
  background: linear-gradient(160deg, #ffd257, #e8963c);
  color: #5a3410;
}

.pc-vgen,
.pc-hgen {
  background: linear-gradient(160deg, #4aa3ff, #2f6fd8);
  color: #fff;
}

.pc-soldier {
  background: linear-gradient(160deg, #8b9aa5, #5f6d77);
  color: #fff;
}

.pc-target {
  background: linear-gradient(160deg, #ff6b6b, #d84343);
  color: #fff;
}

.pc-hbar,
.pc-vbar {
  background: linear-gradient(160deg, #6fa8dc, #4a7fb5);
  color: #fff;
}

.pc-cell {
  background: linear-gradient(160deg, #9bb0c0, #71828f);
  color: #fff;
}

.piece.hint {
  outline: 3px solid var(--primary);
  outline-offset: 1px;
}

.kl-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 16px;
}

.act {
  padding: 10px 16px;
  border-radius: 14px;
  background: var(--card-2);
  border: 1px solid var(--border);
  font-weight: 700;
  font-size: 14px;
  min-height: var(--min-tap);
}

.act:disabled {
  opacity: 0.4;
}

.lv-back {
  display: block;
  margin: 14px auto 0;
  color: var(--text-sub);
  font-size: 14px;
  min-height: var(--min-tap);
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
}

.win-card {
  width: 82%;
  max-width: 340px;
  background: var(--card-2);
  border-radius: 22px;
  padding: 28px 22px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.win-emoji {
  font-size: 46px;
}

.win-card h2 {
  font-size: 20px;
}

.win-stars {
  font-size: 26px;
}

.win-sub {
  color: var(--text-sub);
  font-size: 14px;
  margin-bottom: 6px;
}
</style>
