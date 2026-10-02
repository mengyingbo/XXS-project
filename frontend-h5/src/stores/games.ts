import { defineStore } from 'pinia'

/**
 * 游戏乐园进度（v2.1）：设备级 localStorage，不与孩子档案关联。
 * 结构：每个游戏 关卡key -> 星级(1~3)；sudoku 记录各规格完成局数与最佳用时(秒)。
 */
const KEY = 'xxs_games_progress_v1'

export type GameKey = 'klotski' | 'puzzle' | 'snake' | 'sudoku' | 'idiom'

interface SudokuProgress {
  played: number
  best: Record<string, number>
}

interface SnakeProgress {
  /** 历史最高分（只增不减） */
  best: number
}

interface GamesProgress {
  klotski: Record<string, number>
  puzzle: Record<string, number>
  idiom: Record<string, number>
  snake: SnakeProgress
  sudoku: SudokuProgress
}

function empty(): GamesProgress {
  return { klotski: {}, puzzle: {}, idiom: {}, snake: { best: 0 }, sudoku: { played: 0, best: {} } }
}

function load(): GamesProgress {
  const raw = localStorage.getItem(KEY)
  if (!raw) return empty()
  try {
    const p = JSON.parse(raw) as Partial<GamesProgress>
    const base = empty()
    return {
      klotski: p.klotski ?? base.klotski,
      puzzle: p.puzzle ?? base.puzzle,
      idiom: p.idiom ?? base.idiom,
      snake: p.snake ?? base.snake,
      sudoku: p.sudoku ?? base.sudoku
    }
  } catch {
    return empty()
  }
}

export const useGamesStore = defineStore('games', {
  state: () => ({ data: load() }),
  getters: {
    /** 某游戏获得的星总数（sudoku 无星级概念，返回 0） */
    stars(state): (g: GameKey) => number {
      return (g: GameKey) => {
        if (g === 'sudoku') return 0
        return Object.values(state.data[g] as Record<string, number>).reduce((s, v) => s + v, 0)
      }
    },
    /** 某游戏已通关数 */
    cleared(state): (g: GameKey) => number {
      return (g: GameKey) => Object.keys(state.data[g] as Record<string, number>).length
    },
    /** 关卡是否解锁：第 1 关始终解锁，前一关有星级记录即解锁 */
    isUnlocked(state): (g: GameKey, levelKey: string, index: number) => boolean {
      return (g: GameKey, _levelKey: string, index: number) => {
        if (index <= 0) return true
        if (g === 'sudoku') return true
        const map = state.data[g] as Record<string, number>
        return Object.keys(map).length >= index
      }
    }
  },
  actions: {
    /** 记录关卡结果：星级只升不降。返回是否为新通关 */
    complete(g: GameKey, levelKey: string, stars: number) {
      const map = this.data[g] as Record<string, number>
      const isNew = !(levelKey in map)
      map[levelKey] = Math.max(map[levelKey] ?? 0, Math.min(3, stars))
      this.save()
      return isNew
    },
    recordSudoku(size: string, seconds: number) {
      this.data.sudoku.played += 1
      const best = this.data.sudoku.best
      if (!(size in best) || seconds < best[size]) best[size] = seconds
      this.save()
    },
    /** 贪吃蛇：记录最高分（只增不减） */
    recordSnakeBest(score: number) {
      if (score > this.data.snake.best) {
        this.data.snake.best = score
        this.save()
      }
    },
    reset() {
      this.data = empty()
      this.save()
    },
    save() {
      try {
        localStorage.setItem(KEY, JSON.stringify(this.data))
      } catch {
        /* 隐私模式等存储失败时静默降级为内存态 */
      }
    }
  }
})
