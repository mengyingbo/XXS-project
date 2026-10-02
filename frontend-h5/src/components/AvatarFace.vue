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

// 按 emoji 码点稳定取一个深色柔和底色（深色主题，保留色相区分）
const palettes = ['#3a2630', '#24344a', '#22382a', '#3d3320', '#2e2942', '#1f3a3a']
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
