<template>
  <input
    :value="modelValue"
    class="blank-input text-input"
    :class="{ 'st-correct': state === 'correct', 'st-wrong': state === 'wrong' }"
    type="text"
    autocomplete="off"
    placeholder="在这里输入你的答案"
    :disabled="disabled"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: string
    /** 反馈态：禁止修改 */
    disabled?: boolean
    /** 反馈态：本题对错，决定边框色 */
    state?: 'idle' | 'correct' | 'wrong'
  }>(),
  { disabled: false, state: 'idle' }
)
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<style scoped>
.blank-input {
  font-size: 20px;
  min-height: 56px;
}

.blank-input:disabled {
  opacity: 1;
  cursor: default;
}

.blank-input.st-correct {
  border-color: var(--success);
  background: var(--success-soft);
}

.blank-input.st-wrong {
  border-color: var(--danger);
  background: var(--danger-soft);
}
</style>
