<template>
  <div class="order">
    <!-- 已排好的顺序 -->
    <div class="zone-title">
      我的顺序（点卡片可撤回）
      <button v-if="modelValue.length > 0" class="reset" @click="emit('update:modelValue', [])">
        重置
      </button>
    </div>
    <div class="picked">
      <button
        v-for="(idx, pos) in modelValue"
        :key="`p-${idx}`"
        class="item picked-item"
        @click="removeAt(pos)"
      >
        <span class="pos">{{ pos + 1 }}</span>
        <span class="item-text">{{ options[idx] }}</span>
      </button>
      <div v-if="modelValue.length === 0" class="pick-hint">还没有排列，从下面点选吧</div>
    </div>

    <!-- 待选项（options 已是打乱顺序，提交其原始下标） -->
    <div v-if="modelValue.length < options.length" class="zone-title">点一点，按正确顺序排列</div>
    <div class="pool">
      <button
        v-for="(opt, idx) in options"
        v-show="!modelValue.includes(idx)"
        :key="`o-${idx}`"
        class="item pool-item"
        @click="pick(idx)"
      >
        <span class="item-text">{{ opt }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ modelValue: number[]; options: string[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: number[]] }>()

function pick(idx: number) {
  emit('update:modelValue', [...props.modelValue, idx])
}

function removeAt(pos: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== pos))
}
</script>

<style scoped>
.order {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.zone-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-sub);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reset {
  color: var(--primary);
  font-size: 14px;
  min-height: 36px;
  padding: 0 10px;
}

.picked {
  min-height: 60px;
  border: 2.5px dashed var(--primary);
  border-radius: 16px;
  background: var(--primary-soft);
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pick-hint {
  color: var(--text-sub);
  font-size: 15px;
  text-align: center;
  padding: 12px 0;
}

.pool {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 10px 14px;
  border-radius: 14px;
  text-align: left;
  font-size: 17px;
  border: 2.5px solid var(--border);
  background: #fbfcff;
  line-height: 1.4;
}

.pool-item:active {
  transform: scale(0.99);
}

.picked-item {
  border-color: var(--primary);
  background: #fff;
}

.pos {
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.item-text {
  flex: 1;
}
</style>
