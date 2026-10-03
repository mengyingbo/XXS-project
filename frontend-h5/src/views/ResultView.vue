<template>
  <div class="result-page" v-if="r">
    <!-- 通关彩屑（纯 CSS，无外部资源） -->
    <div v-if="r.passed" class="confetti" aria-hidden="true">
      <span
        v-for="(c, i) in confettiPieces"
        :key="i"
        :style="{
          left: c.left + '%',
          background: c.color,
          animationDelay: c.delay + 's',
          animationDuration: c.duration + 's'
        }"
      ></span>
    </div>

    <header class="r-head">
      <div class="r-level">{{ store.lastLevelName }}</div>
      <div class="r-title">{{ r.passed ? '🎉 通关啦！' : '💪 再接再厉' }}</div>
    </header>

    <!-- 星级 -->
    <div class="stars-card card">
      <div class="big-stars">
        <span
          v-for="i in 3"
          :key="i"
          class="big-star"
          :class="{ on: i <= r.stars }"
          :style="{ animationDelay: 0.25 * i + 0.15 + 's' }"
        >★</span>
      </div>
      <div class="accuracy">正确率 {{ r.accuracy }}%</div>
      <div class="count-line">
        答对 <b>{{ r.correctCount }}</b> / {{ r.totalCount }} 题
      </div>
      <div v-if="r.firstPass" class="r-tag tag-pass">✨ 首次通关</div>
    </div>

    <!-- 多邻国式统计卡 -->
    <div class="stat-card card">
      <div class="stat-row">
        <span class="sr-ico">💎</span>
        <span class="sr-label">获得积分</span>
        <span class="sr-value sr-points">+{{ r.pointsGained }}</span>
      </div>

      <!-- 积分明细（可折叠） -->
      <button class="bd-toggle" @click="showBreakdown = !showBreakdown">
        <span>积分明细</span>
        <span class="bd-arrow" :class="{ up: showBreakdown }">⌄</span>
      </button>
      <div v-show="showBreakdown" class="breakdown">
        <div class="bd-row">
          <span>答对 {{ r.correctCount }} 题</span>
          <span>+{{ r.pointsBreakdown.correct }}</span>
        </div>
        <div v-if="r.pointsBreakdown.combo > 0" class="bd-row">
          <span>🔥 连对加成</span>
          <span>+{{ r.pointsBreakdown.combo }}</span>
        </div>
        <div v-if="r.pointsBreakdown.passBonus > 0" class="bd-row">
          <span>✨ 首次通关奖励</span>
          <span>+{{ r.pointsBreakdown.passBonus }}</span>
        </div>
        <div v-if="r.pointsBreakdown.threeStarBonus > 0" class="bd-row">
          <span>🌟 三星通关奖励</span>
          <span>+{{ r.pointsBreakdown.threeStarBonus }}</span>
        </div>
        <div v-if="r.pointsGained === 0" class="bd-row empty-gain">
          <span>本关没有得分，答对题目就能拿积分哦</span>
        </div>
      </div>

      <div class="stat-row">
        <span class="sr-ico">🎯</span>
        <span class="sr-label">正确率</span>
        <span class="sr-value">{{ r.accuracy }}%</span>
      </div>
      <div class="stat-row">
        <span class="sr-ico">⏱️</span>
        <span class="sr-label">用时</span>
        <span class="sr-value">{{ durationText }}</span>
      </div>

      <div class="balance">💎 积分余额：<b>{{ r.points }}</b></div>
    </div>

    <!-- 错题回顾 -->
    <div v-if="wrongList.length > 0" class="wrong-card card">
      <h3 class="block-title">📝 错题回顾（{{ wrongList.length }} 题）</h3>
      <div v-for="(d, i) in wrongList" :key="d.questionId" class="wrong-item">
        <div class="wr-stem">
          <span class="wr-idx">{{ i + 1 }}</span>
          <span>{{ d.stem }}</span>
        </div>
        <div class="wr-line">
          <span class="wr-label">你的答案</span>
          <span class="wr-wrong">{{ formatAnswerValue(d.type, d.userAnswer, d.options) }}</span>
        </div>
        <div class="wr-line">
          <span class="wr-label">正确答案</span>
          <span class="wr-right">{{ formatAnswerValue(d.type, d.correctAnswer, d.options) }}</span>
        </div>
        <div v-if="d.analysis" class="wr-analysis">💡 {{ d.analysis }}</div>
      </div>
    </div>
    <div v-else class="all-right card">🏆 全部答对，太厉害啦！</div>

    <!-- 操作 -->
    <div class="r-actions">
      <button class="btn btn-ghost" @click="retry">再来一次</button>
      <button class="btn btn-ghost" @click="backMap">返回地图</button>
      <button v-if="r.passed && r.nextLevelId != null" class="btn btn-primary" @click="next">
        下一关 →
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useChildStore } from '@/stores/child'
import { formatAnswerValue } from '@/utils/answer'
import { sfxComplete, sfxEncourage } from '@/utils/sfx'

const router = useRouter()
const store = useChildStore()

onMounted(() => {
  const res = store.lastResult
  if (!res) {
    router.replace('/map')
    return
  }
  window.scrollTo(0, 0)
  // 结算音效：等首屏渲染出来再播，通关琶音 / 未通关温柔鼓励
  window.setTimeout(() => {
    if (store.lastResult !== res) return
    if (res.passed) {
      sfxComplete()
    } else {
      sfxEncourage()
    }
  }, 150)
})

const r = computed(() => store.lastResult)
const wrongList = computed(() =>
  r.value ? r.value.details.filter((d) => !d.isCorrect) : []
)

const showBreakdown = ref(true)

const durationText = computed(() => {
  const totalSec = Math.max(0, Math.round(store.lastDurationMs / 1000))
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
})

// 彩屑：固定一组伪随机样式，避免每次渲染跳动
const CONFETTI_COLORS = ['#ffc800', '#58cc02', '#1cb0f6', '#ff4b4b', '#ff9600', '#ce82ff']
const confettiPieces = Array.from({ length: 22 }, (_, i) => {
  const v = (i * 37 + 11) % 100
  return {
    left: (v * 0.9 + 2) % 100,
    color: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
    delay: ((i * 13) % 20) / 10,
    duration: 2.4 + ((i * 7) % 14) / 10
  }
})

function retry() {
  if (r.value) router.replace(`/play/${r.value.levelId}`)
}

function next() {
  if (r.value?.nextLevelId != null) router.replace(`/play/${r.value.nextLevelId}`)
}

function backMap() {
  router.replace('/map')
}
</script>

<style scoped>
.result-page {
  position: relative;
  max-width: 640px;
  margin: 0 auto;
  padding: 28px 18px 40px;
  min-height: 100vh;
  overflow: hidden;
}

/* ---------- 彩屑 ---------- */
.confetti {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 70vh;
  pointer-events: none;
  overflow: hidden;
}

.confetti span {
  position: absolute;
  top: -20px;
  width: 10px;
  height: 14px;
  border-radius: 3px;
  opacity: 0.9;
  animation-name: confetti-fall;
  animation-timing-function: ease-in;
  animation-iteration-count: infinite;
}

@keyframes confetti-fall {
  0% {
    transform: translateY(-20px) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(72vh) rotate(540deg);
    opacity: 0.2;
  }
}

/* ---------- 标题 ---------- */
.r-head {
  text-align: center;
  margin-bottom: 16px;
}

.r-level {
  color: var(--text-sub);
  font-size: 16px;
}

.r-title {
  font-size: 30px;
  font-weight: 800;
  margin-top: 4px;
}

/* ---------- 星级 ---------- */
.stars-card {
  text-align: center;
  padding: 26px 20px 22px;
}

.big-stars {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 14px;
}

.big-star {
  font-size: 64px;
  line-height: 1;
  color: #46545e;
  transform: scale(0.4);
  opacity: 0;
}

.big-star.on {
  color: var(--star);
  text-shadow: 0 4px 12px rgba(255, 200, 0, 0.5);
  animation: star-pop 0.45s cubic-bezier(0.2, 1.4, 0.5, 1) forwards;
}

@keyframes star-pop {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  70% {
    transform: scale(1.18);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.accuracy {
  font-size: 22px;
  font-weight: 800;
}

.count-line {
  color: var(--text-sub);
  margin-top: 4px;
  font-size: 16px;
}

.count-line b {
  color: #7be338;
}

.r-tag {
  display: inline-block;
  margin-top: 10px;
  font-size: 14px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 999px;
}

.tag-pass {
  background: var(--warning-soft);
  color: var(--gold);
}

/* ---------- 统计卡 ---------- */
.stat-card {
  margin-top: 14px;
  padding: 6px 20px 16px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}

.sr-ico {
  flex: none;
  font-size: 22px;
  width: 30px;
  text-align: center;
}

.sr-label {
  flex: 1;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-main);
}

.sr-value {
  font-size: 19px;
  font-weight: 800;
}

.sr-points {
  color: #7be338;
}

.bd-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  color: var(--info-border);
  font-size: 15px;
  font-weight: 700;
  min-height: 48px;
}

.bd-arrow {
  display: inline-block;
  font-size: 18px;
  transition: transform 0.18s ease;
}

.bd-arrow.up {
  transform: rotate(180deg);
}

.breakdown {
  padding: 0 0 10px 42px;
}

.bd-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 0;
  font-size: 15px;
  color: var(--text-sub);
}

.bd-row span:last-child {
  font-weight: 700;
  color: #7be338;
  flex: none;
}

.empty-gain span {
  color: var(--text-sub);
}

.balance {
  text-align: right;
  color: var(--text-sub);
  font-size: 15px;
  padding-top: 14px;
}

.balance b {
  color: var(--text-main);
  font-size: 18px;
}

/* ---------- 错题回顾 ---------- */
.wrong-card {
  margin-top: 14px;
}

.block-title {
  font-size: 19px;
  margin-bottom: 12px;
}

.wrong-item {
  border-top: 1px solid var(--border);
  padding: 14px 0;
}

.wrong-item:first-of-type {
  border-top: none;
}

.wr-stem {
  display: flex;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.6;
  margin-bottom: 8px;
}

.wr-idx {
  flex: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--danger-soft);
  color: #ff7a7a;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 3px;
}

.wr-line {
  display: flex;
  gap: 10px;
  font-size: 15px;
  padding: 3px 0;
  line-height: 1.55;
}

.wr-label {
  flex: none;
  width: 64px;
  color: var(--text-sub);
}

.wr-wrong {
  color: #ff7a7a;
  flex: 1;
}

.wr-right {
  color: #7be338;
  flex: 1;
}

.wr-analysis {
  margin-top: 6px;
  background: var(--card-2);
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 15px;
  line-height: 1.65;
  color: var(--text-main);
}

.all-right {
  margin-top: 14px;
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: #7be338;
  padding: 28px 20px;
}

/* ---------- 操作按钮 ---------- */
.r-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.r-actions .btn {
  flex: 1;
}

@media (prefers-reduced-motion: reduce) {
  .confetti span,
  .big-star.on {
    animation: none;
  }

  .big-star.on {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
