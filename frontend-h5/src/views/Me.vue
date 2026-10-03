<template>
  <div class="page">
    <TabBar current="me" />

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      加载中…
    </div>

    <template v-else-if="data">
      <!-- 个人头部 -->
      <header class="me-head card">
        <AvatarFace :avatar="data.child.avatar" size="lg" />
        <div class="me-who">
          <div class="me-nick">{{ data.child.nickname }}</div>
          <div class="me-points-row">
            <span class="mp-chip">积分 <b>{{ stats.points }}</b></span>
            <span class="mp-chip">累计获得 <b>{{ stats.totalEarned }}</b></span>
          </div>
        </div>
        <button class="logout" @click="logout">退出</button>
      </header>

      <!-- 答题音效开关（家长可静音） -->
      <div class="sfx-row card">
        <span class="sfx-txt"><span class="sfx-ico">🔔</span>答题音效</span>
        <button
          class="sfx-switch"
          role="switch"
          :aria-checked="sfxOn"
          :class="{ on: sfxOn }"
          @click="toggleSfx"
        >
          <span class="knob"></span>
        </button>
      </div>

      <!-- 今日已学（F-H5-10） -->
      <div class="today card" :class="{ exhausted: store.today?.limitReached }">
        <div class="today-title">
          {{ store.today?.limitReached ? '🌙 今天的学习任务完成啦' : '📅 今天学到这儿' }}
        </div>
        <div class="today-grid">
          <div class="today-cell">
            <span class="tc-num">{{ store.today?.answeredToday ?? 0 }}</span>
            <span class="tc-label">已答题 / {{ store.today?.questionLimit ?? '-' }}</span>
          </div>
          <div class="today-cell">
            <span class="tc-num">{{ store.today?.usedMinutesToday ?? 0 }}</span>
            <span class="tc-label">学习分钟 / {{ store.today?.minuteLimit ?? '-' }}</span>
          </div>
          <div class="today-cell">
            <span class="tc-num">{{ store.today?.remainingQuestions ?? '-' }}</span>
            <span class="tc-label">还能答题</span>
          </div>
        </div>
      </div>

      <!-- 总统计 -->
      <div class="stat-grid">
        <div class="stat-card card">
          <span class="s-num">⭐{{ stats.totalStars }}</span>
          <span class="s-label">总星数</span>
        </div>
        <div class="stat-card card">
          <span class="s-num">🏁{{ stats.passedLevels }}</span>
          <span class="s-label">通关关卡</span>
        </div>
        <div class="stat-card card">
          <span class="s-num">📝{{ stats.answeredTotal }}</span>
          <span class="s-label">答题总数</span>
        </div>
        <div class="stat-card card">
          <span class="s-num">🎯{{ stats.accuracy }}%</span>
          <span class="s-label">正确率</span>
        </div>
      </div>

      <!-- 三个列表 -->
      <div class="tabs card">
        <button
          v-for="t in TABS"
          :key="t.key"
          class="tab-btn"
          :class="{ on: tab === t.key }"
          @click="tab = t.key"
        >
          {{ t.label }}
        </button>
      </div>

      <!-- 兑换记录 -->
      <div v-if="tab === 'orders'" class="list">
        <div v-if="data.redeemOrders.length === 0" class="empty card">
          <span class="emoji">🎁</span>还没有兑换过奖品
        </div>
        <div v-for="o in data.redeemOrders" :key="o.id" class="list-item card">
          <div class="li-main">
            <div class="li-title">{{ o.prizeName }}</div>
            <div class="li-sub">{{ o.createdAt }} · 扣 {{ o.pointsCost }} 分</div>
            <div v-if="o.status === 'REJECTED' && o.remark" class="li-remark">
              家长说明：{{ o.remark }}
            </div>
          </div>
          <span class="order-status" :class="`st-${o.status}`">{{ ORDER_STATUS[o.status] }}</span>
        </div>
      </div>

      <!-- 积分流水 -->
      <div v-else-if="tab === 'logs'" class="list">
        <div v-if="data.pointLogs.length === 0" class="empty card">
          <span class="emoji">⭐</span>还没有积分记录
        </div>
        <div v-for="log in data.pointLogs" :key="log.id" class="list-item card">
          <div class="li-main">
            <div class="li-title">{{ BIZ_LABEL[log.bizType] ?? log.bizType }}</div>
            <div class="li-sub">{{ log.remark }} · {{ log.createdAt }}</div>
          </div>
          <span class="amount" :class="log.changeAmount >= 0 ? 'plus' : 'minus'">
            {{ log.changeAmount >= 0 ? '+' : '' }}{{ log.changeAmount }}
          </span>
        </div>
      </div>

      <!-- 错题本 -->
      <div v-else class="list">
        <!-- 科目筛选 -->
        <div class="tabs card sub-tabs">
          <button
            v-for="s in SUBJECT_TABS"
            :key="s.key"
            class="tab-btn"
            :class="{ on: wrongSubject === s.key }"
            @click="wrongSubject = s.key"
          >
            {{ s.label }}
          </button>
        </div>
        <div v-if="wrongFiltered.length === 0" class="empty card">
          <span class="emoji">🎉</span>{{ wrongSubject === '' ? '错题本是空的，继续保持！' : '该科目还没有错题，继续保持！' }}
        </div>
        <div
          v-for="(w, i) in wrongFiltered"
          :key="w.questionId"
          class="list-item card wrong"
          :class="{ mastered: w.mastered }"
        >
          <div class="li-title">
            <span class="w-idx">{{ i + 1 }}</span>{{ w.stem }}
            <span v-if="w.mastered" class="w-mastered">已掌握</span>
          </div>
          <div class="w-line">
            <span class="w-subject" :class="w.subject">{{ w.subject === 'math' ? '数学' : w.subject === 'english' ? '英语' : '语文' }}</span>
            <span class="w-kp">{{ TYPE_LABEL[w.type] }}{{ w.knowledgePoint ? ' · ' + w.knowledgePoint : '' }} · 错 {{ w.wrongCount }} 次</span>
          </div>
          <div class="w-line">
            <span class="w-label">正确答案</span>
            <span class="w-answer">{{ formatAnswerValue(w.type, w.correctAnswer, w.options) }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TabBar from '@/components/TabBar.vue'
import AvatarFace from '@/components/AvatarFace.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'
import { formatAnswerValue, TYPE_LABEL } from '@/utils/answer'
import { setSfxEnabled, sfxEnabled, sfxTap } from '@/utils/sfx'
import type { OrderStatus, RecordsData, Statistics, WrongQuestion } from '@/types/api'

const router = useRouter()
const store = useChildStore()

const TABS = [
  { key: 'orders', label: '兑换记录' },
  { key: 'logs', label: '积分流水' },
  { key: 'wrong', label: '错题本' }
] as const

type TabKey = (typeof TABS)[number]['key']

const SUBJECT_TABS = [
  { key: '' as const, label: '全部' },
  { key: 'chinese' as const, label: '语文' },
  { key: 'math' as const, label: '数学' },
  { key: 'english' as const, label: '英语' }
]

const ORDER_STATUS: Record<OrderStatus, string> = {
  PENDING: '待家长审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  DELIVERED: '已发放'
}

const BIZ_LABEL: Record<string, string> = {
  ANSWER: '答题得分',
  COMBO: '连对加成',
  PASS: '通关奖励',
  THREE_STAR: '三星奖励',
  REDEEM: '兑换扣减',
  REJECT_REFUND: '兑换退回',
  ADMIN_ADJUST: '家长调整'
}

const EMPTY_STAT: Statistics = {
  points: 0,
  totalEarned: 0,
  answeredTotal: 0,
  correctTotal: 0,
  accuracy: 0,
  sessionTotal: 0,
  passedLevels: 0,
  totalStars: 0
}

const loading = ref(true)
const data = ref<RecordsData | null>(null)
const wrongList = ref<WrongQuestion[]>([])
const tab = ref<TabKey>('orders')

const stats = computed<Statistics>(() => data.value?.statistics ?? EMPTY_STAT)

/* 错题本科目筛选：'' = 全部 */
const wrongSubject = ref<'' | 'chinese' | 'math' | 'english'>('')
const wrongFiltered = computed(() =>
  wrongSubject.value === '' ? wrongList.value : wrongList.value.filter((w) => w.subject === wrongSubject.value)
)

function logout() {
  store.logout()
  router.replace('/')
}

/* 答题音效开关 */
const sfxOn = ref(sfxEnabled())
function toggleSfx() {
  sfxOn.value = !sfxOn.value
  setSfxEnabled(sfxOn.value)
  if (sfxOn.value) sfxTap() // 打开时来一声：既是确认，也顺便解锁音频
}

onMounted(async () => {
  try {
    const [records, wrong] = await Promise.all([
      childApi.records(),
      childApi.wrongQuestions()
    ])
    data.value = records
    wrongList.value = wrong
    if (!store.today) {
      await store.fetchProfile()
    }
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.me-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.me-who {
  flex: 1;
  min-width: 0;
}

.me-nick {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 6px;
}

.me-points-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.mp-chip {
  background: var(--warning-soft);
  color: #ffb347;
  font-size: 14px;
  padding: 3px 12px;
  border-radius: 999px;
}

.mp-chip b {
  font-size: 15px;
}

.logout {
  min-height: var(--min-tap);
  padding: 0 10px;
  color: var(--text-sub);
  font-size: 15px;
}

/* ---------- 答题音效开关 ---------- */
.sfx-row {
  margin-top: 14px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 700;
}

.sfx-txt {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sfx-ico {
  font-size: 20px;
}

.sfx-switch {
  flex: none;
  width: 52px;
  height: 30px;
  border-radius: 999px;
  background: var(--border);
  position: relative;
  transition: background 0.2s ease;
}

.sfx-switch .knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.2s ease;
}

.sfx-switch.on {
  background: var(--primary);
}

.sfx-switch.on .knob {
  transform: translateX(22px);
}

.today {
  margin-top: 14px;
  padding: 16px 20px;
}

.today.exhausted {
  background: var(--warning-soft);
}

.today-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 12px;
}

.today-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  text-align: center;
}

.tc-num {
  display: block;
  font-size: 26px;
  font-weight: 800;
  color: #7be338;
}

.exhausted .tc-num {
  color: #ffb347;
}

.tc-label {
  font-size: 13px;
  color: var(--text-sub);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 14px;
}

.stat-card {
  padding: 14px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.s-num {
  font-size: 19px;
  font-weight: 800;
}

.s-label {
  font-size: 13px;
  color: var(--text-sub);
}

.tabs {
  display: flex;
  margin-top: 16px;
  padding: 6px;
  gap: 6px;
  border-radius: 999px;
}

.tab-btn {
  flex: 1;
  min-height: 44px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-sub);
}

.tab-btn.on {
  background: var(--primary);
  color: #fff;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.li-main {
  flex: 1;
  min-width: 0;
}

.li-title {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.5;
  display: flex;
  gap: 8px;
}

.li-sub {
  color: var(--text-sub);
  font-size: 13px;
  margin-top: 3px;
}

.li-remark {
  margin-top: 4px;
  font-size: 14px;
  color: #ff7a7a;
}

.order-status {
  flex: none;
  font-size: 14px;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
}

.st-PENDING {
  background: var(--warning-soft);
  color: #ffb347;
}

.st-APPROVED {
  background: var(--primary-soft);
  color: #7be338;
}

.st-REJECTED {
  background: var(--danger-soft);
  color: #ff7a7a;
}

.st-DELIVERED {
  background: var(--success-soft);
  color: #7be338;
}

.amount {
  flex: none;
  font-size: 20px;
  font-weight: 800;
}

.amount.plus {
  color: #7be338;
}

.amount.minus {
  color: #ff7a7a;
}

.w-idx {
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

/* 已掌握：整卡弱化 + 绿色标记 */
.list-item.card.wrong.mastered {
  opacity: 0.62;
}

/* 错题本科目筛选：与主 tab 同款，间距略紧 */
.sub-tabs {
  margin-bottom: 12px;
}

/* 科目标签 */
.w-subject {
  flex: none;
  padding: 1px 7px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
}

.w-subject.chinese {
  background: rgba(255, 200, 0, 0.16);
  color: #ffc800;
}

.w-subject.math {
  background: rgba(36, 160, 237, 0.2);
  color: #58b5f0;
}

.w-subject.english {
  background: rgba(178, 108, 255, 0.2);
  color: #c58fff;
}

.w-mastered {
  flex: none;
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(88, 204, 2, 0.18);
  color: #7be338;
  font-size: 12px;
  font-weight: 600;
}

.w-line {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  font-size: 14px;
  align-items: flex-start;
}

.w-kp {
  color: var(--text-sub);
}

.w-label {
  flex: none;
  color: var(--text-sub);
}

.w-answer {
  color: #7be338;
  font-weight: 600;
}
</style>
