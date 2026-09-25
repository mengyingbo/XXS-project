<template>
  <span class="avatar" :class="`avatar--${size}`" :style="bgStyle">
    {{ avatar || '🧒' }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    avatar?: string
    size?: 'sm' | 'md' | 'lg'
  }>(),
  { avatar: '', size: 'md' }
)

// 按 emoji 码点稳定取一个柔和底色
const palettes = ['#ffe8ec', '#e4f1ff', '#e7f9ee', '#fff3da', '#f0e8ff', '#e0f7f5']
const bgStyle = computed(() => {
  const code = (props.avatar || '🧒').codePointAt(0) ?? 0
  return { background: palettes[code % palettes.length] }
})
</script>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex: none;
  user-select: none;
}

.avatar--sm {
  width: 40px;
  height: 40px;
  font-size: 22px;
}

.avatar--md {
  width: 56px;
  height: 56px;
  font-size: 30px;
}

.avatar--lg {
  width: 84px;
  height: 84px;
  font-size: 46px;
}
</style>
