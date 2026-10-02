<template>
  <div class="page page-no-tab idiom-page">
    <header class="it-top">
      <button class="back" @click="back">←</button>
      <div class="it-title">
        <h1>成语接龙</h1>
        <p v-if="state === 'playing'">第 {{ levelNo }} 关 · 第 {{ qi + 1 }}/{{ qs.length }} 题 · 答对 {{ okCount }}</p>
      </div>
    </header>

    <!-- 关卡选择 -->
    <template v-if="state === 'pick'">
      <p class="tip">每关 5 题，答对 3 题过关；答完还能看到成语的小典故哦！</p>
      <div class="lv-list">
        <button
          v-for="(lv, i) in LEVELS"
          :key="lv"
          class="lv-card"
          :class="{ locked: !store.isUnlocked('idiom', lv, i) }"
          :disabled="!store.isUnlocked('idiom', lv, i)"
          @click="start(i + 1)"
        >
          <span class="lv-name">{{ i + 1 === 1 ? '初出茅庐' : i + 1 === 2 ? '小露锋芒' : i + 1 === 3 ? '渐入佳境' : i + 1 === 4 ? '驾轻就熟' : i + 1 === 5 ? '炉火纯青' : '出口成章' }}</span>
          <span class="lv-stars">
            <template v-if="store.data.idiom[lv]">{{ '⭐'.repeat(store.data.idiom[lv]) }}{{ '☆'.repeat(3 - store.data.idiom[lv]) }}</template>
            <template v-else-if="!store.isUnlocked('idiom', lv, i)">🔒</template>
            <template v-else>未挑战</template>
          </span>
        </button>
      </div>
    </template>

    <!-- 答题 -->
    <template v-else-if="state === 'playing' && cur">
      <div class="q-card card">
        <div class="q-type">{{ cur.prompt }}</div>
        <div class="q-stem" :class="{ emoji: cur.type === 'emoji' }">{{ cur.stem }}</div>
      </div>

      <div class="opts">
        <button
          v-for="(op, i) in cur.options"
          :key="i"
          class="opt"
          :class="optClass(i)"
          :disabled="locked"
          @click="choose(i)"
        >
          {{ op }}
        </button>
      </div>

      <!-- 答题反馈 -->
      <div v-if="locked" class="feedback card" :class="wasRight ? 'good' : 'bad'">
        <div class="fb-head">{{ wasRight ? '🎉 答对啦！' : '正确答案：' + cur.options[cur.answer] }}</div>
        <div class="fb-pinyin">{{ cur.idiom.pinyin }}</div>
        <div class="fb-meaning">{{ cur.idiom.meaning }}</div>
        <button class="btn btn-primary fb-next" @click="next">
          {{ qi + 1 >= qs.length ? '看结果' : '下一题' }}
        </button>
      </div>
    </template>

    <!-- 通关 -->
    <div v-if="state === 'done'" class="mask">
      <div class="win-card">
        <div class="win-emoji">🏆</div>
        <h2>第 {{ levelNo }} 关通过！</h2>
        <p class="win-stars">{{ '⭐'.repeat(winStars) }}{{ '☆'.repeat(3 - winStars) }}</p>
        <p class="win-sub">答对 {{ okCount }} / {{ qs.length }} 题</p>
        <div v-for="s in stories" :key="s.idiom" class="story-card">
          <div class="st-title">📖 {{ s.idiom }}的小典故</div>
          <div class="st-body">{{ s.story }}</div>
        </div>
        <button class="btn btn-primary" @click="state = 'pick'">返回关卡</button>
      </div>
    </div>

    <!-- 失败 -->
    <div v-if="state === 'failed'" class="mask">
      <div class="win-card">
        <div class="win-emoji">💪</div>
        <h2>差一点点！</h2>
        <p class="win-sub">答对 {{ okCount }} / {{ qs.length }} 题，答对 3 题就能过关，再来一次吧！</p>
        <button class="btn btn-primary" @click="start(levelNo)">再试一次</button>
        <button class="btn btn-ghost" @click="state = 'pick'">返回关卡</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { IDIOMS, MEANING_POOL, EMOJI_GUESS, findChain, sample, type Idiom } from '@/games/idiomData'
import { useGamesStore } from '@/stores/games'

const router = useRouter()
const store = useGamesStore()

const LEVELS = ['idiom-1', 'idiom-2', 'idiom-3', 'idiom-4', 'idiom-5', 'idiom-6']

type QType = 'chain' | 'blank' | 'emoji' | 'meaning'
interface Q {
  type: QType
  prompt: string
  stem: string
  options: string[]
  answer: number
  idiom: Idiom
}

type State = 'pick' | 'playing' | 'done' | 'failed'
const state = ref<State>('pick')
const levelNo = ref(1)
const qs = ref<Q[]>([])
const qi = ref(0)
const locked = ref(false)
const picked = ref(-1)
const wasRight = ref(false)
const okCount = ref(0)
const winStars = ref(3)

const cur = computed(() => qs.value[qi.value] ?? null)

/** 本关出现的带典故成语，通关后展示（最多 2 个） */
const stories = computed(() =>
  qs.value.map((q) => q.idiom).filter((x) => x.story).slice(0, 2)
)

/** 选项打乱 */
function shuffle<T>(arr: T[]): T[] {
  return sample(arr, arr.length)
}

function randOf<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

/* ---- 四种题型 ---- */

/** 接龙：给一个成语，选出以其末字开头的新成语 */
function buildChain(): Q | null {
  for (let tries = 0; tries < 40; tries++) {
    const curIdiom = randOf(IDIOMS)
    const pool = findChain(curIdiom.idiom)
    if (!pool.length) continue
    const correct = randOf(pool)
    const distract = sample(
      IDIOMS.filter((x) => x.idiom !== correct.idiom && x.idiom[0] !== curIdiom.idiom[curIdiom.idiom.length - 1]),
      3
    )
    if (distract.length < 3) continue
    const options = shuffle([correct.idiom, ...distract.map((x) => x.idiom)])
    return {
      type: 'chain',
      prompt: '🐉 接龙：选一个以「' + curIdiom.idiom.slice(-1) + '」开头的成语',
      stem: curIdiom.idiom + '　' + curIdiom.pinyin,
      options,
      answer: options.indexOf(correct.idiom),
      idiom: correct
    }
  }
  return null
}

/** 填空：成语缺一个字，从字卡中补上 */
function buildBlank(): Q {
  const it = randOf(IDIOMS)
  const i = Math.floor(Math.random() * it.idiom.length)
  const ch = it.idiom[i]
  const distract = sample([...CHARS].filter((c) => c !== ch), 3)
  const options = shuffle([ch, ...distract])
  return {
    type: 'blank',
    prompt: '✏️ 填空：把缺的字补上',
    stem: replaceAll(it.idiom, ch, '❓') + '　' + it.pinyin,
    options,
    answer: options.indexOf(ch),
    idiom: it
  }
}

/** 全部替换（iOS 11 无 replaceAll，用 split/join 实现） */
function replaceAll(s: string, find: string, rep: string): string {
  return s.split(find).join(rep)
}

/** 看图猜词：emoji 表情猜成语 */
function buildEmoji(): Q {
  const e = randOf(EMOJI_GUESS)
  const correct = IDIOMS.find((x) => x.idiom === e.answer)!
  const distract = sample(IDIOMS.filter((x) => x.idiom !== e.answer), 3)
  const options = shuffle([correct.idiom, ...distract.map((x) => x.idiom)])
  return {
    type: 'emoji',
    prompt: '👀 看图猜词：这些表情是什么成语？',
    stem: e.emojis,
    options,
    answer: options.indexOf(correct.idiom),
    idiom: correct
  }
}

/** 补全释义：释义里挖掉关键词，选词补全 */
function buildMeaning(): Q {
  const it = randOf(MEANING_POOL)
  const distract = sample(
    MEANING_POOL.filter((x) => x.key !== it.key).map((x) => x.key!),
    3
  )
  const options = shuffle([it.key!, ...distract])
  return {
    type: 'meaning',
    prompt: '📝 补全释义：选词把意思补充完整',
    stem: '「' + it.idiom + '」' + replaceAll(it.meaning, it.key!, ' ❓ '),
    options,
    answer: options.indexOf(it.key!),
    idiom: it
  }
}

/** 全库用字（填空干扰项） */
const CHARS = new Set<string>()
for (const it of IDIOMS) for (const c of it.idiom) CHARS.add(c)

function buildLevel(): Q[] {
  // 每关 4 种题型各一题，再随机加一题
  const base = shuffle<QType>(['chain', 'blank', 'emoji', 'meaning'])
  base.push(randOf(base))
  return base.map((t) => {
    if (t === 'chain') return buildChain() ?? buildBlank()
    if (t === 'emoji') return buildEmoji()
    if (t === 'meaning') return buildMeaning()
    return buildBlank()
  })
}

function start(n: number) {
  levelNo.value = n
  qs.value = buildLevel()
  qi.value = 0
  okCount.value = 0
  locked.value = false
  picked.value = -1
  state.value = 'playing'
}

function choose(i: number) {
  if (locked.value) return
  locked.value = true
  picked.value = i
  wasRight.value = i === cur.value!.answer
  if (wasRight.value) okCount.value++
}

function optClass(i: number): Record<string, boolean> {
  if (!locked.value) return {}
  const cls: Record<string, boolean> = {}
  if (i === cur.value!.answer) cls.right = true
  else if (i === picked.value) cls.wrong = true
  return cls
}

function next() {
  if (qi.value + 1 >= qs.value.length) {
    // 结算
    const c = okCount.value
    if (c >= 3) {
      winStars.value = c === 5 ? 3 : c === 4 ? 2 : 1
      store.complete('idiom', LEVELS[levelNo.value - 1], winStars.value)
      state.value = 'done'
    } else {
      state.value = 'failed'
    }
    return
  }
  qi.value++
  locked.value = false
  picked.value = -1
}

function back() {
  if (state.value === 'pick') router.replace('/games')
  else state.value = 'pick'
}
</script>

<style scoped>
.idiom-page {
  max-width: 560px;
  margin: 0 auto;
}

.it-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 4px 10px;
}

.back {
  min-width: 44px;
  min-height: 44px;
  color: var(--text-sub);
  font-size: 20px;
}

.it-title {
  flex: 1;
}

.it-title h1 {
  font-size: 20px;
  font-weight: 800;
}

.it-title p {
  font-size: 13px;
  color: var(--text-sub);
}

.tip {
  text-align: center;
  color: var(--warning, #e8b64c);
  font-size: 14px;
  margin: 10px 0 2px;
}

/* ---- 关卡选择 ---- */
.lv-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.lv-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-radius: 18px;
  background: var(--card-2);
  border: 1px solid var(--border);
}

.lv-card:active {
  transform: scale(0.98);
}

.lv-card.locked {
  opacity: 0.55;
}

.lv-name {
  font-size: 17px;
  font-weight: 800;
}

.lv-stars {
  font-size: 15px;
  color: var(--text-sub);
}

/* ---- 答题 ---- */
.q-card {
  margin-top: 8px;
  padding: 18px;
}

.q-type {
  font-size: 14px;
  color: var(--primary);
  font-weight: 700;
}

.q-stem {
  font-size: 26px;
  font-weight: 800;
  margin-top: 12px;
  letter-spacing: 2px;
}

.q-stem.emoji {
  letter-spacing: 6px;
  text-align: center;
  font-size: 34px;
}

.opts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

.opt {
  min-height: 58px;
  border-radius: 14px;
  background: var(--card-2);
  border: 2px solid var(--border-light);
  font-size: 17px;
  font-weight: 700;
  padding: 8px 10px;
}

.opt.right {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.opt.wrong {
  border-color: var(--danger);
  background: rgba(216, 67, 67, 0.12);
}

.opt:disabled {
  cursor: default;
}

/* ---- 反馈 ---- */
.feedback {
  margin-top: 14px;
  padding: 16px;
}

.feedback.good {
  border: 2px solid var(--primary);
}

.feedback.bad {
  border: 2px solid var(--danger);
}

.fb-head {
  font-size: 16px;
  font-weight: 800;
}

.fb-pinyin {
  font-size: 14px;
  color: var(--primary);
  margin-top: 6px;
}

.fb-meaning {
  font-size: 14px;
  color: var(--text-sub);
  margin-top: 4px;
  line-height: 1.5;
}

.fb-next {
  margin-top: 12px;
  width: 100%;
}

/* ---- 弹层 ---- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
}

.win-card {
  width: 82%;
  max-width: 360px;
  max-height: 80vh;
  overflow-y: auto;
  background: var(--card-2);
  border-radius: 22px;
  padding: 26px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.win-emoji {
  font-size: 46px;
}

.win-card h2 {
  font-size: 20px;
}

.win-stars {
  font-size: 26px;
}

.win-sub {
  color: var(--text-sub);
  font-size: 14px;
  margin-bottom: 6px;
}

.story-card {
  text-align: left;
  background: var(--primary-soft);
  border-radius: 14px;
  padding: 12px 14px;
  margin-top: 2px;
}

.st-title {
  font-size: 14px;
  font-weight: 800;
}

.st-body {
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.6;
  margin-top: 4px;
}
</style>
