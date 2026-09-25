<template>
  <div class="page page-no-tab play-page">
    <div v-if="phase === 'loading'" class="loading">
      <div class="spinner"></div>
      正在准备题目…
    </div>

    <!-- ============ 答题阶段 ============ -->
    <template v-else-if="phase === 'play' && session">
      <header class="play-head">
        <button class="quit" @click="quit">← 退出</button>
        <div class="head-center">
          <div class="lv-name">{{ session.levelName }}</div>
          <div class="lesson-name">{{ session.lessonTitle }}</div>
        </div>
        <div class="head-right">
          第 {{ currentIdx + 1 }}/{{ session.questionCount }} 题
        </div>
      </header>

      <div class="progress">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>

      <transition name="slide" mode="out-in">
        <div :key="currentIdx" class="q-card card">
          <div class="q-meta">
            <span class="q-type">{{ TYPE_LABEL[q.type] }}</span>
            <span v-if="q.knowledgePoint" class="q-kp">{{ q.knowledgePoint }}</span>
          </div>
          <h2 class="q-stem">{{ q.stem }}</h2>

          <SingleChoice
            v-if="q.type === 'SINGLE'"
            v-model="singleModel"
            :options="q.options ?? []"
          />
          <JudgeChoice v-else-if="q.type === 'JUDGE'" v-model="judgeModel" />
          <BlankInput v-else-if="q.type === 'BLANK'" v-model="blankModel" />
          <OrderSort
            v-else-if="q.type === 'ORDER'"
            v-model="orderModel"
            :options="q.options ?? []"
          />
        </div>
      </transition>

      <div class="play-actions">
        <button class="btn btn-ghost" @click="skip">
          跳过{{ currentIdx < lastIdx ? ' →' : '' }}
        </button>
        <button v-if="currentIdx > 0" class="btn btn-ghost" @click="prev">上一题</button>
        <button v-if="currentIdx < lastIdx" class="btn btn-primary" @click="next">
          下一题 →
        </button>
        <button v-else class="btn btn-primary" :disabled="submitting" @click="trySubmit">
          {{ submitting ? '判题中…' : '提交关卡' }}
        </button>
      </div>

      <!-- 提交确认 -->
      <div v-if="confirmOpen" class="mask" @click.self="confirmOpen = false">
        <div class="modal">
          <h2 class="modal-title">提交本关？</h2>
          <p class="confirm-text">
            已答 <b>{{ answeredCount }}</b> 题，未答 <b class="warn">{{ unansweredCount }}</b> 题。
          </p>
          <p v-if="unansweredCount > 0" class="confirm-sub">没作答的题目会算作答错哦。</p>
          <div class="modal-actions">
            <button class="btn btn-ghost" @click="confirmOpen = false">再想想</button>
            <button class="btn btn-primary" :disabled="submitting" @click="doSubmit">
              确认提交
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 提交后逐题反馈（F-H5-05，受 showAnalysisImmediately 控制） ============ -->
    <template v-else-if="phase === 'review' && result">
      <header class="play-head">
        <button class="quit" @click="goResult">跳过反馈 →</button>
        <div class="head-center">
          <div class="lv-name">答案反馈</div>
        </div>
        <div class="head-right">
          {{ reviewIdx + 1 }}/{{ result.totalCount }}
        </div>
      </header>

      <div class="progress">
        <div class="progress-fill" :style="{ width: ((reviewIdx + 1) / result.totalCount) * 100 + '%' }"></div>
      </div>

      <transition name="slide" mode="out-in">
        <div :key="reviewIdx" class="rv-card card" :class="detail.isCorrect ? 'is-right' : 'is-wrong'">
          <div class="rv-verdict">
            <span class="rv-big">{{ detail.isCorrect ? '🎉 答对啦' : '💪 再想想' }}</span>
            <span class="rv-type">{{ TYPE_LABEL[detail.type] }}</span>
          </div>
          <h2 class="q-stem">{{ detail.stem }}</h2>

          <div class="rv-row">
            <span class="rv-label">你的答案</span>
            <span :class="detail.isCorrect ? 'rv-right' : 'rv-wrong'">
              {{ formatAnswerValue(detail.type, detail.userAnswer, detail.options) }}
            </span>
          </div>
          <div v-if="!detail.isCorrect" class="rv-row">
            <span class="rv-label">正确答案</span>
            <span class="rv-right">
              {{ formatAnswerValue(detail.type, detail.correctAnswer, detail.options) }}
            </span>
          </div>
          <div v-if="detail.analysis" class="rv-analysis">
            <span class="rv-label">💡 解析</span>
            <p>{{ detail.analysis }}</p>
          </div>
        </div>
      </transition>

      <div class="play-actions">
        <button v-if="reviewIdx > 0" class="btn btn-ghost" @click="reviewIdx--">上一题</button>
        <button v-if="reviewIdx < lastReviewIdx" class="btn btn-primary" @click="reviewIdx++">
          下一题 →
        </button>
        <button v-else class="btn btn-primary" @click="goResult">查看结算 🏆</button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SingleChoice from '@/components/questions/SingleChoice.vue'
import JudgeChoice from '@/components/questions/JudgeChoice.vue'
import BlankInput from '@/components/questions/BlankInput.vue'
import OrderSort from '@/components/questions/OrderSort.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'
import type { AnswerItemReq, SessionStart, SessionSubmit } from '@/types/api'
import {
  formatAnswerValue,
  isAnswered,
  serializeAnswer,
  TYPE_LABEL,
  type AnswerValue
} from '@/utils/answer'

const route = useRoute()
const router = useRouter()
const store = useChildStore()

const levelId = Number(route.params.levelId)

const phase = ref<'loading' | 'play' | 'review'>('loading')
const session = ref<SessionStart | null>(null)
const result = ref<SessionSubmit | null>(null)

const answers = ref<AnswerValue[]>([])
const durations = ref<number[]>([])
const currentIdx = ref(0)
const reviewIdx = ref(0)
const questionStartTs = ref(0)
const sessionStartTs = ref(0)

const submitting = ref(false)
const confirmOpen = ref(false)

const q = computed(() => session.value!.questions[currentIdx.value])
const lastIdx = computed(() => (session.value ? session.value.questionCount - 1 : 0))
const lastReviewIdx = computed(() => (result.value ? result.value.totalCount - 1 : 0))
const detail = computed(() => result.value!.details[reviewIdx.value])

const progressPercent = computed(() =>
  session.value ? ((currentIdx.value + 1) / session.value.questionCount) * 100 : 0
)

const answeredCount = computed(() => answers.value.filter(isAnswered).length)
const unansweredCount = computed(() => (session.value ? session.value.questionCount - answeredCount.value : 0))

// 各题型组件用计算属性桥接到 answers[currentIdx]
const singleModel = computed<number | null>({
  get: () => (typeof answers.value[currentIdx.value] === 'number'
    ? (answers.value[currentIdx.value] as number)
    : null),
  set: (v) => (answers.value[currentIdx.value] = v)
})
const judgeModel = computed<boolean | null>({
  get: () => (typeof answers.value[currentIdx.value] === 'boolean'
    ? (answers.value[currentIdx.value] as boolean)
    : null),
  set: (v) => (answers.value[currentIdx.value] = v)
})
const blankModel = computed<string>({
  get: () => (typeof answers.value[currentIdx.value] === 'string'
    ? (answers.value[currentIdx.value] as string)
    : ''),
  set: (v) => (answers.value[currentIdx.value] = v)
})
const orderModel = computed<number[]>({
  get: () => (Array.isArray(answers.value[currentIdx.value])
    ? (answers.value[currentIdx.value] as number[])
    : []),
  set: (v) => (answers.value[currentIdx.value] = v)
})

function stampDuration() {
  if (questionStartTs.value) {
    durations.value[currentIdx.value] += Date.now() - questionStartTs.value
  }
}

function next() {
  stampDuration()
  if (currentIdx.value < lastIdx.value) {
    currentIdx.value += 1
    questionStartTs.value = Date.now()
  }
}

function prev() {
  stampDuration()
  if (currentIdx.value > 0) {
    currentIdx.value -= 1
    questionStartTs.value = Date.now()
  }
}

function skip() {
  // 跳过：清空当前作答并记为错
  answers.value[currentIdx.value] = null
  if (currentIdx.value < lastIdx.value) {
    next()
  } else {
    trySubmit()
  }
}

function trySubmit() {
  stampDuration()
  confirmOpen.value = true
}

async function doSubmit() {
  if (!session.value || submitting.value) return
  submitting.value = true
  const payloadAnswers: AnswerItemReq[] = session.value.questions.map((qq, i) => ({
    questionId: qq.id,
    userAnswer: serializeAnswer(answers.value[i]),
    durationMs: Math.round(durations.value[i] || 0)
  }))
  try {
    const res = await childApi.submit({
      levelId,
      durationMs: Date.now() - sessionStartTs.value,
      answers: payloadAnswers
    })
    result.value = res
    store.setLastResult(res, session.value.levelName)
    confirmOpen.value = false
    if (session.value.showAnalysisImmediately) {
      reviewIdx.value = 0
      phase.value = 'review'
    } else {
      router.replace('/result')
    }
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    submitting.value = false
  }
}

function goResult() {
  router.replace('/result')
}

function quit() {
  router.replace('/map')
}

onMounted(async () => {
  if (!levelId) {
    showToast('关卡不存在', 'error')
    router.replace('/map')
    return
  }
  try {
    const s = await childApi.start(levelId)
    session.value = s
    answers.value = new Array(s.questionCount).fill(null)
    durations.value = new Array(s.questionCount).fill(0)
    store.setToday(s.today)
    sessionStartTs.value = Date.now()
    questionStartTs.value = Date.now()
    phase.value = 'play'
  } catch (e) {
    showToast((e as Error).message, 'error', 2600)
    window.setTimeout(() => router.replace('/map'), 600)
  }
})

</script>

<style scoped>
.play-page {
  max-width: 760px;
  padding-top: 12px;
}

.play-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
}

.quit {
  min-height: var(--min-tap);
  padding: 0 10px;
  color: var(--text-sub);
  font-weight: 600;
  font-size: 15px;
  flex: none;
}

.head-center {
  flex: 1;
  text-align: center;
  min-width: 0;
}

.lv-name {
  font-size: 19px;
  font-weight: 800;
}

.lesson-name {
  font-size: 14px;
  color: var(--text-sub);
}

.head-right {
  flex: none;
  color: var(--text-sub);
  font-size: 15px;
  font-weight: 600;
  min-width: 72px;
  text-align: right;
}

.progress {
  height: 10px;
  background: #e7ebf6;
  border-radius: 999px;
  overflow: hidden;
  margin: 8px 0 16px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6f95ff, #4f7cff);
  border-radius: 999px;
  transition: width 0.25s ease;
}

.q-card {
  padding: 22px 20px;
}

.q-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.q-type {
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 14px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 999px;
}

.q-kp {
  background: #f1edff;
  color: #6b52d0;
  font-size: 14px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 999px;
}

.q-stem {
  font-size: 21px;
  line-height: 1.6;
  margin-bottom: 22px;
  font-weight: 700;
}

.play-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.play-actions .btn {
  flex: 1;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

.modal-actions .btn {
  flex: 1;
}

.confirm-text {
  text-align: center;
  font-size: 18px;
  margin-bottom: 6px;
}

.confirm-text .warn {
  color: var(--danger);
}

.confirm-sub {
  text-align: center;
  color: var(--text-sub);
  font-size: 15px;
  margin-bottom: 12px;
}

/* 反馈卡 */
.rv-card {
  padding: 22px 20px;
  border-width: 3px;
}

.rv-card.is-right {
  border: 3px solid #bfe8d2;
}

.rv-card.is-wrong {
  border: 3px solid #f6c9c9;
}

.rv-verdict {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.rv-big {
  font-size: 22px;
  font-weight: 800;
}

.is-right .rv-big {
  color: #1d9c5a;
}

.is-wrong .rv-big {
  color: #d84040;
}

.rv-type {
  background: #eef1f9;
  color: var(--text-sub);
  font-size: 13px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 999px;
}

.rv-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 10px 0;
  border-top: 1px dashed var(--border);
  font-size: 17px;
}

.rv-label {
  flex: none;
  width: 76px;
  color: var(--text-sub);
  font-size: 15px;
  font-weight: 600;
  padding-top: 1px;
}

.rv-right {
  color: #1d9c5a;
  font-weight: 600;
  flex: 1;
}

.rv-wrong {
  color: #d84040;
  font-weight: 600;
  flex: 1;
}

.rv-analysis {
  border-top: 1px dashed var(--border);
  padding-top: 10px;
}

.rv-analysis p {
  margin: 6px 0 0;
  color: var(--text-main);
  font-size: 16px;
  line-height: 1.7;
}

/* 切题动画 */
.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(16px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}
</style>
