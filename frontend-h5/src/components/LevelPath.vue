<template>
  <div ref="rootRef" class="path" :style="{ height: totalHeight + 'px' }">
    <!-- 穿过所有节点中心的弯曲点状虚线（底层） -->
    <svg class="path-lines" :width="width" :height="totalHeight" aria-hidden="true">
      <path :d="linePath" :stroke="lineColor" stroke-width="4" stroke-linecap="round"
            stroke-dasharray="0.1 13" fill="none" />
    </svg>

    <!-- 路径元素（固定行高，行内按 slot 百分比横向摆位） -->
    <div
      v-for="item in items"
      :id="item.domId"
      :key="item.key"
      class="p-row"
      :class="`p-row--${item.kind}`"
      :style="rowStyle(item)"
      :data-p-section="sectionAttr(item)"
    >
      <!-- 单元小标记 -->
      <template v-if="item.kind === 'unit'">
        <div class="p-unit-pill">
          <span class="p-unit-no">{{ unitLabel(item.unit).no }}</span>
          <span v-if="unitLabel(item.unit).name" class="p-unit-name">{{ unitLabel(item.unit).name }}</span>
        </div>
      </template>

      <!-- 课名分隔条 -->
      <template v-else-if="item.kind === 'lesson'">
        <div class="p-lesson-line" :class="{ 'is-locked': item.lesson.locked }">
          <span class="p-lesson-rule"></span>
          <span class="p-lesson-txt">
            <template v-if="item.lesson.lessonType === 'GARDEN'">🌸 语文园地</template>
            <template v-else-if="item.lesson.lessonType === 'PRACTICE'">{{ store.subject === 'english' ? '🔁 综合复习' : '🧭 综合实践' }}</template>
            <template v-else-if="item.lesson.lessonType === 'FUN'">🎲 数学好玩</template>
            <template v-else>
              <span class="p-lesson-no">第{{ item.lesson.lessonNo }}课</span>
              {{ item.lesson.title }}
              <span v-if="item.lesson.isSkim" class="badge badge-skim p-skim">略读</span>
            </template>
          </span>
          <span class="p-lesson-rule"></span>
        </div>
      </template>

      <!-- 关卡圆形节点 -->
      <template v-else>
        <div class="p-node-slot" :style="{ left: item.xPct + '%' }">
          <!-- 当前关吉祥物（气泡在头像上方，竖排以保证窄屏不溢出） -->
          <div
            v-if="item.level.id === currentLevelId"
            class="p-mascot"
            :class="`p-mascot--${mascotSide(item)}`"
          >
            <span class="p-bubble">继续闯关吧！</span>
            <AvatarFace :avatar="avatar" size="sm" />
          </div>

          <button
            class="p-node"
            :class="{
              'p-node--passed': item.level.status === 'PASSED',
              'p-node--current': item.level.id === currentLevelId,
              'p-node--locked': item.level.status === 'LOCKED',
              'p-node--trophy': item.trophy
            }"
            :aria-label="nodeAria(item)"
            @click="emit('node-click', item.level)"
          >
            <span v-if="item.level.status === 'LOCKED'" class="p-node-ico p-ico-lock">🔒</span>
            <template v-else-if="item.trophy">
              <span class="p-node-ico">{{ item.level.status === 'PASSED' ? '🏆' : '🎁' }}</span>
            </template>
            <span v-else-if="item.level.status === 'PASSED'" class="p-node-star">★</span>
            <span v-else class="p-node-ico">{{ item.icon }}</span>
          </button>

          <div v-if="item.level.status !== 'LOCKED'" class="p-stars">
            <span
              v-for="i in 3"
              :key="i"
              class="p-mini-star"
              :class="{ on: i <= item.level.stars }"
            >★</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AvatarFace from '@/components/AvatarFace.vue'
import { useChildStore } from '@/stores/child'
import type { LevelNode, LessonNode, UnitNode } from '@/types/api'

const props = withDefaults(
  defineProps<{
    units: UnitNode[]
    currentLevelId: number | null
    avatar?: string
  }>(),
  { avatar: '', currentLevelId: null }
)

const emit = defineEmits<{
  (e: 'node-click', level: LevelNode): void
}>()

const store = useChildStore()

/** 单元 pill 文案（v2.6：英语单元显示教材原名 Unit 1~6 / Revision） */
function unitLabel(unit: UnitNode): { no: string; name: string } {
  if (store.subject === 'english') return { no: unit.title, name: '' }
  return { no: `第${unit.unitNo}单元`, name: unit.title.replace(/^第.单元/, '') }
}

/* 每行固定高度（CSS 与此处常量必须一致，SVG 才能对准节点中心）
   节点中心统一在行内 58px 处：普通节点 top=24(68px)，当前关 top=18(80px) */
const H_UNIT = 62
const H_LESSON = 66
const H_NODE = 116

const ICON_CYCLE = ['📖', '🎧', '⭐', '🎮']
const SLOT_PCT = [50, 24, 50, 76]

type PathItem =
  | { kind: 'unit'; key: string; domId: string; unit: UnitNode; h: number }
  | { kind: 'lesson'; key: string; domId: string; lesson: LessonNode; unitNo: number; h: number }
  | {
      kind: 'node'
      key: string
      domId: string
      level: LevelNode
      icon: string
      trophy: boolean
      xPct: number
      h: number
    }

/** 单元 → 课 → 关卡 拍平为路径元素序列（key/锚点全部使用稳定业务 ID，不用索引） */
const items = computed<PathItem[]>(() => {
  const out: PathItem[] = []
  props.units.forEach((unit, ui) => {
    out.push({
      kind: 'unit',
      key: `unit-${unit.id}`,
      domId: `p-unit-${unit.id}`,
      unit,
      h: H_UNIT
    })
    const lastLessonId = unit.lessons.length ? unit.lessons[unit.lessons.length - 1].id : null
    unit.lessons.forEach((lesson, li) => {
      if (!(ui === 0 && li === 0)) {
        out.push({
          kind: 'lesson',
          key: `lesson-${lesson.id}`,
          domId: `p-lesson-${lesson.id}`,
          lesson,
          unitNo: unit.unitNo,
          h: H_LESSON
        })
      }
      lesson.levels.forEach((level, vi) => {
        const garden = lesson.lessonType === 'GARDEN'
        const trophy = lesson.id === lastLessonId && vi === lesson.levels.length - 1
        out.push({
          kind: 'node',
          key: `node-${level.id}`,
          domId: `p-node-${level.id}`,
          level,
          icon: garden ? '🌸' : ICON_CYCLE[vi % ICON_CYCLE.length],
          trophy,
          xPct: trophy ? 50 : SLOT_PCT[vi % SLOT_PCT.length],
          h: H_NODE
        })
      })
    })
  })
  return out
})

const totalHeight = computed(() => items.value.reduce((sum, it) => sum + it.h, 0) + 24)

function rowStyle(item: PathItem) {
  return { height: item.h + 'px' }
}

function sectionAttr(item: PathItem): string {
  if (item.kind === 'unit') return `unit:${item.unit.id}`
  if (item.kind === 'lesson') return `lesson:${item.lesson.id}@${item.unitNo}`
  return ''
}

function mascotSide(item: Extract<PathItem, { kind: 'node' }>): 'left' | 'right' {
  // 节点偏左→吉祥物在右；偏右→吉祥物在左；居中时按单元/课 id 奇偶稳定落位
  if (item.xPct < 40) return 'right'
  if (item.xPct > 60) return 'left'
  return item.level.id % 2 === 0 ? 'left' : 'right'
}

function nodeAria(item: Extract<PathItem, { kind: 'node' }>): string {
  const s =
    item.level.status === 'PASSED'
      ? `已通关，${item.level.stars}星`
      : item.level.status === 'LOCKED'
        ? '未解锁'
        : '当前可挑战'
  return `${item.level.name}，${s}`
}

/* ---------------- 容器宽度测量 + SVG 弯曲点状线 ---------------- */

const rootRef = ref<HTMLElement | null>(null)
const width = ref(320)
let ro: ResizeObserver | null = null

onMounted(() => {
  if (rootRef.value) {
    width.value = rootRef.value.clientWidth || 320
    ro = new ResizeObserver(() => {
      if (rootRef.value) width.value = rootRef.value.clientWidth
    })
    ro.observe(rootRef.value)
  }
})

onBeforeUnmount(() => ro?.disconnect())

/** Catmull-Rom 式平滑：控制点取相邻点的对侧坐标，形成在节点处收平的 S 曲线 */
const linePath = computed(() => {
  const w = width.value
  const pts: Array<{ x: number; y: number }> = []
  let acc = 0
  for (const it of items.value) {
    if (it.kind === 'node') {
      pts.push({ x: (w * it.xPct) / 100, y: acc + it.h / 2 })
    }
    acc += it.h
  }
  if (pts.length === 0) return ''
  let d = `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`
  for (let i = 1; i < pts.length; i++) {
    const p = pts[i]
    const prev = pts[i - 1]
    d += ` C ${prev.x.toFixed(1)} ${p.y.toFixed(1)}, ${p.x.toFixed(1)} ${prev.y.toFixed(1)}, ${p.x.toFixed(1)} ${p.y.toFixed(1)}`
  }
  return d
})

const lineColor = '#5b6770'

/* ---------------- 供父组件调用的平滑定位（锚点为稳定业务 ID） ---------------- */

function scrollToLevel(levelId: number, smooth: boolean) {
  document.getElementById(`p-node-${levelId}`)?.scrollIntoView({
    behavior: smooth ? 'smooth' : 'auto',
    block: 'center'
  })
}

function scrollToLesson(lessonId: number) {
  const el = document.getElementById(`p-lesson-${lessonId}`)
  if (!el) return
  const top = el.getBoundingClientRect().top + window.scrollY - 132
  window.scrollTo({ top, behavior: 'smooth' })
}

defineExpose({ scrollToLevel, scrollToLesson })
</script>

<style scoped>
.path {
  position: relative;
  width: 100%;
}

.path-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.p-row {
  position: relative;
  width: 100%;
}

/* ---- 单元小标记 ---- */
.p-unit-pill {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  background: rgba(31, 44, 52, 0.92);
  border: 1px solid var(--border-light);
  border-radius: 999px;
  padding: 5px 16px;
  white-space: nowrap;
  max-width: 92%;
}

.p-unit-no {
  font-size: 13px;
  font-weight: 800;
  color: #ffb347;
}

.p-unit-name {
  font-size: 13px;
  color: var(--text-sub);
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 课名分隔条 ---- */
.p-lesson-line {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 24px;
  background: var(--bg);
}

.p-lesson-rule {
  flex: 1;
  height: 2px;
  background: var(--border-light);
  border-radius: 2px;
}

.p-lesson-txt {
  flex: none;
  font-size: 16px;
  font-weight: 800;
  color: #cfd9dd;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.p-lesson-line.is-locked .p-lesson-txt {
  color: #77848d;
}

.p-lesson-no {
  color: var(--text-sub);
  font-weight: 700;
}

.p-skim {
  font-size: 12px;
  padding: 1px 8px;
}

/* ---- 节点 ---- */
.p-node-slot {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.p-node {
  width: 68px;
  height: 68px;
  margin-top: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 6px solid;
  transition: transform 0.1s ease;
  flex: none;
}

.p-node:active {
  transform: translateY(3px);
  border-bottom-width: 3px;
}

.p-node-ico {
  font-size: 31px;
  line-height: 1;
}

.p-ico-lock {
  filter: grayscale(1);
  opacity: 0.55;
}

.p-node-star {
  color: #fff;
  font-size: 34px;
  line-height: 1;
  text-shadow: 0 2px 0 rgba(0, 0, 0, 0.12);
}

/* 已通关：金色 */
.p-node--passed {
  background: linear-gradient(180deg, #ffd84d 0%, #ffc800 100%);
  border-bottom-color: #d9a400;
}

/* 当前关：橙色 + 放大 + 浮动 */
.p-node--current {
  width: 80px;
  height: 80px;
  margin-top: 18px;
  background: linear-gradient(180deg, #ffab2e 0%, #ff9600 100%);
  border-bottom-color: #d97c00;
  border-bottom-width: 7px;
  animation: node-float 1.8s ease-in-out infinite;
}

.p-node--current .p-node-ico {
  font-size: 36px;
}

.p-node--current:active {
  border-bottom-width: 4px;
}

@keyframes node-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-7px);
  }
}

/* 未解锁：灰 */
.p-node--locked {
  background: #3c484f;
  border-bottom-color: #2e3a41;
}

/* 节点下方小星 */
.p-stars {
  margin-top: 4px;
  display: flex;
  gap: 1px;
  line-height: 1;
}

.p-mini-star {
  font-size: 14px;
  color: #46545e;
}

.p-mini-star.on {
  color: var(--gold);
  text-shadow: 0 1px 2px rgba(255, 200, 0, 0.4);
}

/* ---- 当前关吉祥物（竖排：气泡在上、头像在下，窄屏也不溢出） ---- */
.p-mascot {
  position: absolute;
  top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  z-index: 2;
}

.p-mascot--left {
  right: calc(100% + 8px);
}

.p-mascot--right {
  left: calc(100% + 8px);
}

.p-bubble {
  position: relative;
  background: #f7f7f7;
  color: #2b3350;
  font-size: 12px;
  font-weight: 700;
  border-radius: 10px;
  padding: 3px 9px;
  white-space: nowrap;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  animation: bubble-pop 2.4s ease-in-out infinite;
}

.p-bubble::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  margin-left: -6px;
  border: 6px solid transparent;
  border-top-color: #f7f7f7;
}

@keyframes bubble-pop {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}
</style>
