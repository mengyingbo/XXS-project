// 孩子端接口类型（字段与后端 service 返回的 Map 结构严格对齐）

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface ChildBrief {
  id: number
  nickname: string
  avatar: string
}

export interface TodayUsage {
  answeredToday: number
  usedMinutesToday: number
  questionLimit: number
  minuteLimit: number
  remainingQuestions: number
  remainingMinutes: number
  limitReached: boolean
}

// ---- 闯关地图 ----

export type LevelStatus = 'LOCKED' | 'UNLOCKED' | 'PASSED'

export interface LevelNode {
  id: number
  levelNo: number
  name: string
  questionCount: number
  status: LevelStatus
  locked: boolean
  stars: number
  bestAccuracy: number | null
  attemptCount: number
}

export interface LessonNode {
  id: number
  lessonNo: number
  title: string
  lessonType: string
  isSkim: boolean
  locked: boolean
  completed: boolean
  levelCount: number
  levels: LevelNode[]
}

export interface UnitNode {
  id: number
  unitNo: number
  title: string
  description: string | null
  locked: boolean
  completed: boolean
  levelCount: number
  passedCount: number
  stars: number
  lessons: LessonNode[]
}

export interface GameMap {
  currentLevelId: number | null
  totalLevels: number
  passedLevels: number
  totalStars: number
  maxStars: number
  units: UnitNode[]
}

// ---- 答题 ----

export type QuestionType = 'SINGLE' | 'JUDGE' | 'BLANK' | 'ORDER' | 'HAND'

export interface QuestionNode {
  id: number
  type: QuestionType
  stem: string
  options: string[] | null
  /** 仅 HAND 题下发：可接受答案（供前端宽松匹配） */
  answer?: string[] | null
  difficulty: number
  knowledgePoint: string | null
}

export interface SessionStart {
  levelId: number
  levelName: string
  lessonId: number
  lessonTitle: string
  questionCount: number
  questions: QuestionNode[]
  showAnalysisImmediately: boolean
  today: TodayUsage
}

export interface AnswerItemReq {
  questionId: number
  userAnswer: string // JSON 字符串；空串表示跳过
  durationMs?: number
}

export interface AnswerDetail {
  questionId: number
  type: QuestionType
  stem: string
  options: string[] | null
  userAnswer: unknown
  correctAnswer: unknown
  isCorrect: boolean
  analysis: string | null
  knowledgePoint: string | null
}

export interface PointsBreakdown {
  correct: number
  combo: number
  passBonus: number
  threeStarBonus: number
}

/** /child/session/check 只读判题响应 */
export interface AnswerCheck {
  questionId: number
  isCorrect: boolean
  correctAnswer: unknown
  analysis: string | null
}

export interface SessionSubmit {
  sessionId: number
  levelId: number
  totalCount: number
  correctCount: number
  accuracy: number
  stars: number
  passed: boolean
  firstPass: boolean
  pointsGained: number
  pointsBreakdown: PointsBreakdown
  points: number
  nextLevelId: number | null
  details: AnswerDetail[]
  today: TodayUsage
}

// ---- 商城 ----

export interface PrizeItem {
  id: number
  name: string
  image: string | null
  pointsCost: number
  stock: number
  description: string | null
  enoughPoints: boolean
  canRedeem: boolean
}

export interface PrizeList {
  points: number
  prizes: PrizeItem[]
}

export interface RedeemResult {
  orderId: number
  prizeName: string
  pointsCost: number
  status: string
  points: number
}

// ---- 我的 ----

export interface Statistics {
  points: number
  totalEarned: number
  answeredTotal: number
  correctTotal: number
  accuracy: number
  sessionTotal: number
  passedLevels: number
  totalStars: number
}

export type OrderStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELIVERED'

export interface RedeemOrderNode {
  id: number
  prizeName: string
  pointsCost: number
  status: OrderStatus
  remark: string
  createdAt: string
  handledAt: string | null
}

export interface PointLogNode {
  id: number
  changeAmount: number
  balanceAfter: number
  bizType: string
  remark: string
  createdAt: string
}

export interface RecordsData {
  child: ChildBrief
  statistics: Statistics
  redeemOrders: RedeemOrderNode[]
  pointLogs: PointLogNode[]
}

export interface WrongQuestion {
  questionId: number
  type: QuestionType
  stem: string
  options: string[] | null
  /** 已由 api 层 JSON.parse 的规范化答案值 */
  correctAnswer: unknown
  knowledgePoint: string | null
  wrongCount: number
  /** 最近一次作答是否正确（true = 已掌握） */
  mastered: boolean
  /** 所属科目（用于错题本科目筛选） */
  subject: 'chinese' | 'math' | 'english'
  unitTitle?: string
  lessonTitle?: string
  analysis?: string | null
}
