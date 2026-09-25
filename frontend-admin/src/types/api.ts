/** 统一响应体 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页响应（后端 IPage 结构） */
export interface PageData<T> {
  total: number
  page: number
  size: number
  records: T[]
}

// ---------------- 实体 ----------------

export interface AdminInfo {
  id: number
  username: string
  nickname: string | null
  mustChangePwd: boolean
}

export interface LoginResult {
  token: string
  expiresIn: number
  admin: AdminInfo
}

export interface Child {
  id: number
  nickname: string
  avatar: string
  totalPoints: number
  totalEarned: number
  pinFailCount: number
  lockedUntil: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface Unit {
  id: number
  unitNo: number
  title: string
  description: string
  sortOrder: number
}

export interface Lesson {
  id: number
  unitId: number
  lessonNo: number
  title: string
  /** TEXT 课文 / GARDEN 语文园地 */
  lessonType: 'TEXT' | 'GARDEN'
  isSkim: boolean
  sortOrder: number
}

export interface Level {
  id: number
  lessonId: number
  levelNo: number
  name: string
  questionCount: number
  sortOrder: number
}

export type QuestionType = 'SINGLE' | 'JUDGE' | 'BLANK' | 'ORDER'

export interface Question {
  id: number
  lessonId: number
  levelId: number | null
  type: QuestionType
  stem: string
  /** JSON 字符串：SINGLE/ORDER 为选项数组 */
  options: string | null
  /** JSON 字符串：SINGLE 数字下标 / JUDGE 布尔 / BLANK 文本或数组 / ORDER 下标数组 */
  answer: string
  analysis: string
  knowledgePoint: string
  difficulty: number
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface Prize {
  id: number
  name: string
  image: string
  pointsCost: number
  stock: number
  enabled: boolean
  description: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface RedeemOrder {
  id: number
  childId: number
  childNickname: string
  prizeId: number
  prizeName: string
  pointsCost: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELIVERED'
  remark: string
  createdAt: string
  handledAt: string | null
}

// ---------------- 仪表盘 / 统计 ----------------

export interface DashboardData {
  childCount: number
  enabledChildCount: number
  todayActiveChildren: number
  todayAnswers: number
  todaySessions: number
  totalIssuedPoints: number
  totalAvailablePoints: number
  redeemCounts: Record<'PENDING' | 'APPROVED' | 'DELIVERED' | 'REJECTED', number>
  pendingRedeemCount: number
  catalog: {
    unitCount: number
    lessonCount: number
    levelCount: number
    questionCount: number
  }
}

export interface StatsRow {
  total: number
  correct: number
  accuracy: number
}

export interface StatsByChild extends StatsRow {
  childId: number
  nickname: string
}

export interface StatsByLesson extends StatsRow {
  lessonId: number
  lessonTitle: string
  unitTitle: string
}

export interface StatsByKnowledgePoint extends StatsRow {
  knowledgePoint: string
}

export interface StatsData {
  childId: number | null
  days: number | null
  byChild: StatsByChild[]
  byLesson: StatsByLesson[]
  byKnowledgePoint: StatsByKnowledgePoint[]
  weakKnowledgePoints: StatsByKnowledgePoint[]
}

export interface ConfigRow {
  configKey: string
  configValue: string
  defaultValue: string
  remark: string
}

// ---------------- 批量导入 ----------------

export interface ImportRowReq {
  lessonId?: number | null
  levelId?: number | null
  type: string
  stem: string
  options?: string | null
  answer: string
  analysis?: string | null
  knowledgePoint?: string | null
  difficulty?: number | null
}

export interface ImportRowPreview {
  index: number
  lessonId: number | null
  levelId: number | null
  type: string
  stem: string
  errors: string[]
  ok: boolean
}

export interface ImportResult {
  dryRun: boolean
  total: number
  validCount: number
  invalidCount: number
  inserted: number
  rows: ImportRowPreview[]
}

// ---------------- 操作返回 ----------------

export interface AdjustPointsResult {
  childId: number
  delta: number
  points: number
}

export interface RedeemHandleResult {
  id: number
  status: string
  points: number
}

export interface UploadResult {
  fileName: string
  url: string
}
