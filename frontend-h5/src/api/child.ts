import { request } from './http'
import type {
  AnswerItemReq,
  ChildBrief,
  GameMap,
  PrizeList,
  RecordsData,
  RedeemResult,
  SessionStart,
  SessionSubmit,
  WrongQuestion
} from '@/types/api'

/** 孩子端全部接口（需求文档 8.1，共 11 个） */
export const childApi = {
  list(): Promise<ChildBrief[]> {
    return request({ url: '/child/list', method: 'GET' })
  },

  register(data: { nickname: string; avatar: string; pin: string }): Promise<ChildBrief> {
    return request({ url: '/child/register', method: 'POST', data })
  },

  login(data: { childId: number; pin: string }): Promise<{
    token: string
    expiresIn: number
    child: ChildBrief
  }> {
    return request({ url: '/child/login', method: 'POST', data })
  },

  profile(): Promise<{
    child: ChildBrief
    points: number
    totalEarned: number
    today: import('@/types/api').TodayUsage
    showAnalysisImmediately: boolean
  }> {
    return request({ url: '/child/profile', method: 'GET' })
  },

  map(): Promise<GameMap> {
    return request({ url: '/child/map', method: 'GET' })
  },

  start(levelId: number): Promise<SessionStart> {
    return request({ url: '/child/session/start', method: 'POST', data: { levelId } })
  },

  submit(data: {
    levelId: number
    durationMs?: number
    answers: AnswerItemReq[]
  }): Promise<SessionSubmit> {
    return request({ url: '/child/session/submit', method: 'POST', data })
  },

  prizeList(): Promise<PrizeList> {
    return request({ url: '/child/prize/list', method: 'GET' })
  },

  redeem(prizeId: number): Promise<RedeemResult> {
    return request({ url: '/child/prize/redeem', method: 'POST', data: { prizeId } })
  },

  records(): Promise<RecordsData> {
    return request({ url: '/child/records', method: 'GET' })
  },

  async wrongQuestions(): Promise<WrongQuestion[]> {
    // 后端 answer/options 为 JSON 字符串，这里统一 parse 成结构化值
    const rawList = (await request({ url: '/child/wrong-questions', method: 'GET' })) as Array<{
      questionId: number
      type: WrongQuestion['type']
      stem: string
      options?: string | null
      answer?: string | null
      knowledgePoint?: string | null
      wrongCount: number
      unitTitle?: string
      lessonTitle?: string
      analysis?: string | null
    }>
    const parseJson = (s?: string | null): unknown => {
      if (s === null || s === undefined || s === '') return null
      try {
        return JSON.parse(s)
      } catch {
        return s
      }
    }
    return rawList.map((w) => ({
      questionId: w.questionId,
      type: w.type,
      stem: w.stem,
      options: (parseJson(w.options) as string[] | null) ?? null,
      correctAnswer: parseJson(w.answer),
      knowledgePoint: w.knowledgePoint ?? null,
      wrongCount: w.wrongCount,
      unitTitle: w.unitTitle,
      lessonTitle: w.lessonTitle,
      analysis: w.analysis ?? null
    }))
  }
}
