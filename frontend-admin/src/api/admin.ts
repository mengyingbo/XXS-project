import { request } from './http'
import type {
  AdjustPointsResult,
  Child,
  ConfigRow,
  DashboardData,
  ImportResult,
  ImportRowReq,
  Lesson,
  Level,
  LoginResult,
  PageData,
  Prize,
  Question,
  QuestionType,
  RedeemHandleResult,
  RedeemOrder,
  StatsData,
  Unit,
  UploadResult
} from '@/types/api'

/** 管理端全部接口（需求文档 8.2） */
export const adminApi = {
  // ---------------- 登录 / 密码 ----------------
  login(data: { username: string; password: string }): Promise<LoginResult> {
    return request({ url: '/login', method: 'POST', data })
  },
  changePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
    return request({ url: '/password', method: 'PUT', data })
  },

  // ---------------- 仪表盘 / 统计 ----------------
  dashboard(): Promise<DashboardData> {
    return request({ url: '/dashboard', method: 'GET' })
  },
  stats(params: { childId?: number; days?: number }): Promise<StatsData> {
    return request({ url: '/stats', method: 'GET', params })
  },

  // ---------------- 孩子管理 ----------------
  childList(params: { page: number; size: number; keyword?: string }): Promise<PageData<Child>> {
    return request({ url: '/child', method: 'GET', params })
  },
  childCreate(data: { nickname: string; avatar?: string; pin: string; enabled?: boolean }): Promise<Child> {
    return request({ url: '/child', method: 'POST', data })
  },
  childUpdate(id: number, data: { nickname: string; avatar?: string; enabled?: boolean }): Promise<Child> {
    return request({ url: `/child/${id}`, method: 'PUT', data })
  },
  childDelete(id: number): Promise<void> {
    return request({ url: `/child/${id}`, method: 'DELETE' })
  },
  childResetPin(id: number, pin: string): Promise<void> {
    return request({ url: `/child/${id}/pin`, method: 'PUT', data: { pin } })
  },
  childAdjustPoints(id: number, data: { delta: number; remark?: string }): Promise<AdjustPointsResult> {
    return request({ url: `/child/${id}/points`, method: 'PUT', data })
  },
  childResetLevelProgress(id: number, levelId: number): Promise<unknown> {
    return request({ url: `/child/${id}/progress/${levelId}`, method: 'DELETE' })
  },
  childResetAllProgress(id: number): Promise<unknown> {
    return request({ url: `/child/${id}/progress`, method: 'DELETE' })
  },

  // ---------------- 单元 / 课文 / 关卡 ----------------
  unitList(): Promise<Unit[]> {
    return request({ url: '/unit', method: 'GET' })
  },
  unitCreate(data: { subject?: string; unitNo: number; title: string; description?: string; sortOrder?: number }): Promise<Unit> {
    return request({ url: '/unit', method: 'POST', data })
  },
  unitUpdate(id: number, data: { subject?: string; unitNo: number; title: string; description?: string; sortOrder?: number }): Promise<Unit> {
    return request({ url: `/unit/${id}`, method: 'PUT', data })
  },
  unitDelete(id: number): Promise<void> {
    return request({ url: `/unit/${id}`, method: 'DELETE' })
  },

  lessonList(params?: { unitId?: number }): Promise<Lesson[]> {
    return request({ url: '/lesson', method: 'GET', params })
  },
  lessonCreate(data: {
    unitId: number
    lessonNo: number
    title: string
    lessonType: 'TEXT' | 'GARDEN' | 'PRACTICE' | 'FUN'
    isSkim?: boolean
    sortOrder?: number
  }): Promise<Lesson> {
    return request({ url: '/lesson', method: 'POST', data })
  },
  lessonUpdate(id: number, data: {
    unitId: number
    lessonNo: number
    title: string
    lessonType: 'TEXT' | 'GARDEN' | 'PRACTICE' | 'FUN'
    isSkim?: boolean
    sortOrder?: number
  }): Promise<Lesson> {
    return request({ url: `/lesson/${id}`, method: 'PUT', data })
  },
  lessonDelete(id: number): Promise<void> {
    return request({ url: `/lesson/${id}`, method: 'DELETE' })
  },

  levelList(params?: { lessonId?: number }): Promise<Level[]> {
    return request({ url: '/level', method: 'GET', params })
  },
  levelCreate(data: { lessonId: number; levelNo: number; name?: string; questionCount?: number; sortOrder?: number }): Promise<Level> {
    return request({ url: '/level', method: 'POST', data })
  },
  levelUpdate(id: number, data: { lessonId: number; levelNo: number; name?: string; questionCount?: number; sortOrder?: number }): Promise<Level> {
    return request({ url: `/level/${id}`, method: 'PUT', data })
  },
  levelDelete(id: number): Promise<void> {
    return request({ url: `/level/${id}`, method: 'DELETE' })
  },

  // ---------------- 题目 ----------------
  questionList(params: {
    page: number
    size: number
    lessonId?: number
    levelId?: number
    type?: QuestionType | ''
    keyword?: string
    subject?: string
  }): Promise<PageData<Question>> {
    return request({ url: '/question', method: 'GET', params })
  },
  questionGet(id: number): Promise<Question> {
    return request({ url: `/question/${id}`, method: 'GET' })
  },
  questionCreate(data: {
    lessonId: number
    levelId?: number | null
    type: QuestionType
    stem: string
    options?: string | null
    answer: string
    analysis?: string
    knowledgePoint?: string
    difficulty?: number
    sortOrder?: number
  }): Promise<Question> {
    return request({ url: '/question', method: 'POST', data })
  },
  questionUpdate(id: number, data: {
    lessonId: number
    levelId?: number | null
    type: QuestionType
    stem: string
    options?: string | null
    answer: string
    analysis?: string
    knowledgePoint?: string
    difficulty?: number
    sortOrder?: number
  }): Promise<Question> {
    return request({ url: `/question/${id}`, method: 'PUT', data })
  },
  questionDelete(id: number): Promise<void> {
    return request({ url: `/question/${id}`, method: 'DELETE' })
  },
  importTemplate(): Promise<string> {
    return request({ url: '/question/import/template', method: 'GET' })
  },
  importQuestions(data: {
    dryRun: boolean
    defaultLessonId?: number | null
    defaultLevelId?: number | null
    rows: ImportRowReq[]
  }): Promise<ImportResult> {
    return request({ url: '/question/import', method: 'POST', data })
  },

  // ---------------- 奖品 / 兑换 ----------------
  prizeList(params: { page: number; size: number; keyword?: string; enabled?: boolean }): Promise<PageData<Prize>> {
    return request({ url: '/prize', method: 'GET', params })
  },
  prizeCreate(data: {
    name: string
    image?: string
    pointsCost: number
    stock: number
    enabled?: boolean
    description?: string
    sortOrder?: number
  }): Promise<Prize> {
    return request({ url: '/prize', method: 'POST', data })
  },
  prizeUpdate(id: number, data: {
    name: string
    image?: string
    pointsCost: number
    stock: number
    enabled?: boolean
    description?: string
    sortOrder?: number
  }): Promise<Prize> {
    return request({ url: `/prize/${id}`, method: 'PUT', data })
  },
  prizeDelete(id: number): Promise<void> {
    return request({ url: `/prize/${id}`, method: 'DELETE' })
  },
  redeemList(params: { page: number; size: number; status?: string; childId?: number }): Promise<PageData<RedeemOrder>> {
    return request({ url: '/redeem', method: 'GET', params })
  },
  redeemHandle(id: number, data: { action: 'APPROVE' | 'REJECT' | 'DELIVER'; remark?: string }): Promise<RedeemHandleResult> {
    return request({ url: `/redeem/${id}`, method: 'PUT', data })
  },

  // ---------------- 规则配置 ----------------
  configList(): Promise<ConfigRow[]> {
    return request({ url: '/config', method: 'GET' })
  },
  configUpdate(values: Record<string, string>): Promise<void> {
    return request({ url: '/config', method: 'PUT', data: values })
  },

  // ---------------- 文件上传 ----------------
  upload(file: File): Promise<UploadResult> {
    const form = new FormData()
    form.append('file', file)
    return request({
      url: '/upload',
      method: 'POST',
      data: form,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
