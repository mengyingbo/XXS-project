<template>
  <div class="page">
    <TabBar current="map" />

    <!-- 顶部：孩子信息 + 积分 + 退出 -->
    <header class="topbar card">
      <AvatarFace :avatar="store.child?.avatar" size="md" />
      <div class="who">
        <div class="nick">{{ store.child?.nickname }}</div>
        <div class="stars-total">⭐ {{ mapData?.totalStars ?? 0 }} 颗星</div>
      </div>
      <div class="points-box">
        <span class="points-num">{{ store.points }}</span>
        <span class="points-label">积分</span>
      </div>
      <button class="logout" title="退出登录" @click="logout">退出</button>
    </header>

    <!-- 今日额度（F-H5-10） -->
    <div v-if="store.today" class="quota card" :class="{ exhausted: store.today.limitReached }">
      <template v-if="store.today.limitReached">
        <div class="quota-title">🌙 今天学得很棒，明天再来吧！</div>
        <div class="quota-desc">
          今天已答 {{ store.today.answeredToday }} 题 / 学习 {{ store.today.usedMinutesToday }} 分钟
        </div>
      </template>
      <template v-else>
        <div class="quota-title">今日进度</div>
        <div class="bars">
          <div class="bar-line">
            <span class="bar-label">题量</span>
            <div class="bar">
              <div
                class="bar-fill fill-q"
                :style="{ width: qPercent + '%' }"
              ></div>
            </div>
            <span class="bar-num">{{ store.today.answeredToday }}/{{ store.today.questionLimit }}</span>
          </div>
          <div class="bar-line">
            <span class="bar-label">时长</span>
            <div class="bar">
              <div
                class="bar-fill fill-m"
                :style="{ width: mPercent + '%' }"
              ></div>
            </div>
            <span class="bar-num">{{ store.today.usedMinutesToday }}/{{ store.today.minuteLimit }}分</span>
          </div>
        </div>
      </template>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      地图加载中…
    </div>

    <template v-else-if="mapData">
      <!-- 总进度 -->
      <div class="overall">
        已通关 {{ mapData.passedLevels }} / {{ mapData.totalLevels }} 关
      </div>

      <div class="units">
        <section v-for="(u, ui) in mapData.units" :key="u.id" class="unit card">
          <button class="unit-head" @click="toggle(ui)">
            <span class="unit-no">第{{ u.unitNo }}单元</span>
            <span class="unit-title">{{ u.title }}</span>
            <span class="unit-meta">
              <span v-if="u.completed">✅</span>
              <span class="unit-stars">⭐{{ u.stars }}</span>
              <span class="arrow" :class="{ open: opened[ui] }">▾</span>
            </span>
          </button>

          <div v-show="opened[ui]" class="lessons">
            <div
              v-for="lesson in u.lessons"
              :key="lesson.id"
              class="lesson"
              :class="{ 'lesson-lock': lesson.locked }"
            >
              <div class="lesson-info">
                <span class="lesson-title">
                  <span v-if="lesson.lessonType === 'GARDEN'" class="badge badge-garden">语文园地</span>
                  <span v-else>{{ lesson.lessonNo }} {{ lesson.title }}</span>
                  <span v-if="lesson.isSkim" class="badge badge-skim">略读</span>
                  <span v-if="lesson.completed && lesson.lessonType !== 'GARDEN'" class="done">✅</span>
                </span>
                <span v-if="lesson.lessonType === 'GARDEN'" class="garden-name">{{ lesson.title }}</span>
              </div>

              <div class="levels">
                <button
                  v-for="lv in lesson.levels"
                  :key="lv.id"
                  class="level"
                  :class="levelClass(lv.status)"
                  @click="openLevel(lv)"
                >
                  <template v-if="lv.status === 'LOCKED'">
                    <span class="lv-ico">🔒</span>
                  </template>
                  <template v-else-if="lv.status === 'PASSED'">
                    <StarRow :stars="lv.stars" size="sm" />
                  </template>
                  <template v-else>
                    <span class="lv-ico">▶️</span>
                  </template>
                  <span class="lv-name">{{ lv.levelNo }}关</span>
                </button>
              </div>
            </div>

            <div v-if="u.locked" class="unit-locked-tip">🔒 通关上一单元后解锁</div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import TabBar from '@/components/TabBar.vue'
import AvatarFace from '@/components/AvatarFace.vue'
import StarRow from '@/components/StarRow.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'
import type { GameMap, LevelNode, LevelStatus } from '@/types/api'

const router = useRouter()
const store = useChildStore()

const loading = ref(true)
const mapData = ref<GameMap | null>(null)
const opened = reactive<Record<number, boolean>>({})

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

function levelClass(status: LevelStatus) {
  return {
    'lv-locked': status === 'LOCKED',
    'lv-passed': status === 'PASSED',
    'lv-open': status === 'UNLOCKED'
  }
}

function toggle(i: number) {
  opened[i] = !opened[i]
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

function logout() {
  store.logout()
  router.replace('/')
}

onMounted(async () => {
  try {
    const [profile, map] = await Promise.all([
      store.fetchProfile(),
      childApi.map()
    ])
    mapData.value = map
    // 默认展开"当前应挑战"关卡所在单元
    const currentId = map.currentLevelId
    if (currentId != null) {
      const idx = map.units.findIndex((u) =>
        u.lessons.some((l) => l.levels.some((lv) => lv.id === currentId))
      )
      if (idx >= 0) opened[idx] = true
    } else {
      opened[0] = true
    }
    void profile
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
}

.who {
  flex: 1;
  min-width: 0;
}

.nick {
  font-size: 20px;
  font-weight: 700;
}

.stars-total {
  color: var(--text-sub);
  font-size: 14px;
}

.points-box {
  text-align: center;
  background: var(--warning-soft);
  border-radius: 14px;
  padding: 6px 14px;
  min-width: 64px;
}

.points-num {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: #c97c00;
  line-height: 1.2;
}

.points-label {
  font-size: 13px;
  color: #c97c00;
}

.logout {
  min-height: var(--min-tap);
  padding: 0 8px;
  color: var(--text-sub);
  font-size: 15px;
}

.quota {
  margin-top: 14px;
  padding: 14px 18px;
}

.quota.exhausted {
  background: var(--warning-soft);
}

.quota-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 8px;
}

.quota-desc {
  color: #c97c00;
  font-size: 15px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-label {
  width: 36px;
  font-size: 14px;
  color: var(--text-sub);
}

.bar {
  flex: 1;
  height: 12px;
  background: #edf0f8;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}

.fill-q {
  background: var(--primary);
}

.fill-m {
  background: var(--success);
}

.bar-num {
  width: 74px;
  text-align: right;
  font-size: 14px;
  color: var(--text-sub);
}

.overall {
  text-align: center;
  color: var(--text-sub);
  font-size: 15px;
  margin: 18px 0 12px;
}

.units {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.unit {
  padding: 0;
  overflow: hidden;
}

.unit-head {
  width: 100%;
  min-height: 60px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  text-align: left;
}

.unit-no {
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 700;
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 999px;
  flex: none;
}

.unit-title {
  flex: 1;
  font-size: 19px;
  font-weight: 700;
}

.unit-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-sub);
}

.arrow {
  display: inline-block;
  transition: transform 0.2s ease;
  font-size: 18px;
}

.arrow.open {
  transform: rotate(180deg);
}

.lessons {
  padding: 4px 18px 16px;
  border-top: 1px dashed var(--border);
}

.lesson {
  padding: 14px 0 6px;
}

.lesson-lock {
  opacity: 0.55;
}

.lesson-info {
  margin-bottom: 10px;
}

.lesson-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
}

.garden-name {
  margin-left: 8px;
  color: var(--text-sub);
  font-size: 15px;
}

.done {
  font-size: 15px;
}

.levels {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.level {
  min-width: 76px;
  min-height: var(--min-tap);
  padding: 6px 12px;
  border-radius: 14px;
  border: 2px solid var(--border);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fbfcff;
  font-weight: 600;
}

.lv-name {
  font-size: 15px;
}

.lv-open {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary-dark);
}

.lv-passed {
  border-color: #ffe39e;
  background: #fffaf0;
}

.lv-locked {
  color: var(--text-sub);
}

.lv-ico {
  font-size: 16px;
}

.unit-locked-tip {
  color: var(--text-sub);
  font-size: 14px;
  padding: 4px 0;
}
</style>
