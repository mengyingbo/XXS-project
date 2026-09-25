<template>
  <span class="star-row" :class="`star-row--${size}`">
    <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= stars }">★</span>
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    stars: number
    size?: 'sm' | 'md' | 'lg'
  }>(),
  { size: 'md' }
)
</script>

<style scoped>
.star-row {
  display: inline-flex;
  gap: 2px;
  line-height: 1;
  white-space: nowrap;
}

.star {
  color: #d7dcea;
  transition: color 0.2s ease;
}

.star.on {
  color: var(--star);
  text-shadow: 0 1px 3px rgba(255, 197, 49, 0.45);
}

.star-row--sm .star {
  font-size: 15px;
}

.star-row--md .star {
  font-size: 20px;
}

.star-row--lg .star {
  font-size: 46px;
}

.star-row--lg .star {
  margin: 0 4px;
}
</style>
