<template>
  <div class="opts">
    <button
      v-for="(opt, i) in options"
      :key="i"
      class="opt"
      :class="{ on: modelValue === i }"
      @click="choose(i)"
    >
      <span class="letter">{{ OPTION_LETTERS[i] }}</span>
      <span class="text">{{ opt }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { OPTION_LETTERS } from '@/utils/answer'

defineProps<{ modelValue: number | null; options: string[] }>()
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
  min-height: 56px;
  padding: 12px 16px;
  border-radius: 16px;
  border: 2.5px solid var(--border);
  background: #fbfcff;
  text-align: left;
  font-size: 18px;
  line-height: 1.45;
  transition: border-color 0.12s ease, background 0.12s ease, transform 0.08s ease;
}

.opt:active {
  transform: scale(0.99);
}

.letter {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #eef1f9;
  color: var(--text-sub);
  font-weight: 800;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.opt.on {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.opt.on .letter {
  background: var(--primary);
  color: #fff;
}
</style>
