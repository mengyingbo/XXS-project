<template>
  <div class="handwrap" :class="{ 'st-correct': state === 'correct', 'st-wrong': state === 'wrong' }">
    <div class="cells">
      <div v-for="(syl, i) in syllables" :key="i" class="cell-col">
        <span class="syl">{{ syl }}</span>
        <canvas
          ref="canvasRefs"
          class="cell"
          :class="{ 'cell-active': i === lastActive && written[i] && !disabled }"
          :width="CELL * dpr"
          :height="CELL * dpr"
          @touchstart.prevent="onTouchStart($event, i)"
          @touchmove.prevent="onTouchMove($event, i)"
          @touchend.prevent="onTouchEnd($event, i)"
          @mousedown="onMouseDown($event, i)"
          @mousemove="onMouseMove($event, i)"
          @mouseup="onMouseUp($event, i)"
          @mouseleave="onMouseUp($event, i)"
        />
      </div>
    </div>

    <!-- 书写工具 -->
    <div class="tools">
      <button class="tool-btn" :disabled="disabled || !hasWritten" @click="undoCell">撤销上一笔</button>
      <button class="tool-btn" :disabled="disabled || !hasWritten" @click="clearCell">清空本格</button>
      <button class="tool-btn" :disabled="disabled" @click="clearAll">全部重写</button>
    </div>

    <!-- 对照答案 + 自评（方案 A，无自动识别） -->
    <template v-if="!revealed">
      <button class="btn btn-primary btn-block reveal-btn" :disabled="!canReveal" @click="reveal">
        对照答案
      </button>
    </template>
    <template v-else>
      <div class="reveal-box">
        <div class="reveal-label">正确词语</div>
        <div class="reveal-word">
          <span v-for="(ch, i) in target" :key="i" class="reveal-ch">{{ ch }}</span>
        </div>
        <div class="reveal-tip">对照田字格里的字，写对了吗？</div>
      </div>
      <div class="selfeval">
        <button class="btn eval-btn eval-ok" :disabled="disabled" @click="selfJudge(true)">✓ 写对了</button>
        <button class="btn eval-btn eval-no" :disabled="disabled" @click="selfJudge(false)">✗ 写错了</button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

/** 单格边长（CSS 像素） */
const CELL = 208

const props = withDefaults(
  defineProps<{
    modelValue: string
    disabled?: boolean
    state?: 'idle' | 'correct' | 'wrong'
    /** 题干拼音（按字空格分隔） */
    stem: string
    /** 可接受答案（仅取第一项作为目标词，用于对照展示） */
    answer?: string[] | null
  }>(),
  { disabled: false, state: 'idle', stem: '', answer: null }
)
const emit = defineEmits<{ 'update:modelValue': [value: string]; submit: [] }>()

/* ---------- 目标词与格 ---------- */
const target = computed(() => (props.answer && props.answer[0] ? props.answer[0] : '').split(''))
/** 目标字数决定田字格数量（无答案兜底 1 格） */
const cellCount = computed(() => Math.max(target.value.length, 1))
/** 拼音按字分组（音节数与字数不一致时仍逐格展示） */
const syllables = computed(() => {
  const parts = props.stem.trim().split(/\s+/).filter(Boolean)
  const out: string[] = []
  for (let i = 0; i < cellCount.value; i++) out.push(parts[i] ?? '')
  return out
})

/* ---------- 手写状态 ---------- */
const dpr = Math.min(typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1, 2)
/** 每格全部笔画：cellStrokes[i] = 笔画数组，笔画 = [x,y][] */
const cellStrokes: number[][][][] = []
/** 已写标记（响应式，驱动按钮可用） */
const written = ref<boolean[]>([])
const lastActive = ref(0)
/** 正在书写的格索引（-1 = 未在写） */
let drawing = -1
let currentStroke: number[][] = []
const canvasRefs = ref<HTMLCanvasElement[]>([])

const hasWritten = computed(() => written.value.some(Boolean))
const canReveal = computed(
  () => !props.disabled && !revealed.value && written.value.every(Boolean) && !!props.answer?.[0]
)

/** 是否已点「对照答案」 */
const revealed = ref(false)

onMounted(() => {
  cellStrokes.length = 0
  written.value = []
  for (let i = 0; i < cellCount.value; i++) {
    cellStrokes.push([])
    written.value.push(false)
  }
})

/* ---------- 绘制 ---------- */
function cssVar(name: string, fallback: string): string {
  if (typeof getComputedStyle === 'undefined') return fallback
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

function drawGrid(ctx: CanvasRenderingContext2D, size: number) {
  const border = props.state === 'correct' ? cssVar('--success', '#22c55e')
    : props.state === 'wrong' ? cssVar('--danger', '#ef4444') : '#c9c9c9'
  const cross = props.state === 'idle' ? '#e3e3e3' : border
  ctx.clearRect(0, 0, size, size)
  ctx.strokeStyle = border
  ctx.lineWidth = 2
  ctx.strokeRect(1.5, 1.5, size - 3, size - 3)
  // 田字格中线（虚线十字）
  ctx.save()
  ctx.strokeStyle = cross
  ctx.lineWidth = 1
  ctx.setLineDash([5, 5])
  ctx.beginPath()
  ctx.moveTo(size / 2, 4)
  ctx.lineTo(size / 2, size - 4)
  ctx.moveTo(4, size / 2)
  ctx.lineTo(size - 4, size / 2)
  ctx.stroke()
  ctx.restore()
}

function drawStrokes(ctx: CanvasRenderingContext2D, size: number, i: number) {
  const strokes = cellStrokes[i] || []
  ctx.strokeStyle = '#333333'
  ctx.lineWidth = 4
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  for (const stroke of strokes) {
    ctx.beginPath()
    for (let p = 0; p < stroke.length; p++) {
      if (p === 0) ctx.moveTo(stroke[p][0], stroke[p][1])
      else ctx.lineTo(stroke[p][0], stroke[p][1])
    }
    if (stroke.length === 1) {
      // 单点：画个小圆点
      ctx.arc(stroke[0][0], stroke[0][1], 2, 0, Math.PI * 2)
      ctx.fillStyle = '#333333'
      ctx.fill()
    }
    ctx.stroke()
  }
  void size
}

function redrawCell(i: number) {
  const canvas = canvasRefs.value[i]
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  drawGrid(ctx, CELL)
  drawStrokes(ctx, CELL, i)
}

function redrawAll() {
  for (let i = 0; i < cellCount.value; i++) redrawCell(i)
}

// 反馈态变化时重绘（田字格边框变绿/红）
watch(
  () => props.state,
  () => redrawAll()
)

/* ---------- 指针输入（touch + mouse 双通道，iOS 11 兼容） ---------- */
function touchPoint(e: TouchEvent, canvas: HTMLCanvasElement): number[] {
  const rect = canvas.getBoundingClientRect()
  const t = (e.touches && e.touches[0]) || e.changedTouches[0]
  return [t.clientX - rect.left, t.clientY - rect.top]
}

function mousePoint(e: MouseEvent, canvas: HTMLCanvasElement): number[] {
  const rect = canvas.getBoundingClientRect()
  return [e.clientX - rect.left, e.clientY - rect.top]
}

function onTouchStart(e: TouchEvent, i: number) {
  if (props.disabled) return
  const canvas = canvasRefs.value[i]
  if (!canvas) return
  drawing = i
  lastActive.value = i
  currentStroke = [touchPoint(e, canvas)]
}

function onTouchMove(e: TouchEvent, i: number) {
  if (drawing !== i) return
  const canvas = canvasRefs.value[i]
  if (!canvas) return
  const pt = touchPoint(e, canvas)
  currentStroke.push(pt)
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.strokeStyle = '#333333'
  ctx.lineWidth = 4
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  const n = currentStroke.length
  if (n >= 2) {
    ctx.beginPath()
    ctx.moveTo(currentStroke[n - 2][0], currentStroke[n - 2][1])
    ctx.lineTo(currentStroke[n - 1][0], currentStroke[n - 1][1])
    ctx.stroke()
  }
}

function finishStroke(i: number) {
  if (drawing !== i) return
  if (currentStroke.length > 0) {
    cellStrokes[i].push(currentStroke)
    written.value[i] = true
  }
  currentStroke = []
  drawing = -1
  redrawCell(i)
}

function onTouchEnd(e: TouchEvent, i: number) {
  if (drawing !== i) return
  void e
  finishStroke(i)
}

/* 鼠标通道（桌面调试用） */
let mouseDown = false
function onMouseDown(e: MouseEvent, i: number) {
  if (props.disabled) return
  const canvas = canvasRefs.value[i]
  if (!canvas) return
  mouseDown = true
  drawing = i
  lastActive.value = i
  currentStroke = [mousePoint(e, canvas)]
}
function onMouseMove(e: MouseEvent, i: number) {
  if (!mouseDown || drawing !== i) return
  const canvas = canvasRefs.value[i]
  if (!canvas) return
  currentStroke.push(mousePoint(e, canvas))
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  const n = currentStroke.length
  if (n >= 2) {
    ctx.strokeStyle = '#333333'
    ctx.lineWidth = 4
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(currentStroke[n - 2][0], currentStroke[n - 2][1])
    ctx.lineTo(currentStroke[n - 1][0], currentStroke[n - 1][1])
    ctx.stroke()
  }
}
function onMouseUp(e: MouseEvent, i: number) {
  if (!mouseDown || drawing !== i) return
  void e
  mouseDown = false
  finishStroke(i)
}

/* ---------- 撤销 / 清空 ---------- */
function undoCell() {
  const i = lastActive.value
  const strokes = cellStrokes[i]
  if (!strokes || strokes.length === 0) return
  strokes.pop()
  written.value[i] = strokes.length > 0
  redrawCell(i)
}

function clearCell() {
  const i = lastActive.value
  cellStrokes[i] = []
  written.value[i] = false
  redrawCell(i)
}

function clearAll() {
  for (let i = 0; i < cellCount.value; i++) {
    cellStrokes[i] = []
    written.value[i] = false
    redrawCell(i)
  }
  lastActive.value = 0
}

/* ---------- 对照答案 / 自评 ---------- */
function reveal() {
  if (!canReveal.value) return
  revealed.value = true
}

/** 写对了 → 提交目标词；写错了 → 提交必定不匹配的哨兵文本（后端判错并展示正确答案） */
const WRONG_SENTINEL = '没写对'
function selfJudge(ok: boolean) {
  if (props.disabled) return
  emit('update:modelValue', ok ? target.value.join('') : WRONG_SENTINEL)
  emit('submit')
}
</script>

<style scoped>
.handwrap {
  display: block;
}

.cells {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin: 10px 0 12px;
}

.cell-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.syl {
  font-size: 22px;
  line-height: 1.2;
  color: #6b5b3e;
  font-family: 'PingFang SC', sans-serif;
  min-height: 26px;
}

.cell {
  width: 208px;
  height: 208px;
  touch-action: none;
  background: #fff;
  border-radius: 8px;
}

.cell-active {
  box-shadow: 0 0 0 2px #6b5b3e33;
}

.tools {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tool-btn {
  background: #f1ece2;
  color: #6b5b3e;
  border: none;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 15px;
}

.tool-btn:disabled {
  opacity: 0.45;
}

.tool-btn:active:not(:disabled) {
  background: #e5ddcd;
}

.reveal-btn {
  margin-bottom: 10px;
}

.reveal-box {
  text-align: center;
  background: #faf6ee;
  border: 1px dashed #d8cbb2;
  border-radius: 12px;
  padding: 14px 12px 12px;
  margin-bottom: 12px;
}

.reveal-label {
  font-size: 13px;
  color: #9a8a6a;
  margin-bottom: 6px;
}

.reveal-word {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.reveal-ch {
  font-size: 40px;
  line-height: 1.2;
  color: #4a3f2c;
  font-family: 'Kaiti SC', 'KaiTi', 'STKaiti', serif;
  font-weight: 700;
}

.reveal-tip {
  margin-top: 8px;
  font-size: 14px;
  color: #9a8a6a;
}

.selfeval {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}

.eval-btn {
  flex: 1;
  border: none;
  border-radius: 12px;
  padding: 14px 0;
  font-size: 17px;
}

.eval-ok {
  background: var(--success, #22c55e);
  color: #fff;
}

.eval-no {
  background: var(--danger, #ef4444);
  color: #fff;
}

.eval-btn:disabled {
  opacity: 0.5;
}

.handwrap.st-correct .cell {
  background: var(--success-soft, #eefaf0);
}

.handwrap.st-wrong .cell {
  background: var(--danger-soft, #fdeeee);
}
</style>
