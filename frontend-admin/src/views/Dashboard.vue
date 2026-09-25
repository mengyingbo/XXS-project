<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in mainCards" :key="card.label">
        <el-card class="stat-card">
          <div class="stat-num" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>今日概况</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="今日活跃孩子">{{ data?.todayActiveChildren ?? '—' }} 人</el-descriptions-item>
            <el-descriptions-item label="今日答题数">{{ data?.todayAnswers ?? '—' }} 题</el-descriptions-item>
            <el-descriptions-item label="今日完成关卡">{{ data?.todaySessions ?? '—' }} 局</el-descriptions-item>
            <el-descriptions-item label="当前可用积分总额">{{ data?.totalAvailablePoints ?? '—' }} 分</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            兑换申请
            <el-button text type="primary" style="float: right" @click="$router.push('/redeems')">去处理</el-button>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="待审核">
              <el-tag type="warning">{{ data?.redeemCounts?.PENDING ?? 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已通过（待发放）">
              <el-tag type="primary">{{ data?.redeemCounts?.APPROVED ?? 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已发放">
              <el-tag type="success">{{ data?.redeemCounts?.DELIVERED ?? 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已拒绝">
              <el-tag type="danger">{{ data?.redeemCounts?.REJECTED ?? 0 }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card style="margin-top: 16px">
          <template #header>题库规模</template>
          <el-space wrap :size="24">
            <el-tag size="large">单元 {{ data?.catalog?.unitCount ?? 0 }}</el-tag>
            <el-tag size="large" type="success">课文/园地 {{ data?.catalog?.lessonCount ?? 0 }}</el-tag>
            <el-tag size="large" type="warning">关卡 {{ data?.catalog?.levelCount ?? 0 }}</el-tag>
            <el-tag size="large" type="danger">题目 {{ data?.catalog?.questionCount ?? 0 }}</el-tag>
          </el-space>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import type { DashboardData } from '@/types/api'

const loading = ref(true)
const data = ref<DashboardData | null>(null)

const mainCards = computed(() => [
  { label: '孩子档案', value: data.value?.childCount ?? '—', color: '#409eff' },
  { label: '今日活跃孩子', value: data.value?.todayActiveChildren ?? '—', color: '#67c23a' },
  { label: '今日答题数', value: data.value?.todayAnswers ?? '—', color: '#e6a23c' },
  { label: '累计发放积分', value: data.value?.totalIssuedPoints ?? '—', color: '#f56c6c' }
])

onMounted(async () => {
  try {
    data.value = await adminApi.dashboard()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-num {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.3;
}

.stat-label {
  color: #909399;
  margin-top: 4px;
}
</style>
