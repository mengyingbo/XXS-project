<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="head">
        <span>规则配置（修改后立即生效，影响孩子端积分与每日限额）</span>
        <el-button type="primary" :loading="saving" @click="save">保存全部修改</el-button>
      </div>
    </template>

    <el-table :data="rows" border>
      <el-table-column prop="configKey" label="配置项" width="240" />
      <el-table-column label="当前值" width="200">
        <template #default="{ row }">
          <el-switch
            v-if="isBool(row)"
            v-model="editValues[row.configKey]"
            active-value="true"
            inactive-value="false"
          />
          <el-input-number
            v-else-if="isNum(row)"
            v-model="editValues[row.configKey]"
            :min="numMin(row)"
            :max="numMax(row)"
            controls-position="right"
            style="width: 160px"
          />
          <el-input v-else v-model="editValues[row.configKey]" />
        </template>
      </el-table-column>
      <el-table-column prop="defaultValue" label="默认值" width="120" />
      <el-table-column prop="remark" label="说明" min-width="300" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" text type="primary" :disabled="row.configValue === row.defaultValue" @click="editValues[row.configKey] = row.defaultValue">
            恢复默认
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { ConfigRow } from '@/types/api'

const loading = ref(true)
const saving = ref(false)
const rows = ref<ConfigRow[]>([])
const editValues = reactive<Record<string, any>>({})

/** 布尔型配置（下拉/开关呈现），其余按数字处理 */
const BOOL_KEYS = new Set(['show_analysis_immediately', 'combo_enabled'])
const isBool = (row: ConfigRow) => BOOL_KEYS.has(row.configKey)
const isNum = (row: ConfigRow) => !isBool(row)
const numMin = (row: ConfigRow) => (row.configKey.startsWith('points') || row.configKey === 'combo_bonus' ? 0 : 1)
const numMax = (row: ConfigRow) => (row.configKey.includes('rate') ? 100 : 999)

async function load() {
  loading.value = true
  try {
    rows.value = await adminApi.configList()
    for (const row of rows.value) {
      editValues[row.configKey] = isBool(row) ? row.configValue : Number(row.configValue)
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  const values: Record<string, string> = {}
  for (const row of rows.value) {
    const v = editValues[row.configKey]
    if (v === undefined || v === null || String(v) === '') {
      ElMessage.warning(`配置项 ${row.configKey} 不能为空`)
      return
    }
    values[row.configKey] = String(v)
  }
  saving.value = true
  try {
    await adminApi.configUpdate(values)
    ElMessage.success('配置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
