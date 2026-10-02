<template>
  <div class="map-page">
    <TabBar current="map" />

    <!-- ===== 吸顶状态区：学科标 / 连续天数 / 积分 / 星数 + 额度细条 + 单元横幅 ===== -->
    <header class="map-top">
      <div class="stat-bar">
        <button class="stat flag" title="切换科目" @click="router.push('/subject')">
          {{ subjectIco }}
        </button>
        <span class="stat" title="连续学习天数">
          <span class="stat-ico">🔥</span>
          <span class="stat-num">{{ store.streakDays }}</span>
        </span>
        <span class="stat" title="积分">
          <span class="stat-ico">💎</span>
          <span class="stat-num">{{ store.points }}</span>
        </span>
        <span class="stat" title="总星数">
          <span class="stat-ico">⭐</span>
          <span class="stat-num">{{ mapData?.totalStars ?? 0 }}</span>
        </span>
      </div>

      <!-- 今日额度细条（F-H5-10） -->
      <div v-if="store.today" class="quota-strip" :class="{ exhausted: store.today.limitReached }">
        <template v-if="store.today.limitReached">
          <span class="quota-msg">🌙 今天学得很棒，明天再来吧！</span>
        </template>
        <template v-else>
          <span class="q-seg q-seg-q">
            <i class="q-fill" :style="{ width: qPercent + '%' }"></i>
          </span>
          <span class="q-seg q-seg-m">
            <i class="q-fill" :style="{ width: mPercent + '%' }"></i>
          </span>
          <span class="q-txt">
            {{ store.today.answeredToday }}/{{ store.today.questionLimit }}题 ·
            {{ store.today.usedMinutesToday }}/{{ store.today.minuteLimit }}分
          </span>
        </template>
      </div>

      <!-- 当前单元/课横幅 -->
      <div v-if="banner.unit" class="unit-banner">
        <div class="ub-text">
          <div class="ub-line1">{{ bannerLine1 }}</div>
          <div class="ub-line2">{{ bannerLine2 }}</div>
        </div>
        <button class="ub-book" aria-label="打开课程目录" @click="guideOpen = true">📒</button>
      </div>
    </header>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      地图加载中…
    </div>

    <template v-else-if="mapData">
      <LevelPath
        ref="pathRef"
        :units="mapData.units"
        :current-level-id="mapData.currentLevelId"
        :avatar="store.child?.avatar ?? ''"
        @node-click="openLevel"
      />

      <button class="jump-bottom" aria-label="回到当前关" @click="backToCurrent">⬇️</button>
    </template>

    <LessonGuide
      :open="guideOpen"
      :units="mapData?.units ?? []"
      :current-level-id="mapData?.currentLevelId ?? null"
      @close="guideOpen = false"
      @jump="jumpLesson"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TabBar from '@/components/TabBar.vue'
import LevelPath from '@/components/LevelPath.vue'
import LessonGuide from '@/components/LessonGuide.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'
import type { GameMap, LevelNode, LessonNode, UnitNode } from '@/types/api'

const router = useRouter()
const store = useChildStore()

const loading = ref(true)
const mapData = ref<GameMap | null>(null)
const guideOpen = ref(false)
const pathRef = ref<InstanceType<typeof LevelPath> | null>(null)

const activeUnitId = ref<number | null>(null)
const activeLessonId = ref<number | null>(null)

const qPercent = computed(() => {
  const t = store.today
  if (!t || t.questionLimit === 0) return 0
  return Math.min(100, (t.answeredToday / t.questionLimit) * 100)
})

const mPercent = computed(() => {
  const t = store.today
  if (!t || t.minuteLimit === 0) return 0
  return Math.min(100, (t.usedMinutesToday / t.minuteLimit) * 100)
})

const banner = computed(() => {
  const unit = mapData.value?.units.find((u) => u.id === activeUnitId.value) ?? null
  let lesson: LessonNode | null = null
  if (unit) {
    lesson = unit.lessons.find((l) => l.id === activeLessonId.value) ?? unit.lessons[0] ?? null
  }
  return { unit, lesson }
})

const bannerLine1 = computed(() => {
  const { unit, lesson } = banner.value
  if (!unit || !lesson) return ''
  if (store.subject === 'math') {
    // 数学：普通课文按课号，实践活动/数学好玩板块直接显示板块名
    if (lesson.lessonType === 'PRACTICE') return `第 ${unit.unitNo} 单元 · 综合实践`
    if (lesson.lessonType === 'FUN') return `第 ${unit.unitNo} 单元 · 数学好玩`
    return `第 ${unit.unitNo} 单元 · 第 ${lesson.lessonNo} 课`
  }
  if (store.subject === 'english') {
    // 英语（v2.6）：单元标题用教材原名（Unit 1~6 / Revision）
    if (lesson.lessonType === 'PRACTICE') return `${unit.title} · 综合复习`
    return `${unit.title} · 第 ${lesson.lessonNo} 课`
  }
  if (lesson.lessonType === 'GARDEN') return `第 ${unit.unitNo} 单元 · 语文园地`
  return `第 ${unit.unitNo} 单元 · 第 ${lesson.lessonNo} 课`
})

/** 学科标图标（v2.6：三科） */
const subjectIco = computed(() =>
  store.subject === 'math' ? '🧮' : store.subject === 'english' ? '🔤' : '📖'
)

const bannerLine2 = computed(() => banner.value.lesson?.title ?? '')

/** 在树中定位某关卡所属的单元/课（以稳定 ID 关联，不依赖拍平索引） */
function locateLevel(levelId: number | null): { unit: UnitNode | null; lesson: LessonNode | null } {
  if (levelId == null || !mapData.value) return { unit: null, lesson: null }
  for (const u of mapData.value.units) {
    for (const l of u.lessons) {
      if (l.levels.some((lv) => lv.id === levelId)) return { unit: u, lesson: l }
    }
  }
  return { unit: null, lesson: null }
}

function openLevel(lv: LevelNode) {
  if (lv.status === 'LOCKED') {
    showToast('这一关还没解锁，先通过前面的关卡吧', 'info')
    return
  }
  if (store.today?.limitReached) {
    showToast('今天学得很棒，明天再来吧！', 'info')
    return
  }
  router.push(`/play/${lv.id}`)
}

function jumpLesson(lessonId: number) {
  guideOpen.value = false
  const lessonLocked = mapData.value?.units
    .flatMap((u) => u.lessons)
    .some((l) => l.id === lessonId && l.locked)
  if (lessonLocked) {
    showToast('先通过前面的内容吧', 'info')
    return
  }
  nextTick(() => pathRef.value?.scrollToLesson(lessonId))
}

function backToCurrent() {
  if (mapData.value?.currentLevelId != null) {
    pathRef.value?.scrollToLevel(mapData.value.currentLevelId, true)
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

/* ---------- 滚动监听：更新横幅为当前可视位置所属的单元/课 ---------- */

let scrollRaf = 0

function refreshBannerByScroll() {
  scrollRaf = 0
  const sections = Array.from(
    document.querySelectorAll<HTMLElement>('[data-p-section]')
  ).filter((el) => (el.getAttribute('data-p-section') ?? '') !== '')

  let hit: string | null = null
  for (const el of sections) {
    if (el.getBoundingClientRect().top <= 150) {
      hit = el.getAttribute('data-p-section')
    } else {
      break
    }
  }
  if (!hit) return
  if (hit.startsWith('unit:')) {
    activeUnitId.value = Number(hit.slice(5))
    // 单元标记之后、该单元第一课分隔条出现前，横幅显示该单元第一课
    activeLessonId.value = null
  } else if (hit.startsWith('lesson:')) {
    const [lessonPart, unitPart] = hit.split('@')
    activeLessonId.value = Number(lessonPart.slice(7))
    activeUnitId.value = Number(unitPart)
  }
}

function onScroll() {
  if (!scrollRaf) scrollRaf = requestAnimationFrame(refreshBannerByScroll)
}

onMounted(async () => {
  try {
    const [, map] = await Promise.all([store.fetchProfile(), childApi.map(store.subject || 'chinese')])
    mapData.value = map
    // 横幅初始定位到当前关所在课；全部通关则第一单元
    const { unit, lesson } = locateLevel(map.currentLevelId)
    activeUnitId.value = unit?.id ?? map.units[0]?.id ?? null
    activeLessonId.value = lesson?.id ?? map.units[0]?.lessons[0]?.id ?? null

    await nextTick()
    // 自动定位当前关到屏幕中部；全部通关时定位到末尾
    setTimeout(() => {
      if (map.currentLevelId != null) {
        pathRef.value?.scrollToLevel(map.currentLevelId, false)
      } else {
        window.scrollTo({ top: document.body.scrollHeight })
      }
    }, 60)

    window.addEventListener('scroll', onScroll, { passive: true })
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
})
</script>

<style scoped>
.map-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 12px 14px calc(110px + env(safe-area-inset-bottom));
}

/* ---- 吸顶状态区 ---- */
.map-top {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 8px 4px 10px;
  margin: 0 -4px 8px;
  background: linear-gradient(to bottom, var(--bg) 82%, rgba(19, 31, 36, 0));
}

.stat-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 4px 8px 10px;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 17px;
  font-weight: 800;
}

.stat-ico {
  font-size: 20px;
  line-height: 1;
}

.stat.flag {
  font-size: 22px;
}

/* ---- 额度细条 ---- */
.quota-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 4px 10px;
}

.q-seg {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background: #2c3a44;
  overflow: hidden;
}

.q-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}

.q-seg-q .q-fill {
  background: var(--primary);
}

.q-seg-m .q-fill {
  background: var(--info);
}

.q-txt {
  flex: none;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-sub);
  white-space: nowrap;
}

.quota-strip.exhausted {
  display: block;
  background: var(--warning-soft);
  border: 1px solid #8a5a1e;
  border-radius: 12px;
  padding: 8px 14px;
}

.quota-msg {
  font-size: 14px;
  font-weight: 700;
  color: #ffb347;
}

/* ---- 单元横幅 ---- */
.unit-banner {
  display: flex;
  align-items: stretch;
  gap: 0;
  margin: 0 4px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(180deg, #ffa41b 0%, #f5870b 100%);
  border-bottom: 5px solid #d9720a;
  box-shadow: 0 8px 20px rgba(216, 114, 10, 0.25);
}

.ub-text {
  flex: 1;
  padding: 12px 18px;
  color: #fff;
  min-width: 0;
}

.ub-line1 {
  font-size: 14px;
  font-weight: 700;
  opacity: 0.92;
}

.ub-line2 {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ub-book {
  flex: none;
  width: 58px;
  font-size: 26px;
  border-left: 2px solid rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.08);
}

.ub-book:active {
  background: rgba(255, 255, 255, 0.2);
}

/* ---- 回到当前关 ---- */
.jump-bottom {
  position: fixed;
  right: max(18px, calc(50% - 250px));
  bottom: calc(96px + env(safe-area-inset-bottom));
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: #273742;
  border: 2px solid var(--border-light);
  font-size: 20px;
  z-index: 40;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
}

.jump-bottom:active {
  transform: translateY(2px);
}
</style>
