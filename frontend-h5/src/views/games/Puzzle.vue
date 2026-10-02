<template>
  <div class="page page-no-tab puzzle-page">
    <header class="pz-top">
      <button class="back" @click="goBack">← 返回</button>
      <h1 class="pz-title">🧩 拼图大师</h1>
    </header>

    <!-- 状态一：选图片 + 选难度 -->
    <div v-if="state === 'pick'" class="pick">
      <div class="sec-title">选一张图片</div>
      <div class="art-grid">
        <button
          v-for="(a, i) in allArts"
          :key="a.name + i"
          class="art-card"
          :class="{ sel: artIdx === i }"
          @click="artIdx = i"
        >
          <img :src="a.url" :alt="a.name" />
          <span class="art-name">{{ a.name }}</span>
          <span class="art-from">{{ a.from }}</span>
        </button>

        <label class="art-card upload">
          <input type="file" accept="image/*" @change="onUpload" />
          <span class="up-ico">📷</span>
          <span class="art-name">上传照片</span>
          <span class="art-from">用自己喜欢的图拼</span>
        </label>
      </div>

      <div class="sec-title">选难度</div>
      <div class="diff-row">
        <button
          v-for="d in DIFFS"
          :key="d.n"
          class="diff-btn"
          :class="{ sel: n === d.n }"
          @click="n = d.n"
        >
          <b>{{ d.n }}×{{ d.n }}</b>
          <span>{{ d.label }}</span>
        </button>
      </div>

      <button class="start-btn" @click="start">开始拼图</button>
    </div>

    <!-- 状态二：拼图进行中 -->
    <div v-else class="play">
      <div class="stat-bar">
        <span>⏱ {{ fmt(time) }}</span>
        <span>👣 {{ steps }} 步</span>
        <span>{{ n }}×{{ n }}</span>
      </div>

      <div class="board-wrap">
        <div
          ref="boardRef"
          class="board"
          :style="{
            width: boardPx + 'px',
            height: boardPx + 'px',
            gridTemplateColumns: 'repeat(' + n + ', 1fr)',
            gridTemplateRows: 'repeat(' + n + ', 1fr)'
          }"
        >
          <div
            v-for="pos in n * n"
            :key="pos - 1"
            class="cell"
            :class="{ hint: dragTarget === pos - 1 }"
          >
            <div
              class="tile"
              :class="tileClass(pos - 1)"
              :style="tileStyle(pos - 1)"
              @touchstart="onDown(pos - 1, $event)"
              @mousedown="onDown(pos - 1, $event)"
            >
              <span v-if="showNums" class="num">{{ order[pos - 1] + 1 }}</span>
            </div>
          </div>
        </div>
        <!-- 拖拽中的浮起块 -->
        <div v-if="ghost.show" class="ghost" :style="ghostStyle"></div>
      </div>

      <div class="ctrl-row">
        <button @click="restart">🔄 重新开始</button>
        <button @click="changeImg">🖼 换一张</button>
      </div>
      <div class="ctrl-row">
        <button
          @touchstart.prevent="preview = true"
          @touchend="preview = false"
          @touchcancel="preview = false"
          @mousedown="preview = true"
          @mouseup="preview = false"
          @mouseleave="preview = false"
        >
          👁 按住看原图
        </button>
        <button @click="showNums = !showNums">🔢 编号：{{ showNums ? '开' : '关' }}</button>
      </div>

      <img v-if="preview" class="preview" :src="current.url" alt="原图" />
    </div>

    <!-- 状态三：完成弹层 -->
    <div v-if="state === 'done'" class="mask">
      <span
        v-for="(c, i) in confetti"
        :key="i"
        class="confetti"
        :style="{ left: c.left, animationDelay: c.delay, animationDuration: c.dur }"
        >{{ c.emo }}</span
      >
      <div class="done-card">
        <div class="stars-big">
          <span v-for="i in 3" :key="i" :class="{ dim: i > stars }">⭐</span>
        </div>
        <h2>恭喜完成！</h2>
        <p class="done-info">用时 {{ fmt(time) }} · 共 {{ steps }} 步</p>
        <p class="done-tip">{{ starTip }}</p>
        <div class="done-btns">
          <button @click="restart">再来一次</button>
          <button @click="changeImg">换一张图</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGamesStore } from '@/stores/games'
import { PUZZLE_ARTS, renderArt } from '@/games/puzzleArt'

const router = useRouter()
const store = useGamesStore()

/* ---------- 难度 ---------- */
const DIFFS = [
  { n: 3, label: '简单' },
  { n: 4, label: '普通' },
  { n: 5, label: '困难' },
  { n: 6, label: '超难' }
] as const

/* ---------- 素材 ---------- */
interface ArtItem {
  name: string
  from: string
  url: string
}
const builtArts = ref<ArtItem[]>([])
const customArts = ref<ArtItem[]>([])
const allArts = computed(() => customArts.value.concat(builtArts.value))
const artIdx = ref(0)
const current = computed(() => allArts.value[artIdx.value])

onMounted(() => {
  builtArts.value = PUZZLE_ARTS.map((a) => ({
    name: a.name,
    from: a.from,
    url: renderArt(a, 512)
  }))
})

/** 上传照片：居中裁剪成正方形，缩到 512 */
function onUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const cv = document.createElement('canvas')
      cv.width = 512
      cv.height = 512
      const ctx = cv.getContext('2d')!
      const side = Math.min(img.width, img.height)
      ctx.drawImage(
        img,
        (img.width - side) / 2,
        (img.height - side) / 2,
        side,
        side,
        0,
        0,
        512,
        512
      )
      customArts.value.unshift({
        name: '我的照片',
        from: '上传',
        url: cv.toDataURL('image/jpeg', 0.9)
      })
      artIdx.value = 0 // 选中新上传的图
    }
    img.src = reader.result as string
  }
  reader.readAsDataURL(file)
  input.value = '' // 允许重复上传同一张
}

/* ---------- 游戏状态 ---------- */
const state = ref<'pick' | 'playing' | 'done'>('pick')
const n = ref(3)
const order = ref<number[]>([]) // order[显示位] = 该块的原图编号
const initialMisplaced = ref(1) // 开局错位块数（星级基准）
const steps = ref(0)
const time = ref(0)
let timer = 0
const showNums = ref(false)
const preview = ref(false)
const boardRef = ref<HTMLElement | null>(null)
const boardPx = ref(420)
const stars = ref(1)

const cellPx = computed(() => boardPx.value / n.value)

/** 每块的背景偏移：按其原图编号定位 */
function bgPos(home: number): string {
  const col = home % n.value
  const row = Math.floor(home / n.value)
  if (n.value === 1) return 'center'
  return ((col / (n.value - 1)) * 100).toFixed(4) + '% ' + ((row / (n.value - 1)) * 100).toFixed(4) + '%'
}

function tileStyle(pos: number) {
  return {
    backgroundImage: 'url(' + current.value.url + ')',
    backgroundSize: n.value * 100 + '%',
    backgroundPosition: bgPos(order.value[pos])
  }
}

function tileClass(pos: number) {
  return {
    correct: order.value[pos] === pos,
    sel: selPos.value === pos,
    dragging: dragFrom.value === pos && ghost.value.show,
    snap: snapPos.value === pos
  }
}

/* ---------- 计时 ---------- */
function startTimer() {
  stopTimer()
  timer = window.setInterval(() => (time.value += 1), 1000)
}
function stopTimer() {
  if (timer) {
    window.clearInterval(timer)
    timer = 0
  }
}
function fmt(t: number): string {
  const m = Math.floor(t / 60)
  const s = t % 60
  return (m < 10 ? '0' + m : '' + m) + ':' + (s < 10 ? '0' + s : '' + s)
}

/* ---------- 开始 / 打乱 ---------- */
function shuffleOrder() {
  const total = n.value * n.value
  // Fisher-Yates 打乱，保证错位块 >= 80%（且不是已完成状态）
  let arr: number[] = []
  do {
    arr = Array.from({ length: total }, (_, i) => i)
    for (let i = total - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      const tmp = arr[i]
      arr[i] = arr[j]
      arr[j] = tmp
    }
  } while (arr.filter((v, i) => v !== i).length < Math.floor(total * 0.8))
  order.value = arr
  initialMisplaced.value = arr.filter((v, i) => v !== i).length
}

function start() {
  boardPx.value = Math.min(420, window.innerWidth - 56)
  shuffleOrder()
  steps.value = 0
  time.value = 0
  selPos.value = -1
  state.value = 'playing'
  startTimer()
}

function restart() {
  state.value = 'playing'
  start() // 同图同难度重打乱，计时步数清零
}

function changeImg() {
  stopTimer()
  state.value = 'pick'
}

function goBack() {
  stopTimer()
  router.replace('/games')
}

/* ---------- 交换与完成 ---------- */
const selPos = ref(-1)
const snapPos = ref(-1)
let snapTimer = 0

function doSwap(a: number, b: number) {
  if (a === b) return
  const tmp = order.value[a]
  order.value[a] = order.value[b]
  order.value[b] = tmp
  steps.value += 1
  selPos.value = -1
  // 轻微吸附动画：刚归位的块闪一下
  if (order.value[a] === a) {
    snapPos.value = a
    window.clearTimeout(snapTimer)
    snapTimer = window.setTimeout(() => (snapPos.value = -1), 450)
  }
  if (order.value.every((v, i) => v === i)) finish()
}

function finish() {
  stopTimer()
  // 星级：步数效率（基准 = 开局错位块数）
  const base = initialMisplaced.value
  stars.value = steps.value <= base * 1.3 ? 3 : steps.value <= base * 1.8 ? 2 : 1
  store.complete('puzzle', 'puzzle-' + n.value, stars.value)
  makeConfetti()
  state.value = 'done'
}

const starTip = computed(() =>
  stars.value === 3 ? '太厉害了，步骤超高效！' : stars.value === 2 ? '很棒，再少走几步就是三星！' : '完成就是胜利，继续加油！'
)

/* ---------- 撒花 ---------- */
const confetti = ref<Array<{ left: string; delay: string; dur: string; emo: string }>>([])
const EMO = ['🎉', '⭐', '🎊', '✨', '🎈']
function makeConfetti() {
  const list: Array<{ left: string; delay: string; dur: string; emo: string }> = []
  for (let i = 0; i < 18; i++) {
    list.push({
      left: Math.random() * 92 + 4 + '%',
      delay: (Math.random() * 1.2).toFixed(2) + 's',
      dur: (2 + Math.random() * 1.5).toFixed(2) + 's',
      emo: EMO[i % EMO.length]
    })
  }
  confetti.value = list
}

/* ---------- 拖拽（touch + mouse 双通道，兼容 iPad） ---------- */
const ghost = ref({ show: false, x: 0, y: 0, home: 0 })
const dragFrom = ref(-1)
const dragTarget = ref(-1)
let startX = 0
let startY = 0
let dragMoved = false

const ghostStyle = computed(() => ({
  left: ghost.value.x - cellPx.value / 2 + 'px',
  top: ghost.value.y - cellPx.value / 2 + 'px',
  width: cellPx.value + 'px',
  height: cellPx.value + 'px',
  backgroundImage: 'url(' + current.value.url + ')',
  backgroundSize: n.value * 100 + '%',
  backgroundPosition: bgPos(ghost.value.home)
}))

function ptOf(e: Event): { x: number; y: number } {
  const t = e as TouchEvent
  if (t.touches && t.touches.length > 0) {
    return { x: t.touches[0].clientX, y: t.touches[0].clientY }
  }
  const m = e as MouseEvent
  return { x: m.clientX, y: m.clientY }
}

function onDown(pos: number, e: Event) {
  if (state.value !== 'playing') return
  const p = ptOf(e)
  startX = p.x
  startY = p.y
  dragMoved = false
  dragFrom.value = pos
  dragTarget.value = -1
  const isTouch = !!(e as TouchEvent).touches
  if (isTouch) {
    document.addEventListener('touchmove', onMove, { passive: false })
    document.addEventListener('touchend', onUp)
    document.addEventListener('touchcancel', onUp)
  } else {
    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
  }
}

function onMove(e: Event) {
  if (dragFrom.value < 0) return
  const p = ptOf(e)
  const dx = p.x - startX
  const dy = p.y - startY
  if (!dragMoved && Math.abs(dx) < 8 && Math.abs(dy) < 8) return
  dragMoved = true
  if ((e as TouchEvent).cancelable) e.preventDefault() // 拖拽时禁止页面滚动
  ghost.value = { show: true, x: p.x, y: p.y, home: order.value[dragFrom.value] }
  // 指针落在哪个格子
  const rect = boardRef.value ? boardRef.value.getBoundingClientRect() : null
  if (rect) {
    const col = Math.floor((p.x - rect.left) / cellPx.value)
    const row = Math.floor((p.y - rect.top) / cellPx.value)
    dragTarget.value =
      col >= 0 && col < n.value && row >= 0 && row < n.value ? row * n.value + col : -1
  }
}

function onUp() {
  document.removeEventListener('touchmove', onMove)
  document.removeEventListener('touchend', onUp)
  document.removeEventListener('touchcancel', onUp)
  document.removeEventListener('mousemove', onMove)
  document.removeEventListener('mouseup', onUp)
  const from = dragFrom.value
  const target = dragTarget.value
  dragFrom.value = -1
  dragTarget.value = -1
  ghost.value.show = false
  if (state.value !== 'playing') return
  if (dragMoved) {
    // 拖拽交换：松手落到目标格
    if (target >= 0 && target !== from) doSwap(from, target)
  } else {
    // 轻点：两块交换玩法
    if (selPos.value === -1) {
      selPos.value = from
    } else if (selPos.value === from) {
      selPos.value = -1
    } else {
      doSwap(selPos.value, from)
    }
  }
}

onUnmounted(() => {
  stopTimer()
  window.clearTimeout(snapTimer)
  document.removeEventListener('touchmove', onMove)
  document.removeEventListener('touchend', onUp)
  document.removeEventListener('touchcancel', onUp)
  document.removeEventListener('mousemove', onMove)
  document.removeEventListener('mouseup', onUp)
})
</script>

<style scoped>
.puzzle-page {
  max-width: 560px;
  margin: 0 auto;
}

.pz-top {
  text-align: center;
  padding: 22px 0 10px;
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

.pz-title {
  font-size: 26px;
  font-weight: 800;
}

.sec-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
  margin: 16px 4px 10px;
}

/* ---- 素材九宫格 ---- */
.art-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.art-card {
  border-radius: 14px;
  background: var(--card-2);
  border: 2px solid var(--border);
  padding: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform 0.12s ease;
}

.art-card:active {
  transform: scale(0.97);
}

.art-card.sel {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.25);
}

.art-card img {
  width: 100%;
  border-radius: 10px;
  display: block;
}

.art-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
  margin-top: 5px;
}

.art-from {
  font-size: 11px;
  color: var(--text-sub);
}

.upload {
  justify-content: center;
  min-height: 120px;
  cursor: pointer;
}

.upload input {
  display: none;
}

.up-ico {
  font-size: 34px;
}

/* ---- 难度 ---- */
.diff-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.diff-btn {
  border-radius: 14px;
  background: var(--card-2);
  border: 2px solid var(--border);
  padding: 12px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.diff-btn b {
  font-size: 17px;
  color: var(--text-main);
}

.diff-btn span {
  font-size: 12px;
  color: var(--text-sub);
}

.diff-btn.sel {
  border-color: var(--primary);
  background: rgba(64, 158, 255, 0.12);
}

.start-btn {
  display: block;
  width: 100%;
  margin: 22px 0 12px;
  min-height: 54px;
  border-radius: 16px;
  background: var(--primary);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}

/* ---- 拼图进行中 ---- */
.stat-bar {
  display: flex;
  justify-content: center;
  gap: 18px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
  padding: 8px 0 12px;
}

.board-wrap {
  display: flex;
  justify-content: center;
  position: relative;
}

.board {
  display: grid;
  gap: 3px;
  background: var(--card-2);
  border: 2px solid var(--border);
  border-radius: 14px;
  padding: 6px;
  touch-action: none; /* 拖块时不滚动页面（新浏览器） */
  user-select: none;
  -webkit-user-select: none;
}

.cell {
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.cell.hint {
  box-shadow: inset 0 0 0 3px #ffd54f;
}

.tile {
  position: absolute;
  inset: 0;
  background-repeat: no-repeat;
  border-radius: 8px;
  cursor: grab;
  transition: box-shadow 0.15s ease;
}

.tile.correct {
  box-shadow: inset 0 0 0 2px rgba(76, 175, 80, 0.9);
}

.tile.sel {
  box-shadow: inset 0 0 0 3px #ff9800;
}

.tile.dragging {
  opacity: 0.35;
}

.tile.snap {
  animation: snapIn 0.4s ease;
}

@keyframes snapIn {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(0.9);
  }
  100% {
    transform: scale(1);
  }
}

.num {
  position: absolute;
  left: 3px;
  top: 2px;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 6px;
  padding: 0 4px;
  line-height: 16px;
}

.ghost {
  position: fixed;
  border-radius: 8px;
  pointer-events: none;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.35);
  transform: scale(1.08);
  z-index: 30;
  background-repeat: no-repeat;
}

/* ---- 底部操作 ---- */
.ctrl-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

.ctrl-row button {
  min-height: 48px;
  border-radius: 14px;
  background: var(--card-2);
  border: 1px solid var(--border);
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.preview {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: min(76vw, 360px);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
  z-index: 40;
}

/* ---- 完成弹层 ---- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  overflow: hidden;
}

.done-card {
  background: var(--card-2);
  border-radius: 22px;
  padding: 30px 34px;
  text-align: center;
  z-index: 2;
  animation: popIn 0.35s ease;
}

@keyframes popIn {
  0% {
    transform: scale(0.6);
    opacity: 0;
  }
  70% {
    transform: scale(1.06);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.stars-big {
  font-size: 40px;
  letter-spacing: 6px;
}

.stars-big .dim {
  filter: grayscale(1);
  opacity: 0.35;
}

.done-card h2 {
  font-size: 24px;
  margin-top: 8px;
  color: var(--text-main);
}

.done-info {
  font-size: 16px;
  color: var(--text-main);
  margin-top: 6px;
  font-weight: 700;
}

.done-tip {
  font-size: 13px;
  color: var(--text-sub);
  margin-top: 4px;
}

.done-btns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 18px;
}

.done-btns button {
  min-height: 48px;
  border-radius: 14px;
  background: var(--primary);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.confetti {
  position: absolute;
  top: -40px;
  font-size: 26px;
  animation: fall linear forwards;
  z-index: 1;
}

@keyframes fall {
  to {
    transform: translateY(110vh) rotate(360deg);
  }
}
</style>
