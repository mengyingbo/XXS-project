import type { QuestionType } from '@/types/api'

/** 孩子作答的内存值：单选下标 / 判断布尔 / 填空文本 / 排序下标数组 */
export type AnswerValue = number | boolean | string | number[] | null

/** 序列化为后端要求的 userAnswer JSON 字符串；空值=跳过(记错) */
export function serializeAnswer(value: AnswerValue): string {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value.trim() === '' ? '' : JSON.stringify(value)
  if (Array.isArray(value)) return value.length === 0 ? '' : JSON.stringify(value)
  return JSON.stringify(value)
}

export function isAnswered(value: AnswerValue): boolean {
  if (value === null) return false
  if (typeof value === 'string') return value.trim() !== ''
  if (Array.isArray(value)) return value.length > 0
  return true
}

export const OPTION_LETTERS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

export const TYPE_LABEL: Record<QuestionType, string> = {
  SINGLE: '单选题',
  JUDGE: '判断题',
  BLANK: '填空题',
  ORDER: '排一排'
}

/** 结算详情里把答案值格式化成人看的文本 */
export function formatAnswerValue(
  type: QuestionType,
  value: unknown,
  options: string[] | null
): string {
  if (value === null || value === undefined || value === '') return '（未作答）'
  switch (type) {
    case 'SINGLE': {
      const idx = Number(value)
      if (Number.isNaN(idx) || !options || idx < 0 || idx >= options.length) return '（未作答）'
      return `${OPTION_LETTERS[idx]}. ${options[idx]}`
    }
    case 'JUDGE':
      return value === true ? '✓ 正确' : '✗ 错误'
    case 'BLANK':
      return Array.isArray(value) ? value.join(' / ') : String(value)
    case 'ORDER': {
      if (!Array.isArray(value) || !options) return '（未作答）'
      return (value as number[])
        .map((idx, i) => {
          const text = options[idx] ?? '?'
          return `${i + 1}. ${text}`
        })
        .join('　→　')
    }
    default:
      return String(value)
  }
}
