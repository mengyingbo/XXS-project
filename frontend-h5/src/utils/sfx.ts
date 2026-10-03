/**
 * 多邻国风格答题音效（Web Audio 实时合成）
 *
 * - 零外部资源：不引入音频文件，无网络请求、无版权问题
 * - 音色参考 Duolingo 的标志性反馈：
 *   · 答对：清脆上扬的「叮-咚」双音，连对越多音组越高（combo 升调）
 *   · 答错：低沉柔和的「呃-哦」双音，不刺耳、不吓人
 *   · 通关：欢快的大调琶音 + 高音和弦收尾
 *   · 未通关：温柔的两音鼓励
 *   · 点击：轻微的按键「嗒」声
 * - iOS 兼容：AudioContext 惰性创建，首次播放都发生在用户手势内（点「检查」等按钮），
 *   天然满足移动端自动播放策略
 * - 静音开关持久化在 localStorage，可在「我的」页切换
 */

const STORAGE_KEY = 'xxs_sfx_enabled'

let ctx: AudioContext | null = null
let master: GainNode | null = null

/* ---------- 静音开关 ---------- */

function enabled(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) !== '0'
  } catch {
    return true
  }
}

/** 读取音效开关（默认开启） */
export function sfxEnabled(): boolean {
  return enabled()
}

/** 设置音效开关；打开时顺便预热音频上下文 */
export function setSfxEnabled(on: boolean) {
  try {
    localStorage.setItem(STORAGE_KEY, on ? '1' : '0')
  } catch {
    /* 隐私模式等存储异常时忽略，仅本次会话生效 */
  }
  if (on) ensureCtx()
}

/* ---------- 音频上下文 ---------- */

/**
 * 惰性创建 AudioContext。
 * 首次调用必须发生在用户手势内（本项目所有音效都由按钮点击触发，天然满足），
 * 否则 iOS Safari 会保持 suspended 状态无法出声。
 */
function ensureCtx(): AudioContext | null {
  if (!enabled()) return null
  if (typeof window === 'undefined') return null
  try {
    if (!ctx) {
      const AC: typeof AudioContext | undefined =
        window.AudioContext ??
        (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
      if (!AC) return null
      ctx = new AC()
      master = ctx.createGain()
      master.gain.value = 0.42
      master.connect(ctx.destination)
    }
    if (ctx.state === 'suspended') void ctx.resume()
    return ctx
  } catch {
    return null
  }
}

/* ---------- 音色合成 ---------- */

/**
 * 单音符：马林巴/钢片琴音色
 * 正弦基频 + 4 倍频泛音，快 attack + 指数衰减，模拟琴键敲击的明亮质感
 */
function note(freq: number, delay: number, dur: number, vol: number) {
  const c = ensureCtx()
  if (!c || !master) return
  const t0 = c.currentTime + delay

  // 基频
  const gain = c.createGain()
  gain.gain.setValueAtTime(0, t0)
  gain.gain.linearRampToValueAtTime(vol, t0 + 0.012)
  gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur)
  gain.connect(master)

  const osc = c.createOscillator()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(freq, t0)
  osc.connect(gain)
  osc.start(t0)
  osc.stop(t0 + dur + 0.05)

  // 高频泛音：衰减更快，只给音头一点「亮」度
  const harmGain = c.createGain()
  harmGain.gain.setValueAtTime(0, t0)
  harmGain.gain.linearRampToValueAtTime(vol * 0.28, t0 + 0.008)
  harmGain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur * 0.4)
  harmGain.connect(master)

  const harm = c.createOscillator()
  harm.type = 'sine'
  harm.frequency.setValueAtTime(freq * 4, t0)
  harm.connect(harmGain)
  harm.start(t0)
  harm.stop(t0 + dur * 0.4 + 0.05)
}

/** 频率移调：semitones 为半音数（正数升、负数降） */
function shift(freq: number, semitones: number): number {
  return freq * Math.pow(2, semitones / 12)
}

/* ---------- 场景音效 ---------- */

/**
 * 答对：上扬「叮-咚」双音（C5 → G5）
 * 连对越多整体音组越高：第 5 连起逐级 +1 个半音，封顶 +7（约纯五度），
 * 营造「越连越嗨」的多邻国式成就感
 */
export function sfxCorrect(combo = 1) {
  if (!ensureCtx()) return
  const up = Math.min(Math.max(combo - 4, 0), 7)
  const base = shift(523.25, up) // C5
  note(base, 0, 0.28, 0.3)
  note(base * 1.5, 0.105, 0.42, 0.34) // 上方纯五度，第二声更亮更响
}

/** 答错：低沉柔和的「呃-哦」下行双音，圆润不吓人 */
export function sfxWrong() {
  if (!ensureCtx()) return
  note(207.65, 0, 0.26, 0.3) // G#3
  note(155.56, 0.15, 0.34, 0.3) // D#3，下行纯四度
}

/** 通关：欢快大调琶音（C5-E5-G5-C6）+ 高音和弦收尾，配合星星弹出动画 */
export function sfxComplete() {
  if (!ensureCtx()) return
  const seq = [523.25, 659.25, 783.99, 1046.5] // C5 E5 G5 C6
  seq.forEach((f, i) => note(f, i * 0.1, 0.32, 0.26))
  // C 大调高音和弦收尾
  note(1046.5, 0.42, 0.55, 0.2)
  note(1318.51, 0.42, 0.55, 0.16)
  note(1567.98, 0.42, 0.55, 0.14)
}

/** 未通关：温柔的两音鼓励，音量低、节奏慢，不打击孩子 */
export function sfxEncourage() {
  if (!ensureCtx()) return
  note(392.0, 0, 0.3, 0.22) // G4
  note(329.63, 0.2, 0.45, 0.22) // E4
}

/** 轻微按键「嗒」声（点「检查」等），短促下滑的三角波 */
export function sfxTap() {
  const c = ensureCtx()
  if (!c || !master) return
  const t0 = c.currentTime

  const gain = c.createGain()
  gain.gain.setValueAtTime(0.18, t0)
  gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.07)
  gain.connect(master)

  const osc = c.createOscillator()
  osc.type = 'triangle'
  osc.frequency.setValueAtTime(620, t0)
  osc.frequency.exponentialRampToValueAtTime(240, t0 + 0.07)
  osc.connect(gain)
  osc.start(t0)
  osc.stop(t0 + 0.08)
}
