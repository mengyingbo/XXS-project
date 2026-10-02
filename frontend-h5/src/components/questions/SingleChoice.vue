<template>
  <div class="opts">
    <button
      v-for="(opt, i) in options"
      :key="i"
      class="opt"
      :class="{
        on: !reveal && modelValue === i,
        'is-correct': reveal && i === correctIndex,
        'is-wrong': reveal && modelValue === i && i !== correctIndex
      }"
      :disabled="disabled"
      @click="choose(i)"
    >
      <span class="letter">
        <span v-if="reveal && i === correctIndex">✓</span>
        <span v-else-if="reveal && modelValue === i && i !== correctIndex">✕</span>
        <span v-else>{{ OPTION_LETTERS[i] }}</span>
      </span>
      <span class="text">{{ opt }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { OPTION_LETTERS } from '@/utils/answer'

withDefaults(
  defineProps<{
    modelValue: number | null
    options: string[]
    /** 反馈态：禁止改答案 */
    disabled?: boolean
    /** 反馈态：揭示对错（正确项标绿、错选项标红） */
    reveal?: boolean
    /** reveal 时的正确选项下标 */
    correctIndex?: number | null
  }>(),
  { disabled: false, reveal: false, correctIndex: null }
)
const emit = defineEmits<{ 'update:modelValue': [value: number] }>()

function choose(i: number) {
  emit('update:modelValue', i)
}
</script>

<style scoped>
.opts {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.opt {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  min-height: 56px;
  padding: 12px 16px;
  border-radius: 16px;
  border: 2.5px solid var(--border);
  background: var(--card-2);
  color: var(--text-main);
  text-align: left;
  font-size: 18px;
  line-height: 1.45;
  transition: border-color 0.12s ease, background 0.12s ease, transform 0.08s ease;
}

.opt:not(:disabled):active {
  transform: scale(0.99);
}

.opt:disabled {
  cursor: default;
}

.letter {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #3a4a55;
  color: #cdd6db;
  font-weight: 800;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 作答中：信息蓝选中态 */
.opt.on {
  border-color: var(--info-border);
  background: var(--info-soft);
}

.opt.on .letter {
  background: var(--info);
  color: #fff;
}

/* 反馈态：正确项绿、错选项红 */
.opt.is-correct {
  border-color: var(--success);
  background: var(--success-soft);
}

.opt.is-correct .letter {
  background: var(--success);
  color: #fff;
}

.opt.is-wrong {
  border-color: var(--danger);
  background: var(--danger-soft);
}

.opt.is-wrong .letter {
  background: var(--danger);
  color: #fff;
}
</style>
