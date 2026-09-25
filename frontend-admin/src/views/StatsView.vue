<template>
  <div v-loading="loading">
    <el-card class="filter-card">
      <div class="toolbar">
        <el-select v-model="childId" placeholder="全部孩子" clearable style="width: 200px" @change="load">
          <el-option v-for="c in children" :key="c.id" :label="c.nickname" :value="c.id" />
        </el-select>
        <el-radio-group v-model="days" @change="load">
          <el-radio-button :value="7">最近 7 天</el-radio-button>
          <el-radio-button :value="30">最近 30 天</el-radio-button>
          <el-radio-button :value="0">全部时间</el-radio-button>
        </el-radio-group>
        <el-button @click="load">刷新</el-button>
      </div>
    </el-card>

    <!-- 薄弱知识点（F-AD-10 核心产出） -->
    <el-card style="margin-bottom: 16px">
      <template #header>⚠️ 薄弱知识点（作答量 ≥ 3 且正确率最低的前 5 个）</template>
      <el-empty v-if="(data?.weakKnowledgePoints ?? []).length === 0" description="暂无足够作答数据" :image-size="60" />
      <div v-else class="weak-list">
        <div v-for="w in data!.weakKnowledgePoints" :key="w.knowledgePoint" class="weak-item">
          <span class="weak-name">{{ w.knowledgePoint }}</span>
          <span class="weak-meta">{{ w.correct }}/{{ w.total }} 题</span>
          <el-progress
            class="weak-bar"
            :percentage="Number(w.accuracy)"
            :color="Number(w.accuracy) < 60 ? '#f56c6c' : Number(w.accuracy) < 80 ? '#e6a23c' : '#67c23a'"
          />
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>按孩子统计</template>
          <el-table :data="data?.byChild ?? []" size="small" max-height="420">
            <el-table-column prop="nickname" label="孩子" min-width="90" />
            <el-table-column prop="total" label="作答" width="70" />
            <el-table-column label="正确率" width="90">
              <template #default="{ row }">{{ row.accuracy }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>按课文统计</template>
          <el-table :data="data?.byLesson ?? []" size="small" max-height="420">
            <el-table-column label="课文" min-width="110" show-overflow-tooltip>
              <template #default="{ row }">{{ row.unitTitle }} · {{ row.lessonTitle }}</template>
            </el-table-column>
            <el-table-column prop="total" label="作答" width="60" />
            <el-table-column label="正确率" width="80">
              <template #default="{ row }">{{ row.accuracy }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>按知识点统计</template>
          <el-table :data="data?.byKnowledgePoint ?? []" size="small" max-height="420">
            <el-table-column prop="knowledgePoint" label="知识点" min-width="110" show-overflow-tooltip />
            <el-table-column prop="total" label="作答" width="60" />
            <el-table-column label="正确率" width="80">
              <template #default="{ row }">{{ row.accuracy }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import type { Child, StatsData } from '@/types/api'

const loading = ref(true)
const data = ref<StatsData | null>(null)
const childId = ref<number | undefined>()
const days = ref(0)
const children = ref<Child[]>([])

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.stats({
      childId: childId.value,
      days: days.value || undefined
    })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    children.value = (await adminApi.childList({ page: 1, size: 100 })).records
  } catch {
    // 筛选项加载失败不影响统计
  }
  await load()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.weak-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weak-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.weak-name {
  width: 180px;
  font-weight: 600;
}

.weak-meta {
  width: 80px;
  color: #909399;
  font-size: 13px;
}

.weak-bar {
  flex: 1;
}
</style>
