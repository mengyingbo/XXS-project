/**
 * 极简 CSV 解析（RFC 4180：支持双引号包裹、"" 转义、逗号与换行）
 * 与后端 /question/import/template 的模板格式一致
 */
export function parseCsv(text: string): string[][] {
  const rows: string[][] = []
  let row: string[] = []
  let field = ''
  let inQuotes = false
  const src = text.replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n')

  for (let i = 0; i < src.length; i++) {
    const ch = src[i]
    if (inQuotes) {
      if (ch === '"') {
        if (src[i + 1] === '"') {
          field += '"'
          i++
        } else {
          inQuotes = false
        }
      } else {
        field += ch
      }
      continue
    }
    if (ch === '"') {
      inQuotes = true
    } else if (ch === ',') {
      row.push(field)
      field = ''
    } else if (ch === '\n') {
      row.push(field)
      field = ''
      if (row.some((c) => c.trim() !== '')) rows.push(row)
      row = []
    } else {
      field += ch
    }
  }
  row.push(field)
  if (row.some((c) => c.trim() !== '')) rows.push(row)
  return rows
}

/** CSV 行转导入请求行（与后端 ImportRow 对齐） */
export interface CsvImportRow {
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

const EXPECTED_HEADER = ['lessonId', 'levelId', 'type', 'stem', 'options', 'answer', 'analysis', 'knowledgePoint', 'difficulty']

/**
 * 按题型规范化答案：后端要求 answer 必须是合法 JSON。
 * - BLANK：裸文本自动包裹为 JSON 字符串（官方模板示例即为裸文本）
 * - JUDGE：对/错等同义词映射为 true/false
 * - 其余题型原样透传，由后端校验报错
 */
function normalizeAnswer(type: string, answer: string): string {
  if (answer === '') return answer
  if (type === 'BLANK') {
    try {
      JSON.parse(answer)
      return answer // 已是合法 JSON（文本或数组）
    } catch {
      return JSON.stringify(answer)
    }
  }
  if (type === 'JUDGE') {
    const lower = answer.trim().toLowerCase()
    if (['true', '对', '正确', '是', '√'].includes(lower)) return 'true'
    if (['false', '错', '错误', '否', '×'].includes(lower)) return 'false'
    return answer
  }
  return answer
}

/**
 * 解析 CSV 文本为导入行。
 * 首行必须是模板表头（列顺序不限但列名须齐全）；返回 { rows, error }
 */
export function csvToImportRows(text: string): { rows: CsvImportRow[]; error: string } {
  const table = parseCsv(text)
  if (table.length === 0) return { rows: [], error: 'CSV 内容为空' }

  const header = table[0].map((c) => c.trim())
  const missing = EXPECTED_HEADER.filter((h) => !header.includes(h))
  if (missing.length > 0) {
    return { rows: [], error: `CSV 表头缺少列：${missing.join('、')}` }
  }
  // 兼容 iOS 11（Object.fromEntries 为 ES2019，iOS 11 不支持）
  const col: Record<string, number> = {}
  header.forEach((h, i) => { col[h] = i })

  const rows: CsvImportRow[] = []
  for (let r = 1; r < table.length; r++) {
    const cells = table[r]
    const get = (name: string) => (col[name] < cells.length ? cells[col[name]].trim() : '')
    const num = (name: string): number | null => {
      const v = get(name)
      if (v === '') return null
      const n = Number(v)
      return Number.isFinite(n) ? n : null
    }
    const type = get('type').toUpperCase()
    if (!type && !get('stem')) continue // 空行跳过
    rows.push({
      lessonId: num('lessonId'),
      levelId: num('levelId'),
      type,
      stem: get('stem'),
      options: get('options') === '' ? null : get('options'),
      answer: normalizeAnswer(type, get('answer')),
      analysis: get('analysis') === '' ? null : get('analysis'),
      knowledgePoint: get('knowledgePoint') === '' ? null : get('knowledgePoint'),
      difficulty: num('difficulty')
    })
  }
  if (rows.length === 0) return { rows: [], error: '未解析到有效数据行' }
  return { rows, error: '' }
}
