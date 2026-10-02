/** 通用格式化工具 */

export function fmtDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

export const REDEEM_STATUS_LABEL: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  DELIVERED: '已发放',
  REJECTED: '已拒绝'
}

export const REDEEM_STATUS_TAG: Record<string, 'warning' | 'primary' | 'success' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'primary',
  DELIVERED: 'success',
  REJECTED: 'danger'
}

export const QUESTION_TYPE_LABEL: Record<string, string> = {
  SINGLE: '单选题',
  JUDGE: '判断题',
  BLANK: '填空题',
  ORDER: '排序题',
  HAND: '词语手写'
}

/** 把答案/选项 JSON 字符串格式化成可读文本 */
export function fmtAnswer(type: string, answerJson: string | null, optionsJson: string | null): string {
  if (!answerJson) return '—'
  try {
    const answer = JSON.parse(answerJson)
    const options: string[] | null = optionsJson ? JSON.parse(optionsJson) : null
    switch (type) {
      case 'SINGLE':
        return options && typeof answer === 'number' && options[answer] != null
          ? `选项${answer + 1}. ${options[answer]}`
          : String(answer)
      case 'JUDGE':
        return answer === true ? '正确' : '错误'
      case 'ORDER':
        return Array.isArray(answer) && options
          ? answer.map((i: number, n: number) => `${n + 1}.${options[i] ?? '?'}`).join(' → ')
          : JSON.stringify(answer)
      default:
        return Array.isArray(answer) ? answer.join(' / ') : String(answer)
    }
  } catch {
    return answerJson
  }
}
