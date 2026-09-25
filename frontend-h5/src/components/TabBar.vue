<template>
  <nav class="tabbar">
    <RouterLink to="/map" class="tab" :class="{ active: current === 'map' }">
      <span class="tab-ico">🗺️</span>
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
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid var(--border);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab {
  flex: 1;
  max-width: 320px;
  min-height: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--text-sub);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
}

.tab-ico {
  font-size: 24px;
  line-height: 1;
}

.tab.active {
  color: var(--primary);
}

.tab.active .tab-ico {
  transform: translateY(-1px) scale(1.08);
}
</style>
