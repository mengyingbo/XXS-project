<template>
  <div class="page page-no-tab movie-page">
    <!-- 顶部导航 -->
    <header class="top-bar">
      <button class="back-btn" @click="router.push('/')">‹ 返回</button>
      <h1 class="page-title">🎬 电影片库</h1>
    </header>
    <p class="page-sub">四年级高分电影 Top50，边看边学大道理</p>

    <!-- 统计概览 -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-num">{{ movies.length }}</span>
        <span class="stat-label">总片数</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num stat-watched">{{ watchedCount }}</span>
        <span class="stat-label">已看</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num stat-unwatched">{{ unwatchedCount }}</span>
        <span class="stat-label">未看</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">{{ avgRating }}</span>
        <span class="stat-label">平均分</span>
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="filter-tabs">
      <button
        v-for="t in tabs"
        :key="t.value"
        class="ft-btn"
        :class="{ on: filter === t.value }"
        @click="filter = t.value"
      >{{ t.label }}</button>
    </div>

    <!-- 搜索框 -->
    <div class="search-box">
      <span class="sb-icon">🔍</span>
      <input
        v-model.trim="search"
        class="sb-input"
        type="text"
        placeholder="搜索电影名、类型、主题…"
      />
      <button v-if="search" class="sb-clear" @click="search = ''">✕</button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="empty">
      <span class="emoji">⏳</span>
      正在加载电影列表…
    </div>
    <!-- 加载失败 -->
    <div v-else-if="error" class="empty">
      <span class="emoji">😵</span>
      加载失败，请稍后重试
    </div>
    <!-- 电影网格 -->
    <div v-else-if="filtered.length" class="movie-grid">
      <button
        v-for="m in filtered"
        :key="m.no"
        class="movie-card"
        :class="{ watched: m.watched }"
        @click="openDetail(m)"
      >
        <div class="mc-cover-wrap">
          <img :src="m.cover" :alt="m.name" class="mc-cover" loading="lazy" />
          <span class="mc-rating">★ {{ m.rating }}</span>
          <span class="mc-status" :class="m.watched ? 'ms-watched' : 'ms-unwatched'">
            {{ m.watched ? '✓ 已看' : '未看' }}
          </span>
        </div>
        <div class="mc-info">
          <span class="mc-name">{{ m.name }}</span>
          <span class="mc-type">{{ m.type }} · {{ m.duration }}分钟</span>
        </div>
      </button>
    </div>
    <div v-else class="empty">
      <span class="emoji">🔍</span>
      没有找到匹配的电影
    </div>

    <!-- 电影详情弹窗 -->
    <div v-if="selected" class="mask" @click.self="closeDetail">
      <div class="modal detail-modal">
        <button class="close-x" @click="closeDetail">✕</button>
        <img :src="selected.cover" :alt="selected.name" class="dm-cover" />
        <h2 class="dm-title">{{ selected.name }}</h2>
        <div class="dm-tags">
          <span class="dm-tag dm-tag-type">{{ selected.type }}</span>
          <span class="dm-tag dm-tag-duration">⏱ {{ selected.duration }}分钟</span>
          <span class="dm-tag dm-tag-rating">★ {{ selected.rating }}</span>
          <span class="dm-tag" :class="selected.watched ? 'dm-tag-watched' : 'dm-tag-unwatched'">
            {{ selected.watched ? '✓ 已看' : '未看' }}
          </span>
        </div>
        <div class="dm-section">
          <span class="dm-label">推荐主题</span>
          <span class="dm-value">{{ selected.theme }}</span>
        </div>
        <div class="dm-section">
          <span class="dm-label">适龄看点</span>
          <span class="dm-value dm-note">{{ selected.note }}</span>
        </div>
        <div class="dm-actions">
          <button class="btn btn-ghost" @click="closeDetail">关闭</button>
          <button
            class="btn"
            :class="selected.watched ? 'btn-undo' : 'btn-primary'"
            @click="toggleWatch"
          >
            {{ selected.watched ? '标记为未看' : '✓ 标记为已看' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { movieApi } from '@/api/movie'
import type { Movie } from '@/types/api'

const router = useRouter()

// 观看状态用 localStorage 持久化（按数据库 id 存储）
const STORAGE_KEY = 'movie-watched-status-v2'

// 从后端 API 获取的电影列表
const rawMovies = ref<Movie[]>([])
const loading = ref(true)
const error = ref(false)

function loadStatus(): Record<number, boolean> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch {}
  return {}
}

function saveStatus(map: Record<number, boolean>) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(map))
  } catch {}
}

const statusMap = ref<Record<number, boolean>>(loadStatus())

// 合并 API 数据与 localStorage 状态
const movies = computed<Movie[]>(() =>
  rawMovies.value.map(m => ({
    ...m,
    watched: statusMap.value[m.id] ?? m.watched
  }))
)

// 加载电影列表
async function loadMovies() {
  loading.value = true
  error.value = false
  try {
    rawMovies.value = await movieApi.list()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadMovies)

const watchedCount = computed(() => movies.value.filter(m => m.watched).length)
const unwatchedCount = computed(() => movies.value.length - watchedCount.value)
const avgRating = computed(() => {
  const sum = movies.value.reduce((a, m) => a + m.rating, 0)
  return (sum / movies.value.length).toFixed(1)
})

// 筛选 & 搜索
type FilterType = 'all' | 'watched' | 'unwatched'
const tabs = [
  { label: '全部', value: 'all' as FilterType },
  { label: '已看', value: 'watched' as FilterType },
  { label: '未看', value: 'unwatched' as FilterType },
]
const filter = ref<FilterType>('all')
const search = ref('')

const filtered = computed(() => {
  let list = movies.value
  if (filter.value === 'watched') list = list.filter(m => m.watched)
  else if (filter.value === 'unwatched') list = list.filter(m => !m.watched)
  const s = search.value.toLowerCase()
  if (s) {
    list = list.filter(m =>
      m.name.toLowerCase().includes(s) ||
      m.type.toLowerCase().includes(s) ||
      m.theme.toLowerCase().includes(s) ||
      m.note.toLowerCase().includes(s)
    )
  }
  return list
})

// 详情弹窗
const selected = ref<Movie | null>(null)

function openDetail(m: Movie) {
  selected.value = m
}

function closeDetail() {
  selected.value = null
}

function toggleWatch() {
  if (!selected.value) return
  const id = selected.value.id
  const newStatus = !statusMap.value[id]
  statusMap.value = { ...statusMap.value, [id]: newStatus }
  saveStatus(statusMap.value)
  selected.value = { ...selected.value, watched: newStatus }
}
</script>

<style scoped>
.movie-page {
  max-width: 720px;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 10px;
}

.back-btn {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-sub);
  padding: 8px 4px;
  flex: none;
  transition: color 0.15s ease;
}

.back-btn:active {
  color: var(--primary);
}

.page-title {
  font-size: 24px;
  font-weight: 800;
}

.page-sub {
  color: var(--text-sub);
  font-size: 14px;
  margin-top: 2px;
  margin-bottom: 18px;
}

/* ---- 统计概览 ---- */
.stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 8px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.stat-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-main);
}

.stat-watched {
  color: var(--primary);
}

.stat-unwatched {
  color: var(--warning);
}

.stat-label {
  font-size: 13px;
  color: var(--text-sub);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border);
}

/* ---- 筛选标签 ---- */
.filter-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.ft-btn {
  flex: 1;
  min-height: 42px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  background: var(--card);
  border: 2px solid var(--border);
  color: var(--text-sub);
  transition: all 0.15s ease;
}

.ft-btn.on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary);
}

/* ---- 搜索框 ---- */
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--card);
  border: 2px solid var(--border);
  border-radius: 14px;
  padding: 0 14px;
  margin-bottom: 20px;
  min-height: 46px;
}

.sb-icon {
  font-size: 18px;
  flex: none;
  opacity: 0.6;
}

.sb-input {
  flex: 1;
  border: none;
  background: none;
  color: var(--text-main);
  font-size: 16px;
  outline: none;
  min-height: 42px;
}

.sb-input::placeholder {
  color: #78848d;
}

.sb-clear {
  font-size: 16px;
  color: var(--text-sub);
  padding: 4px 8px;
  flex: none;
}

/* ---- 电影网格 ---- */
.movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(155px, 1fr));
  gap: 16px;
}

.movie-card {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--border);
  transition: transform 0.1s ease;
  text-align: left;
}

.movie-card:active {
  transform: scale(0.96);
}

.movie-card.watched {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary);
}

.mc-cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  background: #1a2530;
}

.mc-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.mc-rating {
  position: absolute;
  top: 6px;
  left: 6px;
  background: rgba(0, 0, 0, 0.75);
  color: var(--gold);
  font-size: 12px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 8px;
}

.mc-status {
  position: absolute;
  bottom: 6px;
  right: 6px;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 8px;
}

.ms-watched {
  background: rgba(88, 204, 2, 0.9);
  color: #fff;
}

.ms-unwatched {
  background: rgba(255, 150, 0, 0.9);
  color: #fff;
}

.mc-info {
  padding: 10px 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mc-name {
  font-size: 15px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mc-type {
  font-size: 12px;
  color: var(--text-sub);
}

/* ---- 详情弹窗 ---- */
.detail-modal {
  max-width: 380px;
  text-align: center;
  position: relative;
  padding-top: 20px;
}

.close-x {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 20px;
  color: var(--text-sub);
  z-index: 1;
  padding: 4px 8px;
}

.dm-cover {
  width: 160px;
  height: 240px;
  object-fit: cover;
  border-radius: 14px;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.dm-title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 14px;
}

.dm-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-bottom: 18px;
}

.dm-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.dm-tag-type {
  background: var(--info-soft);
  color: #84d8ff;
}

.dm-tag-duration {
  background: var(--card-2);
  color: var(--text-sub);
}

.dm-tag-rating {
  background: rgba(255, 200, 0, 0.15);
  color: var(--gold);
}

.dm-tag-watched {
  background: var(--primary-soft);
  color: var(--primary);
}

.dm-tag-unwatched {
  background: var(--warning-soft);
  color: #ffb347;
}

.dm-section {
  display: flex;
  gap: 10px;
  text-align: left;
  margin-bottom: 14px;
  align-items: flex-start;
}

.dm-label {
  flex: none;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-sub);
  min-width: 70px;
}

.dm-value {
  font-size: 15px;
  color: var(--text-main);
  flex: 1;
}

.dm-note {
  line-height: 1.6;
}

.dm-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.dm-actions .btn {
  flex: 1;
}

.btn-undo {
  background: var(--card-2);
  color: var(--text-main);
  box-shadow: 0 4px 0 var(--border);
}

.btn-undo:active {
  box-shadow: 0 1px 0 var(--border);
}

/* ---- 空状态 ---- */
.empty {
  text-align: center;
  color: var(--text-sub);
  padding: 60px 16px;
  font-size: 16px;
}

.empty .emoji {
  font-size: 52px;
  display: block;
  margin-bottom: 12px;
}

/* ---- 平板宽屏 ---- */
@media (min-width: 760px) {
  .movie-grid {
    grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
    gap: 20px;
  }
}
</style>
