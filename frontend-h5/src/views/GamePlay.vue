<template>
  <div class="play-page">
    <div v-if="phase === 'loading'" class="loading">
      <div class="spinner"></div>
      正在准备题目…
    </div>

    <template v-else-if="session">
      <!-- ============ 顶栏：退出 / 分段进度条 / 菜单 ============ -->
      <header class="play-head">
        <button class="head-icon" aria-label="退出本关" @click="quitOpen = true">✕</button>

        <div class="seg-progress" aria-label="答题进度">
          <span
            v-for="(qq, i) in session.questions"
            :key="`f-${qq.id}`"
            class="seg"
            :class="firstSegClass(i)"
          ></span>
          <span
            v-for="(qid, i) in reviewQueue"
            :key="`r-${qid}`"
            class="seg seg-review"
            :class="reviewSegClass(i)"
          ></span>
        </div>

        <div class="head-menu-wrap">
          <button
            class="head-icon"
            :class="{ on: menuOpen }"
            aria-label="更多操作"
            @click="menuOpen = !menuOpen"
          >⋯</button>
          <template v-if="menuOpen">
            <div class="menu-mask" @click="menuOpen = false"></div>
            <div class="mini-menu">
              <button :disabled="!canSkip" @click="skip">跳过本题（记答错）</button>
            </div>
          </template>
        </div>
      </header>

      <!-- ============ 错题回炉过渡屏 ============ -->
      <div v-if="stage === 'review-intro'" class="intro-wrap">
        <div class="intro-card card">
          <div class="intro-emoji">💪</div>
          <h2 class="intro-title">把刚才的错题再做一遍吧</h2>
          <p class="intro-sub">还有 {{ reviewQueue.length }} 题，全部做对就能完成本关</p>
          <button class="btn btn-primary btn-block intro-btn" @click="startReview">
            开始复习 →
          </button>
        </div>
      </div>

      <!-- ============ 单题区（一次一道） ============ -->
      <template v-else>
        <transition name="slide" mode="out-in">
          <div :key="`${stage}-${currentQid}`" class="q-scroll">
            <div class="q-card card">
              <div class="q-meta">
                <span class="q-type">{{ TYPE_LABEL[currentQ.type] }}</span>
                <span v-if="reviewing" class="q-review">复习题</span>
                <span v-if="currentQ.knowledgePoint" class="q-kp">
                  {{ currentQ.knowledgePoint }}
                </span>
              </div>
              <h2 v-if="currentQ.type !== 'HAND'" class="q-stem">{{ currentQ.stem }}</h2>

              <SingleChoice
                v-if="currentQ.type === 'SINGLE'"
                v-model="singleModel"
                :options="currentQ.options ?? []"
                :disabled="inputLocked"
                :reveal="!!feedback"
                :correct-index="correctIndex"
              />
              <JudgeChoice
                v-else-if="currentQ.type === 'JUDGE'"
                v-model="judgeModel"
                :disabled="inputLocked"
                :reveal="!!feedback"
                :correct-value="correctJudge"
              />
              <BlankInput
                v-else-if="currentQ.type === 'BLANK'"
                v-model="blankModel"
                :disabled="inputLocked"
                :state="blankState"
              />
              <OrderSort
                v-else-if="currentQ.type === 'ORDER'"
                v-model="orderModel"
                :options="currentQ.options ?? []"
                :disabled="inputLocked"
              />
              <HandwriteInput
                v-else-if="currentQ.type === 'HAND'"
                v-model="blankModel"
                :disabled="inputLocked"
                :state="blankState"
                :stem="currentQ.stem"
                :answer="currentQ.answer ?? null"
                @submit="doCheck"
              />
            </div>
          </div>
        </transition>
      </template>

      <!-- ============ 底部固定操作坞 ============ -->
      <div v-if="stage !== 'review-intro'" class="dock" :class="dockClass">
        <div class="dock-inner">
          <!-- 检查请求失败：可原样重试，不丢作答 -->
          <template v-if="checkError">
            <div class="dock-err">
              <span class="dock-err-ico">⚠️</span>
              <span>{{ checkErrMsg || '网络不给力，请再试一次' }}</span>
            </div>
            <button class="btn btn-danger btn-block dock-btn" :disabled="checking" @click="retryCheck">
              再试一次
            </button>
          </template>

          <!-- 检查反馈 -->
          <template v-else-if="feedback">
            <div class="fb-body">
              <div class="fb-head">
                <span class="fb-ico">{{ feedback.isCorrect ? '✓' : '✕' }}</span>
                <div class="fb-text">
                  <div class="fb-title">{{ feedback.isCorrect ? '答对啦！' : '答错了，别灰心' }}</div>
                  <div v-if="feedback.isCorrect" class="fb-sub">{{ encourage }}</div>
                </div>
              </div>
              <div v-if="!feedback.isCorrect" class="fb-answer">
                <span class="fb-answer-label">正确答案</span>
                <span class="fb-answer-text">
                  {{ formatAnswerValue(currentQ.type, feedback.correctAnswer, currentQ.options) }}
                </span>
              </div>
              <div v-if="!feedback.isCorrect && showAnalysis" class="fb-analysis">
                <span class="fb-analysis-label">💡 解析</span>
                <p>{{ feedback.analysis }}</p>
              </div>
            </div>
            <button class="btn fb-btn btn-block dock-btn" @click="onContinue">
              {{ continueText }}
            </button>
          </template>

          <!-- 待检查 -->
          <template v-else>
            <button
              class="btn btn-primary btn-block dock-btn"
              :disabled="!canCheck || checking"
              @click="doCheck"
            >
              {{ checking ? '检查中…' : '检查' }}
            </button>
          </template>
        </div>
      </div>

      <!-- 退出确认：未提交本就不写库，规则不变 -->
      <div v-if="quitOpen" class="mask" @click.self="quitOpen = false">
        <div class="modal">
          <h2 class="modal-title">现在退出？</h2>
          <p class="confirm-text">现在退出，本次作答不会保存哦。</p>
          <div class="modal-actions">
            <button class="btn btn-ghost" @click="quitOpen = false">继续答题</button>
            <button class="btn btn-danger" @click="backMap">退出</button>
          </div>
        </div>
      </div>

      <!-- 提交失败：可重试，退出则本局不保存 -->
      <div v-if="submitErrorOpen" class="mask">
        <div class="modal">
          <h2 class="modal-title">提交失败</h2>
          <p class="confirm-text">{{ submitErrMsg || '网络异常，请稍后再试' }}</p>
          <p class="confirm-sub">本局结果还没有保存，请重试提交。</p>
          <div class="modal-actions">
            <button class="btn btn-ghost" @click="backMap">退出（不保存）</button>
            <button class="btn btn-primary" :disabled="submitting" @click="doSubmit">
              {{ submitting ? '提交中…' : '再试一次' }}
            </button>
          </div>
        </div>
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
import HandwriteInput from '@/components/questions/HandwriteInput.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'
import type { AnswerCheck, AnswerItemReq, QuestionNode, SessionStart } from '@/types/api'
import {
  formatAnswerValue,
  isAnswered,
  serializeAnswer,
  TYPE_LABEL,
  type AnswerValue
} from '@/utils/answer'

const PRAISES = ['太棒了！', '真厉害！', '继续保持！', '你真聪明！', '答得又快又准！']

const route = useRoute()
const router = useRouter()
const store = useChildStore()

const levelId = Number(route.params.levelId)

const phase = ref<'loading' | 'ready'>('loading')
const session = ref<SessionStart | null>(null)

// first：首答阶段；review-intro：回炉过渡小屏；review：错题回炉
const stage = ref<'first' | 'review-intro' | 'review'>('first')
const firstPos = ref(0)
const reviewQueue = ref<number[]>([]) // 错题 questionId 队列（稳定业务 ID，不用索引）
const reviewPos = ref(0)

// workAnswer：当前工作作答（按 questionId）；first*：提交口径，永远是第一次作答
const workAnswer = ref<Record<number, AnswerValue>>({})
const firstAnswer = ref<Record<number, AnswerValue>>({})
const firstCorrect = ref<Record<number, boolean>>({})
const firstDuration = ref<Record<number, number>>({})

const feedback = ref<AnswerCheck | null>(null)
const encourage = ref('')
const checking = ref(false)
const checkError = ref(false)
const checkErrMsg = ref('')
const submitting = ref(false)
const submitErrorOpen = ref(false)
const submitErrMsg = ref('')
const quitOpen = ref(false)
const menuOpen = ref(false)

const qStartTs = ref(0)
const sessionStartTs = ref(0)

const questions = computed<QuestionNode[]>(() => session.value?.questions ?? [])
const qById = computed(() => {
  const m = new Map<number, QuestionNode>()
  questions.value.forEach((qq) => m.set(qq.id, qq))
  return m
})

const currentQid = computed<number>(() =>
  stage.value === 'first'
    ? questions.value[firstPos.value]?.id
    : reviewQueue.value[reviewPos.value]
)
const currentQ = computed<QuestionNode>(() => qById.value.get(currentQid.value) as QuestionNode)
const reviewing = computed(() => stage.value === 'review')
const showAnalysis = computed(
  () => !!session.value?.showAnalysisImmediately && !!feedback.value?.analysis
)

// 当前题工作答案，桥接 4 个题型组件
const qModel = computed<AnswerValue>({
  get: () => (currentQid.value ? workAnswer.value[currentQid.value] ?? null : null),
  set: (v) => {
    if (currentQid.value) workAnswer.value[currentQid.value] = v
  }
})
const singleModel = computed<number | null>({
  get: () => (typeof qModel.value === 'number' ? (qModel.value as number) : null),
  set: (v) => (qModel.value = v)
})
const judgeModel = computed<boolean | null>({
  get: () => (typeof qModel.value === 'boolean' ? (qModel.value as boolean) : null),
  set: (v) => (qModel.value = v)
})
const blankModel = computed<string>({
  get: () => (typeof qModel.value === 'string' ? (qModel.value as string) : ''),
  set: (v) => (qModel.value = v)
})
const orderModel = computed<number[]>({
  get: () => (Array.isArray(qModel.value) ? (qModel.value as number[]) : []),
  set: (v) => (qModel.value = v)
})

const canCheck = computed(() => isAnswered(qModel.value))
const canSkip = computed(
  () => stage.value === 'first' && !feedback.value && !checking.value && !checkError.value
)
const inputLocked = computed(() => !!feedback.value || checking.value || checkError.value)

const correctIndex = computed<number | null>(() =>
  feedback.value && currentQ.value.type === 'SINGLE'
    ? Number((feedback.value as AnswerCheck).correctAnswer)
    : null
)
const correctJudge = computed<boolean | null>(() =>
  feedback.value && currentQ.value.type === 'JUDGE'
    ? Boolean((feedback.value as AnswerCheck).correctAnswer)
    : null
)
const blankState = computed<'idle' | 'correct' | 'wrong'>(() => {
  if (!feedback.value) return 'idle'
  return feedback.value.isCorrect ? 'correct' : 'wrong'
})

const dockClass = computed(() => ({
  'dock-correct': !!feedback.value?.isCorrect,
  'dock-wrong': !!feedback.value && !feedback.value.isCorrect,
  'dock-error': checkError.value
}))

const continueText = computed(() => {
  if (submitting.value) return '结算中…'
  if (stage.value === 'review' && feedback.value && !feedback.value.isCorrect) return '再试一次'
  return '继续'
})

/* ---------- 分段进度条 ---------- */
function firstSegClass(i: number): string {
  if (stage.value === 'first') {
    if (i < firstPos.value) return 'seg-done'
    if (i === firstPos.value) return feedback.value ? 'seg-done' : 'seg-current'
    return ''
  }
  // 回炉阶段：首答段全部完成
  return 'seg-done'
}

function reviewSegClass(i: number): string {
  if (stage.value !== 'review') return ''
  if (i < reviewPos.value) return 'seg-done'
  if (i === reviewPos.value) return feedback.value ? 'seg-done' : 'seg-current'
  return ''
}

/* ---------- 检查 / 反馈 ---------- */
async function runCheck(value: AnswerValue) {
  if (checking.value || feedback.value || !session.value || !currentQid.value) return
  checking.value = true
  checkError.value = false
  menuOpen.value = false
  const qid = currentQid.value
  const startedAt = qStartTs.value
  try {
    const res = await childApi.check({
      levelId,
      questionId: qid,
      userAnswer: serializeAnswer(value)
    })
    // 首答口径只在第一次检查时落账；回炉检查不计入结算
    if (stage.value === 'first') {
      firstAnswer.value[qid] = value
      firstCorrect.value[qid] = res.isCorrect
      firstDuration.value[qid] = Date.now() - startedAt
    }
    feedback.value = res
    encourage.value = PRAISES[Math.floor(Math.random() * PRAISES.length)]
  } catch (e) {
    checkError.value = true
    checkErrMsg.value = (e as Error).message
  } finally {
    checking.value = false
  }
}

function doCheck() {
  if (canCheck.value) runCheck(qModel.value)
}

function skip() {
  if (!canSkip.value) return
  // 跳过：空答案记为答错（沿用旧语义），同样走反馈让孩子看到正确答案
  qModel.value = null
  runCheck(null)
}

function retryCheck() {
  if (!checkError.value) return
  checkError.value = false
  runCheck(qModel.value)
}

/* ---------- 推进 / 回炉 ---------- */
function enterFirst(pos: number) {
  stage.value = 'first'
  firstPos.value = pos
  feedback.value = null
  checkError.value = false
  menuOpen.value = false
  qStartTs.value = Date.now()
}

function enterReview(pos: number) {
  stage.value = 'review'
  reviewPos.value = pos
  const qid = reviewQueue.value[pos]
  if (qid != null) workAnswer.value[qid] = null // 回炉题重新作答
  feedback.value = null
  checkError.value = false
  menuOpen.value = false
  qStartTs.value = Date.now()
}

function startReview() {
  enterReview(0)
}

function onContinue() {
  if (!feedback.value || submitting.value) return

  if (stage.value === 'first') {
    if (firstPos.value < questions.value.length - 1) {
      enterFirst(firstPos.value + 1)
      return
    }
    // 首答结束：错与跳过均入回炉队列
    reviewQueue.value = questions.value
      .filter((qq) => !firstCorrect.value[qq.id])
      .map((qq) => qq.id)
    if (reviewQueue.value.length === 0) {
      doSubmit()
    } else {
      feedback.value = null
      stage.value = 'review-intro'
    }
    return
  }

  if (stage.value === 'review') {
    if (!feedback.value.isCorrect) {
      // 回炉答错：留在本题，清空重做，直到答对一次
      const qid = currentQid.value
      if (qid) workAnswer.value[qid] = null
      feedback.value = null
      qStartTs.value = Date.now()
    } else if (reviewPos.value < reviewQueue.value.length - 1) {
      enterReview(reviewPos.value + 1)
    } else {
      doSubmit()
    }
  }
}

/* ---------- 提交（只调一次；答案/耗时均取首答值） ---------- */
async function doSubmit() {
  if (!session.value || submitting.value) return
  submitting.value = true
  submitErrorOpen.value = false
  const totalMs = Date.now() - sessionStartTs.value
  const payloadAnswers: AnswerItemReq[] = session.value.questions.map((qq) => ({
    questionId: qq.id,
    userAnswer: serializeAnswer(firstAnswer.value[qq.id] ?? null),
    durationMs: Math.round(firstDuration.value[qq.id] || 0)
  }))
  try {
    const res = await childApi.submit({
      levelId,
      durationMs: totalMs,
      answers: payloadAnswers
    })
    store.setLastResult(res, session.value.levelName, totalMs)
    router.replace('/result')
  } catch (e) {
    submitErrMsg.value = (e as Error).message
    submitErrorOpen.value = true
  } finally {
    submitting.value = false
  }
}

function backMap() {
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
    store.setToday(s.today)
    sessionStartTs.value = Date.now()
    qStartTs.value = Date.now()
    phase.value = 'ready'
  } catch (e) {
    showToast((e as Error).message, 'error', 2600)
    window.setTimeout(() => router.replace('/map'), 600)
  }
})
</script>

<style scoped>
.play-page {
  max-width: 640px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 12px 18px 0;
}

/* ---------- 顶栏 ---------- */
.play-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
}

.head-icon {
  flex: none;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: var(--text-sub);
  font-size: 20px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.head-icon:active {
  background: var(--card-2);
}

.head-icon.on {
  background: var(--card-2);
  color: var(--text-main);
}

.head-menu-wrap {
  position: relative;
  flex: none;
}

.menu-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
}

.mini-menu {
  position: absolute;
  top: 44px;
  right: 0;
  z-index: 41;
  min-width: 184px;
  background: var(--card-2);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.45);
  overflow: hidden;
}

.mini-menu button {
  display: block;
  width: 100%;
  text-align: left;
  padding: 13px 16px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.mini-menu button:disabled {
  color: #6d7a83;
  cursor: default;
}

.mini-menu button:not(:disabled):active {
  background: var(--card);
}

/* ---------- 分段进度条 ---------- */
.seg-progress {
  flex: 1;
  display: flex;
  gap: 3px;
  min-width: 0;
}

.seg {
  flex: 1;
  height: 14px;
  border-radius: 999px;
  background: var(--border);
  transition: background 0.2s ease;
}

.seg-done {
  background: var(--primary);
}

.seg-current {
  background: var(--primary);
  opacity: 0.4;
  animation: seg-pulse 1.2s ease-in-out infinite;
}

/* 复习段：未到为暗橙，完成/当前为橙，与首答段视觉区分 */
.seg-review {
  background: #5a4326;
}

.seg-review.seg-done {
  background: var(--warning);
}

.seg-review.seg-current {
  background: var(--warning);
  opacity: 0.55;
  animation: seg-pulse 1.2s ease-in-out infinite;
}

@keyframes seg-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 0.75;
  }
}

/* ---------- 题目卡 ---------- */
.q-scroll {
  padding: 14px 0 230px;
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
  background: var(--info-soft);
  color: var(--info-border);
  font-size: 14px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 999px;
}

.q-review {
  background: var(--warning-soft);
  color: #ffb347;
  font-size: 14px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 999px;
}

.q-kp {
  background: var(--card-2);
  color: var(--text-sub);
  font-size: 14px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 999px;
}

.q-stem {
  font-size: 22px;
  line-height: 1.6;
  margin-bottom: 22px;
  font-weight: 800;
}

/* ---------- 回炉过渡屏 ---------- */
.intro-wrap {
  display: flex;
  justify-content: center;
  padding-top: 22vh;
}

.intro-card {
  width: 100%;
  max-width: 420px;
  text-align: center;
  padding: 34px 26px 28px;
}

.intro-emoji {
  font-size: 72px;
  line-height: 1;
  margin-bottom: 16px;
  animation: intro-bounce 1.6s ease-in-out infinite;
}

@keyframes intro-bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.intro-title {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 8px;
}

.intro-sub {
  color: var(--text-sub);
  font-size: 16px;
  margin-bottom: 22px;
}

/* ---------- 底部操作坞 ---------- */
.dock {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  background: var(--card);
  border-top: 2px solid var(--border);
  padding: 12px 18px calc(12px + env(safe-area-inset-bottom));
}

.dock-inner {
  max-width: 640px;
  margin: 0 auto;
}

.dock-btn {
  min-height: 52px;
  margin-top: 10px;
}

.dock-correct {
  background: var(--primary);
  border-top-color: var(--primary-dark);
}

.dock-wrong {
  background: var(--danger);
  border-top-color: #d93b3b;
}

.fb-body {
  max-height: 40vh;
  overflow-y: auto;
  color: #fff;
}

.fb-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fb-ico {
  flex: none;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #fff;
  font-size: 26px;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dock-correct .fb-ico {
  color: var(--primary-dark);
}

.dock-wrong .fb-ico {
  color: var(--danger);
}

.fb-title {
  font-size: 21px;
  font-weight: 800;
  color: #fff;
}

.fb-sub {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

.fb-answer {
  margin-top: 10px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  padding: 9px 14px;
  color: #fff;
}

.fb-answer-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  opacity: 0.85;
}

.fb-answer-text {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.5;
}

.fb-analysis {
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  padding: 9px 14px;
}

.fb-analysis-label {
  font-size: 13px;
  font-weight: 700;
  opacity: 0.85;
}

.fb-analysis p {
  margin: 3px 0 0;
  font-size: 15px;
  line-height: 1.65;
}

.fb-btn {
  background: #fff;
  box-shadow: 0 4px 0 rgba(0, 0, 0, 0.2);
}

.fb-btn:active {
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.2);
}

.dock-correct .fb-btn {
  color: var(--primary-dark);
}

.dock-wrong .fb-btn {
  color: var(--danger);
}

/* 检查失败提示条 */
.dock-err {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ffb347;
  font-size: 16px;
  font-weight: 700;
}

/* ---------- 弹层 ---------- */
.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-actions .btn {
  flex: 1;
}

.confirm-text {
  text-align: center;
  font-size: 18px;
  margin-bottom: 6px;
}

.confirm-sub {
  text-align: center;
  color: var(--text-sub);
  font-size: 15px;
  margin-bottom: 14px;
}

/* ---------- 切题动画 ---------- */
.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@media (prefers-reduced-motion: reduce) {
  .seg-current,
  .intro-emoji,
  .slide-enter-active,
  .slide-leave-active {
    animation: none;
    transition: none;
  }
}
</style>
