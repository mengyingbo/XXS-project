<template>
  <div class="toast-wrap">
    <div
      v-for="t in toasts"
      :key="t.id"
      class="toast"
      :class="`toast-${t.type}`"
    >
      {{ t.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { toasts } from '@/composables/useToast'
</script>
