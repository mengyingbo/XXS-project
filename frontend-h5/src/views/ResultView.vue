<template>
  <div class="page page-no-tab result-page" v-if="r">
    <header class="r-head">
      <div class="r-level">{{ store.lastLevelName }}</div>
      <div class="r-title">{{ r.passed ? '🎉 通关啦！' : '💪 再挑战一次吧' }}</div>
    </header>

    <!-- 星级 -->
    <div class="stars-card card">
      <div class="big-stars">
        <span
          v-for="i in 3"
          :key="i"
          class="big-star"
          :class="{ on: i <= r.stars }"
          :style="{ animationDelay: 0.25 * i + 0.1 + 's' }"
        >★</span>
      </div>
      <div class="accuracy">正确率 {{ r.accuracy }}%</div>
      <div class="count-line">
        答对 <b>{{ r.correctCount }}</b> / {{ r.totalCount }} 题
      </div>
      <div v-if="r.firstPass" class="r-tag tag-pass">✨ 首次通关</div>
    </div>

    <!-- 积分 -->
    <div class="points-card card">
      <div class="gain">
        <span class="gain-num">+{{ r.pointsGained }}</span>
        <span class="gain-label">本关获得积分</span>
      </div>
      <div class="breakdown">
        <div v-if="r.pointsBreakdown.correct > 0" class="bd-row">
          <span>答对 {{ r.correctCount }} 题</span>
          <span>+{{ r.pointsBreakdown.correct }}</span>
        </div>
        <div v-if="r.pointsBreakdown.combo > 0" class="bd-row">
          <span>🔥 连对加成</span>
          <span>+{{ r.pointsBreakdown.combo }}</span>
        </div>
        <div v-if="r.pointsBreakdown.passBonus > 0" class="bd-row">
          <span>首次通关奖励</span>
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
      <div class="balance">我的积分余额：<b>{{ r.points }}</b></div>
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
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChildStore } from '@/stores/child'
import { formatAnswerValue } from '@/utils/answer'

const router = useRouter()
const store = useChildStore()

onMounted(() => {
  if (!store.lastResult) {
    router.replace('/map')
  }
})

const r = computed(() => store.lastResult)
const wrongList = computed(() =>
  r.value ? r.value.details.filter((d) => !d.isCorrect) : []
)

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
  max-width: 640px;
  padding-top: 20px;
}

.r-head {
  text-align: center;
  margin-bottom: 16px;
}

.r-level {
  color: var(--text-sub);
  font-size: 16px;
}

.r-title {
  font-size: 28px;
  font-weight: 800;
  margin-top: 4px;
}

.stars-card {
  text-align: center;
  padding: 24px 20px 20px;
}

.big-stars {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-bottom: 12px;
}

.big-star {
  font-size: 60px;
  color: #dfe4f0;
  transform: scale(0.4);
  opacity: 0;
}

.big-star.on {
  color: var(--star);
  text-shadow: 0 4px 12px rgba(255, 197, 49, 0.5);
  animation: star-pop 0.45s cubic-bezier(0.2, 1.4, 0.5, 1) forwards;
}

@keyframes star-pop {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  70% {
    transform: scale(1.15);
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
  color: var(--success);
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
  color: #c97c00;
}

.points-card {
  margin-top: 14px;
  padding: 20px;
}

.gain {
  display: flex;
  align-items: baseline;
  gap: 10px;
  justify-content: center;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--border);
}

.gain-num {
  font-size: 40px;
  font-weight: 800;
  color: #c97c00;
}

.gain-label {
  color: var(--text-sub);
  font-size: 16px;
}

.breakdown {
  padding: 12px 0 4px;
}

.bd-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 4px;
  font-size: 16px;
}

.bd-row span:last-child {
  font-weight: 700;
  color: #c97c00;
}

.empty-gain span {
  color: var(--text-sub);
}

.balance {
  text-align: right;
  color: var(--text-sub);
  font-size: 15px;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
}

.balance b {
  color: var(--text-main);
  font-size: 18px;
}

.wrong-card {
  margin-top: 14px;
}

.block-title {
  font-size: 19px;
  margin-bottom: 12px;
}

.wrong-item {
  border-top: 1px dashed var(--border);
  padding: 14px 0;
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
  color: var(--danger);
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
  color: #d84040;
  flex: 1;
}

.wr-right {
  color: #1d9c5a;
  flex: 1;
}

.wr-analysis {
  margin-top: 6px;
  background: #f7f9ff;
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
  color: #1d9c5a;
  padding: 28px 20px;
}

.r-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.r-actions .btn {
  flex: 1;
}
</style>
