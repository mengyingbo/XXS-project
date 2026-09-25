<template>
  <input
    :value="modelValue"
    class="blank-input text-input"
    type="text"
    autocomplete="off"
    placeholder="在这里输入你的答案"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>

<script setup lang="ts">
defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<style scoped>
.blank-input {
  font-size: 20px;
  min-height: 56px;
}
</style>
