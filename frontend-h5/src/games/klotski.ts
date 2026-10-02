/**
 * 华容道数据与求解器（v2.1 游戏乐园）
 *
 * 模式 A 传统华容道：4 列 × 5 行，棋子类型
 *   C 曹操 2×2 / V 竖将 1×2 / H 横将 2×1 / P 兵 1×1
 *   布局用字符串描述："VCCV|VCCV|VHHV|VPPV|P..P"
 *   同型棋子由字符相同标识（移动时同字符整体处理，位置由扫描自动恢复）
 * 胜利：C 的顶行 y === 3（即占据 3、4 两行，滑出底部出口）
 *
 * 模式 B 路径规划（Rush Hour）：6 列 × 6 行
 *   布局串每行 6 字符：a~z 唯一滑块，'.' 空，小写为主车（长度 2，横放，y=出口行）
 * 胜利：主车左端 x === 4
 */

export interface Piece {
  id: string
  w: number
  h: number
  x: number
  y: number
}

export interface KlotskiLevel {
  name: string
  mode: 'classic' | 'rush'
  rows: string[]
  /** 思维训练重点标注 */
  focus: string
}

/* ---------------- 传统华容道关卡（每棋子唯一字符，全部经 BFS 验证可解，按步数难度排序） ----------------
 * 约定：C 曹操；V W X Y 竖将；H I J 横将；p q r s t u 兵 */
export const CLASSIC_LEVELS: KlotskiLevel[] = [
  {
    name: '一夫当关',
    mode: 'classic',
    focus: '步骤预判 · 竖将的空隙',
    rows: ['CVW.', 'CVW.', 'HH..', 'pqr.', 'st..']
  },
  {
    name: '初出茅庐',
    mode: 'classic',
    focus: '空间想象 · 认识棋子',
    rows: ['CCW.', 'CCW.', 'HH..', 'Vpq.', 'V.r.']
  },
  {
    name: '兵临城下',
    mode: 'classic',
    focus: '空间想象 · 兵的调度',
    rows: ['CCWp', 'CCWq', 'HHr.', 'Xs.t', 'X...']
  },
  {
    name: '左右布兵',
    mode: 'classic',
    focus: '步骤预判 · 左右开弓',
    rows: ['.CC.', '.CC.', 'WVHX', 'pqrs', 't..u']
  },
  {
    name: '小试锋芒',
    mode: 'classic',
    focus: '步骤预判 · 给曹操让路',
    rows: ['VCCp', 'VCCq', 'HH..', 'JJrs', '...t']
  },
  {
    name: '兵分三路',
    mode: 'classic',
    focus: '步骤预判 · 多线调度',
    rows: ['CCWp', 'CCWq', 'rHHs', 'tV..', 'tV..']
  },
  {
    name: '雨声淅沥',
    mode: 'classic',
    focus: '空间想象 · 小步腾挪',
    rows: ['VCC.', 'VCC.', 'VHHW', 'VpqW', 'r.st']
  },
  {
    name: '水泄不通',
    mode: 'classic',
    focus: '空间想象 · 高难名局',
    rows: ['CCVW', 'CCVW', 'XHHY', 'pqrs', 'tu..']
  },
  {
    name: '过五关斩六将',
    mode: 'classic',
    focus: '步骤预判 · 终极挑战',
    rows: ['VCCW', 'VCCW', 'XHHY', 'pqrs', 't..u']
  },
  {
    name: '横刀立马',
    mode: 'classic',
    focus: '空间想象 · 经典名局',
    rows: ['VCCW', 'VCCW', 'XHHY', 'XpqY', 'r..s']
  }
]

/* ---------------- 路径规划关卡（Rush Hour 式，a 为主车，出口在第 3 行右侧） ---------------- */
export const RUSH_LEVELS: KlotskiLevel[] = [
  { name: '热身 1', mode: 'rush', focus: '观察 · 找到阻挡车', rows: ['......', '..b...', 'a.c...', '.b....', '...d..', '...d..'] },
  { name: '热身 2', mode: 'rush', focus: '观察 · 反向思考', rows: ['......', '..b...', 'a.cb..', '..cb..', '......', '......'] },
  { name: '初级 1', mode: 'rush', focus: '空间想象 · 腾出通道', rows: ['......', '..b.c.', 'a.b.c.', 'dd..c.', '..ee..', '......'] },
  { name: '初级 2', mode: 'rush', focus: '空间想象 · 让三步', rows: ['..b...', '..b...', 'a.bdd.', 'c.....', 'c.ff..', 'c.....'] },
  { name: '初级 3', mode: 'rush', focus: '步骤预判 · 先退后进', rows: ['c.....', 'c..d..', 'a..d..', 'c.ee..', '..e.f.', '..e.f.'] },
  { name: '中级 1', mode: 'rush', focus: '步骤预判 · 多车联动', rows: ['b..cc.', 'b.....', 'a..dd.', 'ee.gg.', 'ff....', 'hh....'] },
  { name: '中级 2', mode: 'rush', focus: '步骤预判 · 隐藏路线', rows: ['c.....', 'c.bb..', 'a.dd..', 'ee.f..', 'gg.f..', 'hh....'] },
  { name: '中级 3', mode: 'rush', focus: '空间想象 · 立体调度', rows: ['bb.c..', '...c..', 'a.dd..', 'e.ff..', 'e.gg..', 'hh....'] },
  { name: '高级 1', mode: 'rush', focus: '步骤预判 · 全盘推演', rows: ['c.....', 'c.bb..', 'a.dd..', 'ee.g..', 'ff.g..', 'hh....'] },
  { name: '高级 2', mode: 'rush', focus: '步骤预判 · 连环让位', rows: ['bb.c..', '...c..', 'a.dd..', 'ee.f..', 'gg.f..', 'h.....'] },
  { name: '高级 3', mode: 'rush', focus: '空间想象 · 巅峰挑战', rows: ['c.....', 'c.bb..', 'a.dd..', 'e.ff..', 'e.gg..', 'hh....'] },
  { name: '大师局', mode: 'rush', focus: '步骤预判 · 大师之路', rows: ['bb.c..', '...c..', 'a.dd..', 'ee.f..', '.g.f..', '.ghh..'] }
]

/* ---------------- 解析与状态 ---------------- */

export function parseClassic(rows: string[]): Piece[] {
  const pieces: Piece[] = []
  const seen = new Set<string>()
  for (let y = 0; y < rows.length; y++) {
    for (let x = 0; x < rows[y].length; x++) {
      const ch = rows[y][x]
      if (ch === '.' || seen.has(ch)) continue
      seen.add(ch)
      // 向右、向下扩展确定尺寸
      let w = 1
      while (x + w < rows[y].length && rows[y][x + w] === ch) w++
      let h = 1
      while (y + h < rows.length && rows[y + h][x] === ch) h++
      pieces.push({ id: ch, w, h, x, y })
    }
  }
  return pieces
}

export function parseRush(rows: string[]): Piece[] {
  const pieces: Piece[] = []
  const seen = new Set<string>()
  for (let y = 0; y < rows.length; y++) {
    for (let x = 0; x < rows[y].length; x++) {
      const ch = rows[y][x]
      if (ch === '.' || seen.has(ch)) continue
      seen.add(ch)
      const horiz = x + 1 < rows[y].length && rows[y][x + 1] === ch
      const w = horiz ? (x + 2 < rows[y].length && rows[y][x + 2] === ch ? 3 : 2) : 1
      const h = horiz ? 1 : (y + 1 < rows.length && rows[y + 1][x] === ch ? 3 : 2)
      pieces.push({ id: ch, w, h, x, y })
    }
  }
  return pieces
}

export function boardSize(mode: 'classic' | 'rush'): { cols: number; rows: number } {
  return mode === 'classic' ? { cols: 4, rows: 5 } : { cols: 6, rows: 6 }
}

export function isWin(mode: 'classic' | 'rush', pieces: Piece[]): boolean {
  if (mode === 'classic') {
    const c = pieces.find((p) => p.id === 'C')
    return !!c && c.y === 3
  }
  const a = pieces.find((p) => p.id === 'a')
  return !!a && a.x >= 4
}

export function clonePieces(pieces: Piece[]): Piece[] {
  return pieces.map((p) => ({ ...p }))
}

/**
 * 状态签名（BFS 去重）：按形状分组，组内位置排序。
 * 同型棋子（如 4 个兵、4 个竖将）互换不影响可解性，归一化后状态空间大幅缩小。
 */
function signature(pieces: Piece[]): string {
  const byShape = new Map<string, string[]>()
  for (const p of pieces) {
    const k = `${p.w}x${p.h}`
    const arr = byShape.get(k) ?? []
    arr.push(`${p.x},${p.y}`)
    byShape.set(k, arr)
  }
  const keys = [...byShape.keys()].sort()
  return keys
    .map((k) => {
      const pos = byShape.get(k)!.sort()
      return k + ':' + pos.join('|')
    })
    .join(';')
}

function occupied(pieces: Piece[], cols: number, rows: number): Int32Array {
  const grid = new Int32Array(cols * rows).fill(-1)
  pieces.forEach((p, i) => {
    for (let dy = 0; dy < p.h; dy++) {
      for (let dx = 0; dx < p.w; dx++) grid[(p.y + dy) * cols + p.x + dx] = i
    }
  })
  return grid
}

const DIRS: Array<[number, number]> = [
  [0, -1],
  [0, 1],
  [-1, 0],
  [1, 0]
]

/** 单步移动枚举（按棋子索引操作，支持同型棋子如多个兵） */
export function movesOf(pieces: Piece[], idx: number, mode: 'classic' | 'rush'): Piece[][] {
  const { cols, rows } = boardSize(mode)
  const grid = occupied(pieces, cols, rows)
  if (idx < 0 || idx >= pieces.length) return []
  const p = pieces[idx]
  const out: Piece[][] = []
  for (const [dx, dy] of DIRS) {
    // 允许一步，以及同方向连续两步（一次滑动两格）
    for (let step = 1; step <= 2; step++) {
      const nx = p.x + dx * step
      const ny = p.y + dy * step
      // 目标区域必须全部为空，且跨过的中间格也为空（step=2 时需中途格可通）
      let ok = nx >= 0 && ny >= 0 && nx + p.w <= cols && ny + p.h <= rows
      if (ok && step === 2) {
        const mx = p.x + dx
        const my = p.y + dy
        for (let sy = 0; sy < p.h && ok; sy++) {
          for (let sx = 0; sx < p.w && ok; sx++) {
            const cell = grid[(my + sy) * cols + mx + sx]
            if (cell !== -1 && cell !== idx) ok = false
          }
        }
      }
      if (ok) {
        for (let sy = 0; sy < p.h && ok; sy++) {
          for (let sx = 0; sx < p.w && ok; sx++) {
            const cell = grid[(ny + sy) * cols + nx + sx]
            if (cell !== -1 && cell !== idx) ok = false
          }
        }
      }
      if (ok) {
        const next = clonePieces(pieces)
        next[idx].x = nx
        next[idx].y = ny
        out.push(next)
      }
    }
  }
  return out
}

/** 玩家滑动一格（按棋子索引） */
export function slideOnce(pieces: Piece[], idx: number, dx: number, dy: number, mode: 'classic' | 'rush'): Piece[] | null {
  const { cols, rows } = boardSize(mode)
  if (idx < 0 || idx >= pieces.length) return null
  const p = pieces[idx]
  const nx = p.x + dx
  const ny = p.y + dy
  if (nx < 0 || ny < 0 || nx + p.w > cols || ny + p.h > rows) return null
  const grid = occupied(pieces, cols, rows)
  for (let sy = 0; sy < p.h; sy++) {
    for (let sx = 0; sx < p.w; sx++) {
      const cell = grid[(ny + sy) * cols + nx + sx]
      if (cell !== -1 && cell !== idx) return null
    }
  }
  const next = clonePieces(pieces)
  next[idx].x = nx
  next[idx].y = ny
  return next
}

/**
 * BFS 最少步求解。返回从当前状态到胜利的移动序列（[棋子索引, dx, dy]），
 * 无解返回 null。
 */
export function solve(mode: 'classic' | 'rush', start: Piece[]): Array<[number, number, number]> | null {
  const initial = clonePieces(start)
  if (isWin(mode, initial)) return []
  const queue: Array<{ pieces: Piece[]; path: Array<[number, number, number]> }> = [
    { pieces: initial, path: [] }
  ]
  const visited = new Set([signature(initial)])
  let head = 0 // 用头指针出队，避免 shift() 的 O(n) 开销
  while (head < queue.length) {
    const cur = queue[head++]
    for (let idx = 0; idx < cur.pieces.length; idx++) {
      const before = cur.pieces[idx]
      for (const next of movesOf(cur.pieces, idx, mode)) {
        const sig = signature(next)
        if (visited.has(sig)) continue
        visited.add(sig)
        const after = next[idx]
        const path = [...cur.path, [idx, after.x - before.x, after.y - before.y] as [number, number, number]]
        if (isWin(mode, next)) return path
        queue.push({ pieces: next, path })
      }
    }
    // 状态空间兜底（不会实际触达）
    if (queue.length > 300000) return null
  }
  return null
}
