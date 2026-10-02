<template>
  <nav class="tabbar">
    <div class="tabbar-inner">
      <RouterLink to="/map" class="tab" :class="{ active: current === 'map' }">
        <span class="tab-ico">🏠</span>
        <span class="tab-txt">闯关</span>
      </RouterLink>
      <RouterLink to="/shop" class="tab" :class="{ active: current === 'shop' }">
        <span class="tab-ico">🎁</span>
        <span class="tab-txt">商城</span>
      </RouterLink>
      <RouterLink to="/me" class="tab" :class="{ active: current === 'me' }">
        <span class="tab-ico">👤</span>
        <span class="tab-txt">我的</span>
      </RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
defineProps<{ current: 'map' | 'shop' | 'me' }>()
</script>

<style scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  justify-content: center;
  padding: 0 12px calc(10px + env(safe-area-inset-bottom));
  background: linear-gradient(to top, rgba(19, 31, 36, 0.98) 62%, rgba(19, 31, 36, 0));
  pointer-events: none;
}

.tabbar-inner {
  pointer-events: auto;
  width: 100%;
  max-width: 460px;
  display: flex;
  background: #1f2c34;
  border: 1px solid var(--border);
  border-radius: 22px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
  padding: 6px;
}

.tab {
  flex: 1;
  min-height: 58px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--text-sub);
  text-decoration: none;
  font-size: 13px;
  font-weight: 700;
  transition: background 0.15s ease, color 0.15s ease;
}

.tab-ico {
  font-size: 23px;
  line-height: 1;
  transition: transform 0.15s ease;
}

.tab.active {
  color: #7be338;
  background: var(--primary-soft);
}

.tab.active .tab-ico {
  transform: translateY(-1px) scale(1.12);
}
</style>
