/**
 * 拼图内置插画（v2.2 阶段 10）
 * 用 Canvas 代码绘制的四年级上册课本主题卡通插画，规避扫描图版权问题。
 * 每张画在 512×512 画布上，导出为 dataURL 供拼图切块使用。
 */

export interface PuzzleArt {
  name: string
  from: string
  tag: '语文' | '数学'
  draw: (ctx: CanvasRenderingContext2D) => void
}

const S = 512

/* ---------- 通用元素 ---------- */

function sky(ctx: CanvasRenderingContext2D, top: string, bottom: string, h = S) {
  const g = ctx.createLinearGradient(0, 0, 0, h)
  g.addColorStop(0, top)
  g.addColorStop(1, bottom)
  ctx.fillStyle = g
  ctx.fillRect(0, 0, S, h)
}

function sun(ctx: CanvasRenderingContext2D, x: number, y: number, r: number) {
  ctx.fillStyle = '#ffd54f'
  ctx.beginPath()
  ctx.arc(x, y, r, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = 'rgba(255, 213, 79, 0.35)'
  ctx.beginPath()
  ctx.arc(x, y, r * 1.5, 0, Math.PI * 2)
  ctx.fill()
}

function cloud(ctx: CanvasRenderingContext2D, x: number, y: number, s: number) {
  ctx.fillStyle = '#ffffff'
  ctx.beginPath()
  ctx.arc(x, y, 26 * s, 0, Math.PI * 2)
  ctx.arc(x + 30 * s, y - 12 * s, 32 * s, 0, Math.PI * 2)
  ctx.arc(x + 64 * s, y, 24 * s, 0, Math.PI * 2)
  ctx.arc(x + 32 * s, y + 10 * s, 28 * s, 0, Math.PI * 2)
  ctx.fill()
}

function label(ctx: CanvasRenderingContext2D, text: string) {
  ctx.fillStyle = 'rgba(255,255,255,0.92)'
  ctx.fillRect(14, S - 58, ctx.measureText(text).width + 34, 44)
  ctx.fillStyle = '#455a64'
  ctx.font = 'bold 24px sans-serif'
  ctx.fillText(text, 30, S - 26)
}

/* ---------- 语文 · 人教版四上 ---------- */

/** 《观潮》钱塘江大潮 */
function drawTide(ctx: CanvasRenderingContext2D) {
  sky(ctx, '#8ec9f0', '#cfe9fa', 300)
  sun(ctx, 420, 70, 34)
  cloud(ctx, 90, 80, 1)
  cloud(ctx, 280, 120, 0.7)
  // 远处江岸与白塔
  ctx.fillStyle = '#78909c'
  ctx.fillRect(0, 260, S, 40)
  ctx.fillStyle = '#b0bec5'
  ctx.fillRect(30, 210, 26, 52)
  ctx.beginPath()
  ctx.moveTo(22, 210)
  ctx.lineTo(43, 210)
  ctx.lineTo(43, 190)
  ctx.lineTo(32, 172)
  ctx.lineTo(22, 190)
  ctx.closePath()
  ctx.fillStyle = '#eceff1'
  ctx.fill()
  // 江面
  const sea = ctx.createLinearGradient(0, 300, 0, S)
  sea.addColorStop(0, '#4f9bd5')
  sea.addColorStop(1, '#2b6ca8')
  ctx.fillStyle = sea
  ctx.fillRect(0, 300, S, S - 300)
  // 一线潮：横贯江面的白色水墙
  ctx.fillStyle = '#ffffff'
  ctx.beginPath()
  ctx.moveTo(0, 380)
  ctx.quadraticCurveTo(130, 330, 256, 372)
  ctx.quadraticCurveTo(390, 410, 512, 356)
  ctx.lineTo(512, 300)
  ctx.lineTo(0, 300)
  ctx.closePath()
  ctx.fill()
  // 浪花翻卷
  ctx.fillStyle = '#e3f2fd'
  for (let i = 0; i < 9; i++) {
    const x = 30 + i * 56
    ctx.beginPath()
    ctx.arc(x, 400 + (i % 3) * 26, 16 + (i % 2) * 8, Math.PI, 0)
    ctx.fill()
  }
  // 飞溅水珠
  ctx.fillStyle = 'rgba(255,255,255,0.8)'
  for (let i = 0; i < 14; i++) {
    ctx.beginPath()
    ctx.arc(60 + i * 30, 320 + (i % 4) * 14, 4, 0, Math.PI * 2)
    ctx.fill()
  }
  label(ctx, '语文 · 观潮')
}

/** 《爬山虎的脚》 */
function drawIvy(ctx: CanvasRenderingContext2D) {
  // 砖墙背景
  ctx.fillStyle = '#e0c9a6'
  ctx.fillRect(0, 0, S, S)
  ctx.strokeStyle = '#c9ab7c'
  ctx.lineWidth = 4
  for (let row = 0; row < 10; row++) {
    const y = row * 52
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(S, y)
    ctx.stroke()
    const offset = row % 2 ? 0 : 52
    for (let x = offset; x < S; x += 104) {
      ctx.beginPath()
      ctx.moveTo(x, y)
      ctx.lineTo(x, y + 52)
      ctx.stroke()
    }
  }
  // 藤蔓主茎
  ctx.strokeStyle = '#5d7a3a'
  ctx.lineWidth = 12
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(80, S)
  ctx.bezierCurveTo(140, 400, 90, 330, 170, 260)
  ctx.bezierCurveTo(250, 190, 210, 130, 330, 70)
  ctx.stroke()
  // 枝杈
  ctx.lineWidth = 7
  const branches: Array<[number, number, number, number]> = [
    [140, 370, 260, 320],
    [120, 300, 30, 250],
    [180, 250, 300, 220],
    [250, 165, 150, 120],
    [300, 105, 410, 90]
  ]
  for (const [x1, y1, x2, y2] of branches) {
    ctx.beginPath()
    ctx.moveTo(x1, y1)
    ctx.quadraticCurveTo((x1 + x2) / 2, y1 - 30, x2, y2)
    ctx.stroke()
  }
  // 叶子（掌形，脚尖朝下）
  const leaf = (x: number, y: number, s: number, rot: number, c: string) => {
    ctx.save()
    ctx.translate(x, y)
    ctx.rotate(rot)
    ctx.fillStyle = c
    ctx.beginPath()
    ctx.moveTo(0, 0)
    ctx.bezierCurveTo(38 * s, -30 * s, 30 * s, -78 * s, 0, -92 * s)
    ctx.bezierCurveTo(-30 * s, -78 * s, -38 * s, -30 * s, 0, 0)
    ctx.fill()
    ctx.restore()
  }
  const greens = ['#66a13c', '#7cb84e', '#4e8c2e']
  const spots: Array<[number, number, number]> = [
    [262, 330, 1], [40, 262, 0.9], [300, 228, 1], [156, 130, 0.85],
    [408, 96, 0.95], [120, 420, 0.8], [360, 330, 0.7], [70, 160, 0.7]
  ]
  spots.forEach((p, i) => leaf(p[0], p[1], p[2], (i % 4 - 1.5) * 0.5, greens[i % 3]))
  // 脚：茎上长出的细小圆片扒住墙
  ctx.fillStyle = '#8d6e63'
  for (const [x, y] of [[150, 392], [98, 330], [200, 282], [268, 200], [318, 132]]) {
    ctx.beginPath()
    ctx.arc(x as number, y as number, 7, 0, Math.PI * 2)
    ctx.fill()
  }
  label(ctx, '语文 · 爬山虎的脚')
}

/** 《蟋蟀的住宅》 */
function drawCricket(ctx: CanvasRenderingContext2D) {
  sky(ctx, '#3f51b5', '#9fa8da', 190)
  // 月亮与星星
  ctx.fillStyle = '#fff9c4'
  ctx.beginPath()
  ctx.arc(430, 60, 30, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  const stars: Array<[number, number]> = [[80, 40], [180, 90], [300, 50], [360, 110], [230, 30]]
  for (const [x, y] of stars) {
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, Math.PI * 2)
    ctx.fill()
  }
  // 草丛剪影
  ctx.fillStyle = '#33691e'
  ctx.fillRect(0, 150, S, 30)
  ctx.fillStyle = '#43a047'
  for (let x = 0; x < S; x += 26) {
    const h = 30 + ((x * 7) % 40)
    ctx.beginPath()
    ctx.moveTo(x, 185)
    ctx.quadraticCurveTo(x + 10, 185 - h, x + 20, 185)
    ctx.fill()
  }
  // 地面剖面
  ctx.fillStyle = '#8d6e63'
  ctx.fillRect(0, 185, S, S - 185)
  ctx.fillStyle = '#a1887f'
  ctx.fillRect(0, 185, S, 26)
  // 住宅：门口斜下的通道 + 平台
  ctx.fillStyle = '#5d4037'
  ctx.beginPath()
  ctx.moveTo(236, 185)
  ctx.lineTo(276, 185)
  ctx.lineTo(300, 250)
  ctx.lineTo(212, 250)
  ctx.closePath()
  ctx.fill()
  ctx.fillStyle = '#4e342e'
  ctx.beginPath()
  ctx.ellipse(256, 255, 60, 16, 0, 0, Math.PI * 2)
  ctx.fill()
  // 半穴居洞室
  ctx.fillStyle = '#6d4c41'
  ctx.beginPath()
  ctx.ellipse(256, 300, 74, 52, 0, 0, Math.PI * 2)
  ctx.fill()
  // 蟋蟀
  ctx.fillStyle = '#4a2c1a'
  ctx.beginPath()
  ctx.ellipse(300, 238, 26, 14, -0.2, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(324, 230, 9, 0, Math.PI * 2)
  ctx.fill()
  ctx.strokeStyle = '#4a2c1a'
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.moveTo(330, 224)
  ctx.lineTo(352, 200)
  ctx.moveTo(330, 228)
  ctx.lineTo(356, 214)
  ctx.stroke()
  // 触角旁的音符（叫声）
  ctx.fillStyle = '#fff176'
  ctx.font = '26px sans-serif'
  ctx.fillText('♪', 356, 196)
  ctx.fillText('♫', 386, 214)
  label(ctx, '语文 · 蟋蟀的住宅')
}

/* ---------- 数学 · 北师大版四上 ---------- */

/** 《大数的认识》算盘 */
function drawAbacus(ctx: CanvasRenderingContext2D) {
  sky(ctx, '#ffe0b2', '#ffcc80')
  // 木框算盘
  const bx = 36
  const by = 120
  const bw = S - 72
  const bh = 300
  ctx.fillStyle = '#8d6e63'
  ctx.fillRect(bx - 18, by - 18, bw + 36, bh + 36)
  ctx.fillStyle = '#fff3e0'
  ctx.fillRect(bx, by, bw, bh)
  // 横梁与档
  ctx.fillStyle = '#6d4c41'
  ctx.fillRect(bx, by + bh * 0.32, bw, 18)
  const cols = 7
  for (let i = 0; i <= cols; i++) {
    const x = bx + 8 + (i * (bw - 16)) / cols
    ctx.fillRect(x - 3, by, 6, bh)
  }
  // 算珠：上 2 下 5（示例拨出 2024）
  const bead = (x: number, y: number, r: number, c: string) => {
    ctx.fillStyle = c
    ctx.beginPath()
    ctx.ellipse(x, y, r, r * 0.82, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = 'rgba(255,255,255,0.35)'
    ctx.beginPath()
    ctx.ellipse(x - r * 0.3, y - r * 0.3, r * 0.3, r * 0.22, 0, 0, Math.PI * 2)
    ctx.fill()
  }
  // 每档拨的珠数（上珠=5）：表示 2 0 2 4 0 0 0
  const pattern = [2, 0, 2, 4, 0, 0, 0]
  for (let i = 0; i < cols; i++) {
    const x = bx + 8 + ((i + 0.5) * (bw - 16)) / cols
    const up = Math.floor(pattern[i] / 5)
    const down = pattern[i] % 5
    for (let u = 0; u < up; u++) bead(x, by + 40 + u * 40, 17, '#e53935')
    for (let d = 0; d < down; d++) bead(x, by + bh * 0.55 + d * 42, 17, '#1e88e5')
  }
  // 大数卡片
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(96, 60, 320, 46)
  ctx.strokeStyle = '#ffb300'
  ctx.lineWidth = 4
  ctx.strokeRect(96, 60, 320, 46)
  ctx.fillStyle = '#37474f'
  ctx.font = 'bold 34px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('20240000', 256, 94)
  ctx.textAlign = 'left'
  ctx.fillStyle = '#5d4037'
  ctx.font = 'bold 22px sans-serif'
  ctx.fillText('二千零二十四万', 130, 470)
  label(ctx, '数学 · 大数的认识')
}

/** 《线的认识》《角的度量》 */
function drawLines(ctx: CanvasRenderingContext2D) {
  sky(ctx, '#e1f5fe', '#b3e5fc')
  // 线段
  ctx.strokeStyle = '#1565c0'
  ctx.lineWidth = 8
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(60, 110)
  ctx.lineTo(300, 110)
  ctx.stroke()
  ctx.fillStyle = '#1565c0'
  ctx.font = 'bold 24px sans-serif'
  ctx.fillText('线段（两个端点）', 60, 152)
  // 射线
  ctx.beginPath()
  ctx.moveTo(60, 220)
  ctx.lineTo(440, 220)
  ctx.stroke()
  ctx.fillStyle = '#ffb300'
  ctx.beginPath()
  ctx.arc(60, 220, 10, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#37474f'
  ctx.fillText('射线（一个端点）', 60, 262)
  // 直线
  ctx.beginPath()
  ctx.moveTo(30, 320)
  ctx.lineTo(480, 320)
  ctx.stroke()
  ctx.fillStyle = '#37474f'
  ctx.fillText('直线（两端无限）', 60, 362)
  // 角 + 量角器
  ctx.strokeStyle = '#e53935'
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.moveTo(110, 460)
  ctx.lineTo(280, 460)
  ctx.moveTo(110, 460)
  ctx.lineTo(236, 376)
  ctx.stroke()
  // 角度弧
  ctx.strokeStyle = '#e53935'
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.arc(110, 460, 52, -Math.PI / 3.2, 0)
  ctx.stroke()
  ctx.fillStyle = '#e53935'
  ctx.font = 'bold 22px sans-serif'
  ctx.fillText('60°', 172, 440)
  // 半圆量角器
  ctx.strokeStyle = 'rgba(0, 200, 120, 0.8)'
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.arc(110, 460, 120, Math.PI, 0)
  ctx.stroke()
  for (let a = 0; a <= 180; a += 15) {
    const rad = Math.PI - (a * Math.PI) / 180
    ctx.beginPath()
    ctx.moveTo(110 + Math.cos(rad) * 108, 460 - Math.sin(rad) * 108)
    ctx.lineTo(110 + Math.cos(rad) * 120, 460 - Math.sin(rad) * 120)
    ctx.stroke()
  }
  label(ctx, '数学 · 线与角')
}

/** 《买文具》生活场景 */
function drawShop(ctx: CanvasRenderingContext2D) {
  sky(ctx, '#fff8e1', '#ffe9c4', 340)
  // 店铺屋顶与招牌
  ctx.fillStyle = '#ef5350'
  ctx.beginPath()
  ctx.moveTo(30, 130)
  ctx.lineTo(256, 40)
  ctx.lineTo(482, 130)
  ctx.closePath()
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(96, 88, 320, 40)
  ctx.fillStyle = '#d84315'
  ctx.font = 'bold 30px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('文 具 店', 256, 118)
  ctx.textAlign = 'left'
  // 店面
  ctx.fillStyle = '#ffcc80'
  ctx.fillRect(60, 130, 392, 210)
  ctx.fillStyle = '#aed581'
  ctx.fillRect(0, 340, S, S - 340)
  // 货架商品：铅笔 / 笔记本 / 书包
  // 铅笔
  ctx.fillStyle = '#fbc02d'
  ctx.fillRect(100, 180, 130, 22)
  ctx.fillStyle = '#e53935'
  ctx.beginPath()
  ctx.moveTo(230, 180)
  ctx.lineTo(254, 191)
  ctx.lineTo(230, 202)
  ctx.closePath()
  ctx.fill()
  ctx.fillStyle = '#5d4037'
  ctx.fillRect(100, 176, 16, 30)
  ctx.fillStyle = '#37474f'
  ctx.font = 'bold 22px sans-serif'
  ctx.fillText('2元', 130, 232)
  // 笔记本
  ctx.fillStyle = '#42a5f5'
  ctx.fillRect(300, 168, 110, 76)
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(310, 178, 90, 56)
  ctx.strokeStyle = '#90caf9'
  ctx.lineWidth = 3
  for (let i = 0; i < 4; i++) {
    ctx.beginPath()
    ctx.moveTo(316, 190 + i * 13)
    ctx.lineTo(394, 190 + i * 13)
    ctx.stroke()
  }
  ctx.fillStyle = '#37474f'
  ctx.fillText('6元', 330, 274)
  // 书包
  ctx.fillStyle = '#ab47bc'
  ctx.beginPath()
  ctx.roundRect ? ctx.roundRect(150, 300, 120, 110, 18) : ctx.rect(150, 300, 120, 110)
  ctx.fill()
  ctx.fillStyle = '#ce93d8'
  ctx.fillRect(178, 286, 64, 30)
  ctx.fillStyle = '#f3e5f5'
  ctx.fillRect(170, 340, 80, 40)
  ctx.fillStyle = '#37474f'
  ctx.fillText('20元', 168, 480)
  // 价签总额问题
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(310, 380, 168, 64)
  ctx.strokeStyle = '#ef5350'
  ctx.lineWidth = 4
  ctx.strokeRect(310, 380, 168, 64)
  ctx.fillStyle = '#d84315'
  ctx.font = 'bold 24px sans-serif'
  ctx.fillText('买1支铅笔和', 324, 406)
  ctx.fillText('1个书包 = ?元', 324, 434)
  label(ctx, '数学 · 买文具')
}

export const PUZZLE_ARTS: PuzzleArt[] = [
  { name: '钱塘江大潮', from: '语文《观潮》', tag: '语文', draw: drawTide },
  { name: '爬山虎', from: '语文《爬山虎的脚》', tag: '语文', draw: drawIvy },
  { name: '蟋蟀的住宅', from: '语文《蟋蟀的住宅》', tag: '语文', draw: drawCricket },
  { name: '算盘与大数', from: '数学《大数的认识》', tag: '数学', draw: drawAbacus },
  { name: '线与角', from: '数学《线的认识》', tag: '数学', draw: drawLines },
  { name: '买文具', from: '数学《买文具》', tag: '数学', draw: drawShop }
]

/** 把插画渲染为指定尺寸的正方形 dataURL（512 源图缩放） */
export function renderArt(art: PuzzleArt, size: number): string {
  const cv = document.createElement('canvas')
  cv.width = size
  cv.height = size
  const ctx = cv.getContext('2d')!
  ctx.scale(size / S, size / S)
  art.draw(ctx)
  return cv.toDataURL('image/png')
}
