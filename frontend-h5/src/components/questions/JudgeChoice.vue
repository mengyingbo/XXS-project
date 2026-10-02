<template>
  <div class="judge-opts">
    <button
      class="judge-btn"
      :class="{
        on: !reveal && modelValue === true,
        'is-correct': reveal && correctValue === true,
        'is-wrong': reveal && modelValue === true && correctValue !== true
      }"
      :disabled="disabled"
      @click="emit('update:modelValue', true)"
    >
      <span class="j-ico">✓</span>
      <span>正确</span>
    </button>
    <button
      class="judge-btn"
      :class="{
        on: !reveal && modelValue === false,
        'is-correct': reveal && correctValue === false,
        'is-wrong': reveal && modelValue === false && correctValue !== false
      }"
      :disabled="disabled"
      @click="emit('update:modelValue', false)"
    >
      <span class="j-ico">✗</span>
      <span>错误</span>
    </button>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: boolean | null
    /** 反馈态：禁止改答案 */
    disabled?: boolean
    /** 反馈态：揭示对错 */
    reveal?: boolean
    /** reveal 时的正确判断值 */
    correctValue?: boolean | null
  }>(),
  { disabled: false, reveal: false, correctValue: null }
)
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
  background: var(--card-2);
  color: var(--text-main);
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

.judge-btn:not(:disabled):active {
  transform: scale(0.97);
}

.judge-btn:disabled {
  cursor: default;
}

/* 作答中：信息蓝选中态（不提前暗示对错） */
.judge-btn.on {
  border-color: var(--info-border);
  background: var(--info-soft);
  color: var(--info-border);
}

/* 反馈态 */
.judge-btn.is-correct {
  border-color: var(--success);
  background: var(--success-soft);
  color: #7be338;
}

.judge-btn.is-wrong {
  border-color: var(--danger);
  background: var(--danger-soft);
  color: #ff7a7a;
}
</style>
