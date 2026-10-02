/**
 * snakeCore.js 的类型声明（实现为纯 ES5，不含类型信息）
 */
export type SnakeDir = 'up' | 'down' | 'left' | 'right'
export type SnakeState = 'waiting' | 'running' | 'paused' | 'over'

export interface SnakeHooks {
  /** 得分变化 */
  onScore?: (score: number) => void
  /** 游戏结束（含最终得分） */
  onOver?: (score: number) => void
  /** 触发提速（每吃 5 个食物） */
  onSpeedUp?: () => void
  /** 状态机变化：waiting/running/paused/over */
  onStateChange?: (state: SnakeState) => void
}

export interface SnakeGame {
  /** 开始新局（重置蛇、分数、速度） */
  start(): void
  /** 暂停/继续切换 */
  togglePause(): void
  /** 设置移动方向（防反向、下一次移动生效） */
  setDirection(d: SnakeDir): void
  getState(): SnakeState
  getScore(): number
  /** 清理定时器与 window 事件监听 */
  destroy(): void
}

declare function createGame(options: {
  canvas: HTMLCanvasElement
  hooks?: SnakeHooks
}): SnakeGame

export default createGame
