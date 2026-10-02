<template>
  <transition name="sheet">
    <div v-if="open" class="guide-mask" @click.self="emit('close')">
      <aside class="guide-sheet" role="dialog" aria-label="课程目录">
        <header class="guide-head">
            <span class="guide-title">📒 课程目录</span>
            <button class="guide-close" aria-label="关闭" @click="emit('close')">✕</button>
        </header>

        <div class="guide-body">
          <section v-for="unit in units" :key="unit.id" class="g-unit">
            <div class="g-unit-head">
              <span class="g-unit-no">{{ unitLabel(unit) }}</span>
              <span class="g-unit-stars">⭐ {{ unit.stars }}</span>
            </div>

            <button
              v-for="lesson in unit.lessons"
              :key="lesson.id"
              class="g-lesson"
              :class="{ 'g-lesson--lock': lesson.locked, 'g-lesson--current': isCurrent(lesson) }"
              @click="pick(lesson)"
            >
              <span class="g-lesson-name">
                <span v-if="lesson.lessonType === 'GARDEN'">🌸 语文园地</span>
                <span v-else-if="lesson.lessonType === 'PRACTICE'">{{ store.subject === 'english' ? '🔁 综合复习' : '🧭 综合实践' }}</span>
                <span v-else-if="lesson.lessonType === 'FUN'">🎲 数学好玩</span>
                <template v-else>
                  <span class="g-lesson-no">{{ lesson.lessonNo }}</span>
                  {{ lesson.title }}
                  <span v-if="lesson.isSkim" class="badge badge-skim g-skim">略读</span>
                </template>
              </span>
              <span class="g-lesson-meta">
                <span class="g-lesson-stars">⭐{{ lessonStars(lesson) }}</span>
                <span class="g-lesson-state">{{ stateIcon(lesson) }}</span>
              </span>
            </button>
          </section>
        </div>
      </aside>
    </div>
  </transition>
</template>

<script setup lang="ts">
import type { LessonNode, UnitNode } from '@/types/api'
import { useChildStore } from '@/stores/child'

const props = defineProps<{
  open: boolean
  units: UnitNode[]
  currentLevelId: number | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'jump', lessonId: number): void
}>()

const store = useChildStore()

/** 单元标题（v2.6：英语显示教材原名 Unit 1~6 / Revision） */
function unitLabel(unit: UnitNode): string {
  return store.subject === 'english' ? unit.title : `第${unit.unitNo}单元`
}

function lessonStars(lesson: LessonNode): number {
  return lesson.levels.reduce((sum, lv) => sum + (lv.stars || 0), 0)
}

function isCurrent(lesson: LessonNode): boolean {
  return lesson.levels.some((lv) => lv.id === props.currentLevelId)
}

function stateIcon(lesson: LessonNode): string {
  if (lesson.completed) return '✅'
  if (lesson.locked) return '🔒'
  return '▶️'
}

function pick(lesson: LessonNode) {
  if (lesson.locked) return
  emit('jump', lesson.id)
}
</script>

<style scoped>
.guide-mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(6, 12, 16, 0.55);
  display: flex;
  justify-content: flex-end;
}

.guide-sheet {
  width: 100%;
  max-width: 380px;
  height: 100%;
  background: var(--card);
  border-left: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  box-shadow: -12px 0 40px rgba(0, 0, 0, 0.45);
}

.guide-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--border);
}

.guide-title {
  font-size: 20px;
  font-weight: 800;
}

.guide-close {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--card-2);
  color: var(--text-sub);
  font-size: 16px;
  font-weight: 700;
}

.guide-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px calc(24px + env(safe-area-inset-bottom));
}

.g-unit {
  margin-bottom: 18px;
}

.g-unit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px 8px;
}

.g-unit-no {
  font-size: 15px;
  font-weight: 800;
  color: #ffb347;
}

.g-unit-stars {
  font-size: 14px;
  color: var(--text-sub);
}

.g-lesson {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 52px;
  padding: 8px 12px;
  border-radius: 14px;
  border: 2px solid transparent;
  text-align: left;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.g-lesson:active {
  background: var(--card-2);
}

.g-lesson--current {
  border-color: var(--warning);
  background: rgba(255, 150, 0, 0.12);
}

.g-lesson--lock {
  opacity: 0.55;
}

.g-lesson-name {
  font-size: 16px;
  font-weight: 700;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.g-lesson-no {
  flex: none;
  color: var(--text-sub);
  font-weight: 800;
}

.g-skim {
  font-size: 11px;
  padding: 1px 7px;
}

.g-lesson-meta {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-sub);
}

/* 右侧滑入/滑出 */
.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.2s ease;
}

.sheet-enter-active .guide-sheet,
.sheet-leave-active .guide-sheet {
  transition: transform 0.22s ease;
}

.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}

.sheet-enter-from .guide-sheet,
.sheet-leave-to .guide-sheet {
  transform: translateX(100%);
}
</style>
