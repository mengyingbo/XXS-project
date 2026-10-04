<template>
  <div class="page page-no-tab home-page">
    <!-- 顶部标题区 -->
    <header class="hero">
      <div class="hero-emoji">🎡🌈</div>
      <h1 class="hero-title">儿童成长乐园</h1>
      <p class="hero-sub">学知识 · 看电影 · 玩游戏，快乐每一天！</p>
    </header>

    <!-- 功能模块卡片 -->
    <div class="module-grid">
      <!-- 知识闯关 -->
      <button class="module-card mc-quiz" @click="go('/profiles')">
        <div class="mc-icon-wrap">
          <span class="mc-icon">🎒</span>
          <span class="mc-sparkle">✨</span>
        </div>
        <div class="mc-body">
          <span class="mc-title">知识闯关</span>
          <span class="mc-desc">语文 · 数学 · 英语，闯关升级学知识</span>
        </div>
        <span class="mc-arrow">›</span>
      </button>

      <!-- 电影片库 -->
      <button class="module-card mc-movie" @click="go('/movies')">
        <div class="mc-icon-wrap">
          <span class="mc-icon">🎬</span>
          <span class="mc-badge-new">NEW</span>
        </div>
        <div class="mc-body">
          <span class="mc-title">电影片库</span>
          <span class="mc-desc">50 部高分好电影，边看边学大道理</span>
        </div>
        <span class="mc-arrow">›</span>
      </button>

      <!-- 游戏乐园 -->
      <button class="module-card mc-games" @click="go('/games')">
        <div class="mc-icon-wrap">
          <span class="mc-icon">🎮</span>
        </div>
        <div class="mc-body">
          <span class="mc-title">游戏乐园</span>
          <span class="mc-desc">华容道 · 拼图 · 贪吃蛇 · 数独 · 成语接龙</span>
        </div>
        <span class="mc-arrow">›</span>
      </button>
    </div>

    <!-- 底部装饰 -->
    <div class="footer-deco">
      <span class="fd-emoji">🌟 ⭐ 🌟</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function go(path: string) {
  router.push(path)
}
</script>

<style scoped>
.home-page {
  max-width: 680px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  justify-content: center;
}

.hero {
  text-align: center;
  padding: 20px 0 36px;
}

.hero-emoji {
  font-size: 52px;
  animation: bounce-slow 2.4s ease-in-out infinite;
}

.hero-title {
  font-size: 34px;
  font-weight: 800;
  margin-top: 8px;
  background: linear-gradient(135deg, #58cc02, #1cb0f6, #ffc800);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-sub {
  color: var(--text-sub);
  margin-top: 8px;
  font-size: 16px;
  letter-spacing: 1px;
}

/* ---- 模块卡片 ---- */
.module-grid {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.module-card {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 22px 20px;
  border-radius: var(--radius-lg);
  text-align: left;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
  position: relative;
  overflow: hidden;
}

.module-card:active {
  transform: scale(0.97);
}

/* 知识闯关 - 绿色系 */
.mc-quiz {
  background: linear-gradient(135deg, #1a3a1a, #20361f);
  border: 2px solid #2d5a2d;
  box-shadow: 0 8px 24px rgba(88, 204, 2, 0.15);
}

/* 电影片库 - 蓝紫系 */
.mc-movie {
  background: linear-gradient(135deg, #1a2540, #2a1f4d);
  border: 2px solid #3a3570;
  box-shadow: 0 8px 24px rgba(28, 176, 246, 0.15);
}

/* 游戏乐园 - 橙色系 */
.mc-games {
  background: linear-gradient(135deg, #3a2a18, #2d2210);
  border: 2px solid #5a4020;
  box-shadow: 0 8px 24px rgba(255, 150, 0, 0.15);
}

.mc-icon-wrap {
  position: relative;
  flex: none;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mc-quiz .mc-icon-wrap {
  background: rgba(88, 204, 2, 0.15);
}

.mc-movie .mc-icon-wrap {
  background: rgba(28, 176, 246, 0.15);
}

.mc-games .mc-icon-wrap {
  background: rgba(255, 150, 0, 0.15);
}

.mc-icon {
  font-size: 34px;
}

.mc-sparkle {
  position: absolute;
  top: -2px;
  right: -2px;
  font-size: 16px;
  animation: sparkle 1.6s ease-in-out infinite;
}

.mc-badge-new {
  position: absolute;
  top: -6px;
  right: -6px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 8px;
  letter-spacing: 0.5px;
}

.mc-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.mc-title {
  font-size: 22px;
  font-weight: 800;
}

.mc-desc {
  font-size: 14px;
  color: var(--text-sub);
  line-height: 1.5;
}

.mc-arrow {
  font-size: 30px;
  color: var(--text-sub);
  flex: none;
}

/* ---- 底部装饰 ---- */
.footer-deco {
  text-align: center;
  padding: 36px 0 8px;
}

.fd-emoji {
  font-size: 22px;
  letter-spacing: 6px;
  opacity: 0.6;
  animation: float 3s ease-in-out infinite;
}

/* ---- 动画 ---- */
@keyframes bounce-slow {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes sparkle {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

/* ---- 横屏矮屏 ---- */
@media (max-height: 500px) and (orientation: landscape) {
  .home-page {
    justify-content: flex-start;
  }
  .hero {
    padding: 10px 0 16px;
  }
  .hero-emoji {
    font-size: 38px;
  }
  .hero-title {
    font-size: 26px;
  }
  .module-grid {
    gap: 12px;
  }
  .module-card {
    padding: 14px 16px;
  }
}
</style>
