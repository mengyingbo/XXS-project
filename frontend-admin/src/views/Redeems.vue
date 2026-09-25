<template>
  <el-card>
    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="load(1)">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="DELIVERED">已发放</el-radio-button>
        <el-radio-button value="REJECTED">已拒绝</el-radio-button>
      </el-radio-group>
      <el-select v-model="childFilter" placeholder="按孩子筛选" clearable filterable style="width: 200px" @change="load(1)">
        <el-option v-for="c in children" :key="c.id" :label="c.nickname" :value="c.id" />
      </el-select>
      <el-button @click="load()">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="单号" width="80" />
      <el-table-column prop="childNickname" label="孩子" width="120" />
      <el-table-column prop="prizeName" label="奖品" min-width="140" />
      <el-table-column prop="pointsCost" label="消耗积分" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="REDEEM_STATUS_TAG[row.status]">{{ REDEEM_STATUS_LABEL[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="处理时间" width="160">
        <template #default="{ row }">{{ fmtDateTime(row.handledAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            size="small" type="success" @click="handle(row, 'APPROVE')"
          >通过</el-button>
          <el-button
            v-if="row.status === 'PENDING' || row.status === 'APPROVED'"
            size="small" type="danger" @click="handle(row, 'REJECT')"
          >拒绝</el-button>
          <el-button
            v-if="row.status === 'PENDING' || row.status === 'APPROVED'"
            size="small" type="primary" @click="handle(row, 'DELIVER')"
          >标记已发放</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="page"
      v-model:page-size="size"
      :page-sizes="[10, 20, 50]"
      @current-change="load()"
      @size-change="load(1)"
    />

    <!-- 审核备注 -->
    <el-dialog v-model="handleOpen" :title="ACTION_TITLE[action]" width="420px">
      <p style="margin-top: 0">
        「{{ current?.childNickname }}」兑换「{{ current?.prizeName }}」（{{ current?.pointsCost }} 积分）
      </p>
      <p v-if="action === 'REJECT'" class="warn-text">拒绝会自动退回孩子积分与奖品库存。</p>
      <el-input v-model="remark" type="textarea" :rows="2" maxlength="255" placeholder="备注（可选）" />
      <template #footer>
        <el-button @click="handleOpen = false">取消</el-button>
        <el-button :type="action === 'REJECT' ? 'danger' : 'primary'" :loading="saving" @click="submitHandle">
          确认{{ ACTION_TITLE[action] }}
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { REDEEM_STATUS_LABEL, REDEEM_STATUS_TAG, fmtDateTime } from '@/utils/format'
import type { Child, RedeemOrder } from '@/types/api'

const ACTION_TITLE: Record<string, string> = {
  APPROVE: '通过',
  REJECT: '拒绝',
  DELIVER: '标记已发放'
}

const list = ref<RedeemOrder[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const statusFilter = ref('')
const childFilter = ref<number | undefined>()
const children = ref<Child[]>([])
const loading = ref(false)
const saving = ref(false)

const handleOpen = ref(false)
const current = ref<RedeemOrder | null>(null)
const action = ref<'APPROVE' | 'REJECT' | 'DELIVER'>('APPROVE')
const remark = ref('')

async function load(targetPage?: number) {
  if (targetPage) page.value = targetPage
  loading.value = true
  try {
    const data = await adminApi.redeemList({
      page: page.value,
      size: size.value,
      status: statusFilter.value || undefined,
      childId: childFilter.value
    })
    list.value = data.records as RedeemOrder[]
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handle(row: RedeemOrder, act: 'APPROVE' | 'REJECT' | 'DELIVER') {
  current.value = row
  action.value = act
  remark.value = ''
  handleOpen.value = true
}

async function submitHandle() {
  if (!current.value) return
  saving.value = true
  try {
    await adminApi.redeemHandle(current.value.id, { action: action.value, remark: remark.value || undefined })
    ElMessage.success(`已${ACTION_TITLE[action.value]}`)
    handleOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
  try {
    children.value = (await adminApi.childList({ page: 1, size: 100 })).records
  } catch {
    // 筛选项加载失败不影响列表
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.warn-text {
  color: #e6a23c;
}
</style>
