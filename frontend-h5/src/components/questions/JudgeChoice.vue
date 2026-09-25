<template>
  <div class="judge-opts">
    <button
      class="judge-btn j-true"
      :class="{ on: modelValue === true }"
      @click="emit('update:modelValue', true)"
    >
      <span class="j-ico">✓</span>
      <span>正确</span>
    </button>
    <button
      class="judge-btn j-false"
      :class="{ on: modelValue === false }"
      @click="emit('update:modelValue', false)"
    >
      <span class="j-ico">✗</span>
      <span>错误</span>
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ modelValue: boolean | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<style scoped>
.judge-opts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.judge-btn {
  min-height: 110px;
  border-radius: 18px;
  border: 2.5px solid var(--border);
  background: #fbfcff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 22px;
  font-weight: 700;
  transition: transform 0.08s ease, border-color 0.12s ease, background 0.12s ease;
}

.j-ico {
  font-size: 40px;
  line-height: 1;
}

.judge-btn:active {
  transform: scale(0.97);
}

.j-true.on {
  border-color: var(--success);
  background: var(--success-soft);
  color: #1d9c5a;
}

.j-false.on {
  border-color: var(--danger);
  background: var(--danger-soft);
  color: #d84040;
}
</style>
