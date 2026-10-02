/**
 * 数独生成器（v2.1 游戏乐园）
 * 支持四宫格(2×2)、六宫格(2×3)、九宫格(3×3)：回溯生成终盘 → 对称挖洞 → 唯一解验证
 */
export interface SudokuPuzzle {
  size: number // 每行列数：4 / 6 / 9
  boxW: number
  boxH: number
  /** puzzles[r][c]，0 为空格 */
  givens: number[][]
  solution: number[][]
}

function boxDims(size: number): { boxW: number; boxH: number } {
  if (size === 4) return { boxW: 2, boxH: 2 }
  if (size === 6) return { boxW: 3, boxH: 2 } // 3 列 × 2 行的宫
  return { boxW: 3, boxH: 3 }
}

function makeRng(): () => number {
  let s = Date.now() % 2147483647
  if (s <= 0) s += 2147483646
  return () => {
    s = (s * 16807) % 2147483647
    return (s - 1) / 2147483646
  }
}

function shuffle<T>(arr: T[], rng: () => number): T[] {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

function ok(board: number[][], size: number, boxW: number, boxH: number, r: number, c: number, v: number): boolean {
  for (let i = 0; i < size; i++) {
    if (board[r][i] === v || board[i][c] === v) return false
  }
  const r0 = Math.floor(r / boxH) * boxH
  const c0 = Math.floor(c / boxW) * boxW
  for (let dy = 0; dy < boxH; dy++) {
    for (let dx = 0; dx < boxW; dx++) {
      if (board[r0 + dy][c0 + dx] === v) return false
    }
  }
  return true
}

/** 生成完整终盘 */
function fullBoard(size: number, boxW: number, boxH: number, rng: () => number): number[][] {
  const board: number[][] = Array.from({ length: size }, () => new Array(size).fill(0))
  const fill = (pos: number): boolean => {
    if (pos === size * size) return true
    const r = Math.floor(pos / size)
    const c = pos % size
    for (const v of shuffle(Array.from({ length: size }, (_, i) => i + 1), rng)) {
      if (ok(board, size, boxW, boxH, r, c, v)) {
        board[r][c] = v
        if (fill(pos + 1)) return true
        board[r][c] = 0
      }
    }
    return false
  }
  fill(0)
  return board
}

/** 解的数量计数（最多数到 cap 即停，用于唯一性验证） */
function countSolutions(board: number[][], size: number, boxW: number, boxH: number, cap: number): number {
  let count = 0
  const go = (): void => {
    if (count >= cap) return
    let best = -1
    let bestCands: number[] = []
    for (let r = 0; r < size && count < cap; r++) {
      for (let c = 0; c < size && count < cap; c++) {
        if (board[r][c] !== 0) continue
        const cands: number[] = []
        for (let v = 1; v <= size; v++) {
          if (ok(board, size, boxW, boxH, r, c, v)) cands.push(v)
        }
        if (best === -1 || cands.length < bestCands.length) {
          best = r * size + c
          bestCands = cands
          if (cands.length === 0) return
          if (cands.length === 1) break
        }
      }
    }
    if (best === -1) {
      count++
      return
    }
    const r = Math.floor(best / size)
    const c = best % size
    for (const v of bestCands) {
      board[r][c] = v
      go()
      board[r][c] = 0
      if (count >= cap) return
    }
  }
  go()
  return count
}

/** 生成谜题：挖洞保持唯一解 */
export function generate(size: number): SudokuPuzzle {
  const { boxW, boxH } = boxDims(size)
  const rng = makeRng()
  const solution = fullBoard(size, boxW, boxH, rng)
  const puzzle = solution.map((row) => [...row])
  const holes = size === 4 ? 7 : size === 6 ? 12 : 46
  const positions = shuffle(
    Array.from({ length: size * size }, (_, i) => i),
    rng
  )
  let removed = 0
  for (const pos of positions) {
    if (removed >= holes) break
    const r = Math.floor(pos / size)
    const c = pos % size
    const saved = puzzle[r][c]
    if (saved === 0) continue
    puzzle[r][c] = 0
    if (countSolutions(puzzle.map((row) => [...row]), size, boxW, boxH, 2) === 1) {
      removed++
    } else {
      puzzle[r][c] = saved
    }
  }
  return { size, boxW, boxH, givens: puzzle, solution }
}

/** 用户棋盘是否存在冲突（返回冲突格集合 key "r-c"） */
export function conflicts(board: number[][], size: number, boxW: number, boxH: number): Set<string> {
  const bad = new Set<string>()
  const rowSeen: Array<Map<number, number[]>> = Array.from({ length: size }, () => new Map())
  const colSeen: Array<Map<number, number[]>> = Array.from({ length: size }, () => new Map())
  const boxSeen: Array<Map<number, number[]>> = Array.from({ length: size }, () => new Map())
  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      const v = board[r][c]
      if (!v) continue
      const b = Math.floor(r / boxH) * (size / boxW) + Math.floor(c / boxW)
      rowSeen[r].set(v, [...(rowSeen[r].get(v) ?? []), r * size + c])
      colSeen[c].set(v, [...(colSeen[c].get(v) ?? []), r * size + c])
      boxSeen[b].set(v, [...(boxSeen[b].get(v) ?? []), r * size + c])
    }
  }
  const mark = (m: Map<number, number[]>) => {
    for (const cells of m.values()) {
      if (cells.length > 1) {
        for (const pos of cells) bad.add(`${Math.floor(pos / size)}-${pos % size}`)
      }
    }
  }
  rowSeen.forEach(mark)
  colSeen.forEach(mark)
  boxSeen.forEach(mark)
  return bad
}
