<template>
  <div class="page page-no-tab sd-page">
    <header class="sd-top">
      <button class="back" @click="back">←</button>
      <div class="sd-title">
        <h1>数独王者</h1>
        <p v-if="state === 'playing'">{{ diffLabel }} · ⏱️ {{ fmt(elapsed) }} · 💡 {{ hintsLeft }}</p>
      </div>
    </header>

    <!-- 难度选择 -->
    <template v-if="state === 'pick'">
      <p class="tip">四年级的同学建议从六宫格开始哦！</p>
      <div class="diff-list">
        <button class="diff-card" @click="start(4)">
          <span class="d-ico">🟢</span>
          <span class="d-name">四宫格</span>
          <span class="d-sub">入门热身</span>
          <span v-if="store.data.sudoku.best['4']" class="d-best">最快 {{ fmt(store.data.sudoku.best['4']) }}</span>
        </button>
        <button class="diff-card" @click="start(6)">
          <span class="d-ico">🔵</span>
          <span class="d-name">六宫格 <em class="rec">推荐</em></span>
          <span class="d-sub">四年级起步</span>
          <span v-if="store.data.sudoku.best['6']" class="d-best">最快 {{ fmt(store.data.sudoku.best['6']) }}</span>
        </button>
        <button class="diff-card" @click="start(9)">
          <span class="d-ico">🟣</span>
          <span class="d-name">九宫格</span>
          <span class="d-sub">终极挑战</span>
          <span v-if="store.data.sudoku.best['9']" class="d-best">最快 {{ fmt(store.data.sudoku.best['9']) }}</span>
        </button>
      </div>
    </template>

    <!-- 棋盘 -->
    <template v-else-if="state === 'playing' && puzzle">
      <div
        class="sgrid"
        :style="{ gridTemplateColumns: `repeat(${puzzle.size}, 1fr)` }"
      >
        <button
          v-for="(cell, i) in flatBoard"
          :key="i"
          class="scell"
          :class="cellClass(i)"
          @click="select(i)"
        >
          {{ cell || '' }}
        </button>
      </div>

      <div class="pad">
        <button
          v-for="v in puzzle.size"
          :key="v"
          class="pnum"
          :class="{ used: countOf(v) >= puzzle.size }"
          @click="input(v)"
        >
          {{ v }}
        </button>
      </div>
      <div class="pad pad-x">
        <button class="pact" @click="erase" :disabled="!editable(sel)">🧽 擦除</button>
        <button class="pact" @click="checkBoard">✅ 检查</button>
        <button class="pact" @click="hint" :disabled="hintsLeft <= 0">💡 提示</button>
        <button class="pact" @click="state = 'pick'">← 退出</button>
      </div>
    </template>

    <!-- 完成 -->
    <div v-if="state === 'done'" class="mask">
      <div class="win-card">
        <div class="win-emoji">🏆</div>
        <h2>完成！</h2>
        <p class="win-stars">{{ '⭐'.repeat(winStars) }}{{ '☆'.repeat(3 - winStars) }}</p>
        <p class="win-sub">用时 {{ fmt(elapsed) }} · 用了 {{ 3 - hintsLeft }} 次提示</p>
        <button class="btn btn-primary" @click="start(lastSize)">再来一局</button>
        <button class="btn btn-ghost" @click="state = 'pick'">选难度</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { conflicts, generate, type SudokuPuzzle } from '@/games/sudoku'
import { useGamesStore } from '@/stores/games'

const router = useRouter()
const store = useGamesStore()

type State = 'pick' | 'playing' | 'done'
const state = ref<State>('pick')
const lastSize = ref(6)
const puzzle = ref<SudokuPuzzle | null>(null)
const board = ref<number[][]>([])
const given = ref<boolean[][]>([])
const sel = ref(-1)
const hintsLeft = ref(3)
const elapsed = ref(0)
const winStars = ref(3)
let timer = 0

const diffLabel = computed(() => (lastSize.value === 4 ? '四宫格' : lastSize.value === 6 ? '六宫格' : '九宫格'))

const flatBoard = computed(() => (board.value ? board.value.flat() : []))

function fmt(s: number): string {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return m > 0 ? m + ' 分 ' + sec + ' 秒' : sec + ' 秒'
}

function countOf(v: number): number {
  return flatBoard.value.filter((x) => x === v).length
}

function start(size: number) {
  lastSize.value = size
  puzzle.value = generate(size)
  board.value = puzzle.value.givens.map((row) => [...row])
  given.value = puzzle.value.givens.map((row) => row.map((v) => v !== 0))
  sel.value = -1
  hintsLeft.value = 3
  elapsed.value = 0
  window.clearInterval(timer)
  timer = window.setInterval(() => elapsed.value++, 1000)
  state.value = 'playing'
}

function back() {
  if (state.value === 'pick') router.replace('/games')
  else state.value = 'pick'
}

onBeforeUnmount(() => window.clearInterval(timer))

function editable(i: number): boolean {
  if (i < 0 || !puzzle.value) return false // 未选中格子（初始 -1）时不可编辑
  const r = Math.floor(i / lastSize.value)
  const c = i % lastSize.value
  return !given.value[r][c]
}

function select(i: number) {
  sel.value = i
}

function input(v: number) {
  if (sel.value < 0 || !editable(sel.value)) return
  const r = Math.floor(sel.value / lastSize.value)
  const c = sel.value % lastSize.value
  board.value[r][c] = v
  checkDone()
}

function erase() {
  if (sel.value < 0 || !editable(sel.value)) return
  const r = Math.floor(sel.value / lastSize.value)
  const c = sel.value % lastSize.value
  board.value[r][c] = 0
}

function checkBoard() {
  const bad = conflicts(board.value, puzzle.value!.size, puzzle.value!.boxW, puzzle.value!.boxH)
  if (bad.size === 0) return
  window.setTimeout(() => badCells.value.clear(), 1500)
  badCells.value = bad
}

const badCells = ref<Set<string>>(new Set())

function hint() {
  if (hintsLeft.value <= 0 || !puzzle.value) return
  // 找一个空格或错误格填正确答案
  const { size, solution } = puzzle.value
  for (let i = 0; i < size * size; i++) {
    const r = Math.floor(i / size)
    const c = i % size
    if (!given.value[r][c] && board.value[r][c] !== solution[r][c]) {
      board.value[r][c] = solution[r][c]
      given.value[r][c] = true // 提示填入的格子锁定
      hintsLeft.value--
      checkDone()
      return
    }
  }
}

function checkDone() {
  const p = puzzle.value!
  for (let r = 0; r < p.size; r++) {
    for (let c = 0; c < p.size; c++) {
      if (board.value[r][c] !== p.solution[r][c]) return
    }
  }
  window.clearInterval(timer)
  winStars.value = hintsLeft.value >= 3 ? 3 : hintsLeft.value >= 1 ? 2 : 1
  store.recordSudoku(String(lastSize.value), elapsed.value)
  state.value = 'done'
}

function cellClass(i: number): Record<string, boolean> {
  const size = lastSize.value
  const r = Math.floor(i / size)
  const c = i % size
  const p = puzzle.value!
  const cls: Record<string, boolean> = {
    given: given.value[r][c],
    sel: sel.value === i,
    // 宫线加粗：右边缘 / 下边缘
    boxR: c % p.boxW === p.boxW - 1 && c !== size - 1,
    boxB: r % p.boxH === p.boxH - 1 && r !== size - 1
  }
  const v = board.value[r][c]
  if (v && v !== p.solution[r][c]) cls.err = true
  if (badCells.value.has(r + '-' + c)) cls.err = true
  return cls
}
</script>

<style scoped>
.sd-page {
  max-width: 560px;
  margin: 0 auto;
}

.sd-top {
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
}

.sd-title {
  flex: 1;
}

.sd-title h1 {
  font-size: 20px;
  font-weight: 800;
}

.sd-title p {
  font-size: 13px;
  color: var(--text-sub);
}

.tip {
  text-align: center;
  color: var(--warning, #e8b64c);
  font-size: 14px;
  margin: 10px 0 2px;
}

.diff-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 12px;
}

.diff-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px;
  border-radius: 20px;
  background: var(--card-2);
  border: 1px solid var(--border);
}

.diff-card:active {
  transform: scale(0.98);
}

.d-ico {
  font-size: 30px;
}

.d-name {
  font-size: 18px;
  font-weight: 800;
}

.rec {
  font-style: normal;
  font-size: 12px;
  color: var(--primary);
  border: 1px solid var(--primary);
  border-radius: 8px;
  padding: 0 6px;
  margin-left: 6px;
}

.d-sub {
  font-size: 13px;
  color: var(--text-sub);
}

.d-best {
  font-size: 12px;
  color: var(--primary);
}

/* ---- 棋盘 ---- */
.sgrid {
  display: grid;
  gap: 2px;
  background: var(--border);
  border: 2px solid var(--border-light);
  border-radius: 12px;
  padding: 3px;
  width: 100%;
  max-width: 380px;
  margin: 8px auto 0;
  touch-action: manipulation;
}

.scell {
  aspect-ratio: 1;
  background: var(--card-2);
  color: var(--text-main);
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.scell.given {
  color: #7fd06a;
}

.scell.sel {
  background: var(--primary-soft);
  outline: 2px solid var(--primary);
}

.scell.err {
  color: var(--danger);
  background: rgba(216, 67, 67, 0.15);
}

.scell.boxR {
  margin-right: 4px;
}

.scell.boxB {
  margin-bottom: 4px;
}

/* ---- 数字键盘 ---- */
.pad {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 16px;
}

.pad .pnum {
  width: 48px;
  height: 52px;
  border-radius: 12px;
  background: var(--card-2);
  border: 1px solid var(--border);
  font-size: 22px;
  font-weight: 800;
}

.pad .pnum.used {
  opacity: 0.35;
}

.pad-x {
  margin-top: 10px;
  flex-wrap: wrap;
}

.pact {
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--card-2);
  border: 1px solid var(--border);
  font-weight: 700;
  font-size: 14px;
  min-height: var(--min-tap);
}

.pact:disabled {
  opacity: 0.4;
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
