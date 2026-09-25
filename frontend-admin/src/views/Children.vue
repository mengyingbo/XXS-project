<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按昵称搜索"
        clearable
        style="width: 240px"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button type="primary" @click="load(1)">搜索</el-button>
      <el-button type="success" @click="openCreate">新建孩子档案</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="昵称" min-width="120">
        <template #default="{ row }">
          <span style="margin-right: 6px">{{ row.avatar }}</span>{{ row.nickname }}
        </template>
      </el-table-column>
      <el-table-column label="当前积分" width="100">
        <template #default="{ row }">
          <b :style="{ color: '#e6a23c' }">{{ row.totalPoints }}</b>
        </template>
      </el-table-column>
      <el-table-column prop="totalEarned" label="累计获得" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="PIN锁定至" width="160">
        <template #default="{ row }">
          {{ row.lockedUntil ? fmtDateTime(row.lockedUntil) : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ fmtDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openResetPin(row)">重置PIN</el-button>
          <el-button size="small" type="primary" @click="openAdjust(row)">调分</el-button>
          <el-dropdown trigger="click" style="margin-left: 10px" @command="(cmd: string) => onMore(cmd, row)">
            <el-button size="small" type="info">
              更多<el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="`resetOne:${row.id}`">重置某关进度</el-dropdown-item>
                <el-dropdown-item :command="`resetAll:${row.id}`">重置全部进度</el-dropdown-item>
                <el-dropdown-item :command="`delete:${row.id}`" divided>删除档案</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="editOpen" :title="editForm.id ? '编辑档案' : '新建孩子档案'" width="440px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="昵称" required>
          <el-input v-model="editForm.nickname" maxlength="10" show-word-limit />
        </el-form-item>
        <el-form-item label="头像">
          <el-select v-model="editForm.avatar" style="width: 100%">
            <el-option v-for="e in AVATARS" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editForm.id" label="PIN" required>
          <el-input v-model="editForm.pin" placeholder="4 位数字" maxlength="4" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置 PIN -->
    <el-dialog v-model="pinOpen" title="重置 PIN" width="380px">
      <p style="margin-top: 0">为「{{ current?.nickname }}」设置新的 4 位 PIN：</p>
      <el-input v-model="newPin" maxlength="4" placeholder="4 位数字" size="large" />
      <template #footer>
        <el-button @click="pinOpen = false">取消</el-button>
        <el-button type="warning" :loading="saving" @click="savePin">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 调分 -->
    <el-dialog v-model="adjustOpen" title="手动调整积分" width="420px">
      <el-form label-width="90px">
        <el-form-item label="孩子">{{ current?.nickname }}（当前 {{ current?.totalPoints }} 分）</el-form-item>
        <el-form-item label="变动值" required>
          <el-input-number v-model="adjustForm.delta" :step="10" />
          <span class="tip">正数增加、负数扣减，不能为 0</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" maxlength="255" placeholder="默认“家长手动调整”，会写入积分流水" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 重置某关进度 -->
    <el-dialog v-model="resetOpen" title="重置某关进度" width="480px">
      <p style="margin-top: 0">
        为「{{ current?.nickname }}」选择要重置的关卡：该关回到已解锁、星级与尝试次数清零，可重新获得通关奖励；不影响其后关卡状态。
      </p>
      <el-cascader
        v-model="resetCascade"
        :options="cascadeOptions"
        placeholder="单元 / 课文 / 关卡"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="resetOpen = false">取消</el-button>
        <el-button type="danger" :disabled="!resetLevelId" :loading="saving" @click="saveResetOne">确认重置</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import { fmtDateTime } from '@/utils/format'
import type { Child, Level, Lesson, Unit } from '@/types/api'

const AVATARS = ['🐭', '🐰', '🦊', '🐻', '🐼', '🐯', '🦁', '🐮', '🐨', '🐸', '🦄', '🐙']

const list = ref<Child[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)

const editOpen = ref(false)
const pinOpen = ref(false)
const adjustOpen = ref(false)
const resetOpen = ref(false)

const editForm = reactive({ id: 0, nickname: '', avatar: '🐭', pin: '', enabled: true })
const adjustForm = reactive({ delta: 10, remark: '' })
const newPin = ref('')
const current = ref<Child | null>(null)

// 级联选择（单元→课文→关卡）
const cascadeOptions = ref<CascaderNode[]>([])
const resetCascade = ref<number[]>([])
const resetLevelId = ref<number | null>(null)

interface CascaderNode {
  value: number
  label: string
  children?: CascaderNode[]
}

async function load(targetPage?: number) {
  if (targetPage) page.value = targetPage
  loading.value = true
  try {
    const data = await adminApi.childList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(editForm, { id: 0, nickname: '', avatar: '🐭', pin: '', enabled: true })
  editOpen.value = true
}

function openEdit(row: Child) {
  Object.assign(editForm, { id: row.id, nickname: row.nickname, avatar: row.avatar || '🐭', pin: '', enabled: row.enabled })
  editOpen.value = true
}

async function saveEdit() {
  if (!editForm.nickname.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  if (!editForm.id && !/^\d{4}$/.test(editForm.pin)) {
    ElMessage.warning('PIN 必须是 4 位数字')
    return
  }
  saving.value = true
  try {
    if (editForm.id) {
      await adminApi.childUpdate(editForm.id, {
        nickname: editForm.nickname.trim(),
        avatar: editForm.avatar,
        enabled: editForm.enabled
      })
      ElMessage.success('已保存')
    } else {
      await adminApi.childCreate({
        nickname: editForm.nickname.trim(),
        avatar: editForm.avatar,
        pin: editForm.pin,
        enabled: editForm.enabled
      })
      ElMessage.success('档案已创建')
    }
    editOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openResetPin(row: Child) {
  current.value = row
  newPin.value = ''
  pinOpen.value = true
}

async function savePin() {
  if (!current.value) return
  if (!/^\d{4}$/.test(newPin.value)) {
    ElMessage.warning('PIN 必须是 4 位数字')
    return
  }
  saving.value = true
  try {
    await adminApi.childResetPin(current.value.id, newPin.value)
    ElMessage.success('PIN 已重置（同时解除锁定）')
    pinOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openAdjust(row: Child) {
  current.value = row
  adjustForm.delta = 10
  adjustForm.remark = ''
  adjustOpen.value = true
}

async function saveAdjust() {
  if (!current.value) return
  if (adjustForm.delta === 0) {
    ElMessage.warning('变动值不能为 0')
    return
  }
  saving.value = true
  try {
    const res = await adminApi.childAdjustPoints(current.value.id, {
      delta: adjustForm.delta,
      remark: adjustForm.remark || undefined
    })
    ElMessage.success(`调整成功，当前积分 ${res.points}`)
    adjustOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function loadCascade() {
  const [units, lessons, levels] = await Promise.all([
    adminApi.unitList(),
    adminApi.lessonList(),
    adminApi.levelList()
  ])
  const lessonByUnit = new Map<number, Lesson[]>()
  for (const l of lessons) {
    if (!lessonByUnit.has(l.unitId)) lessonByUnit.set(l.unitId, [])
    lessonByUnit.get(l.unitId)!.push(l)
  }
  const levelByLesson = new Map<number, Level[]>()
  for (const l of levels) {
    if (!levelByLesson.has(l.lessonId)) levelByLesson.set(l.lessonId, [])
    levelByLesson.get(l.lessonId)!.push(l)
  }
  cascadeOptions.value = (units as Unit[]).map((u) => ({
    value: u.id,
    label: `第${u.unitNo}单元 ${u.title}`,
    children: (lessonByUnit.get(u.id) ?? []).map((ls) => ({
      value: ls.id,
      label: ls.title,
      children: (levelByLesson.get(ls.id) ?? []).map((lv) => ({
        value: lv.id,
        label: `${lv.levelNo}关 ${lv.name || ''}`
      }))
    }))
  }))
}

async function openResetOne(row: Child) {
  current.value = row
  resetCascade.value = []
  resetLevelId.value = null
  if (cascadeOptions.value.length === 0) {
    await loadCascade()
  }
  resetOpen.value = true
}

async function saveResetOne() {
  if (!current.value || !resetLevelId.value) return
  saving.value = true
  try {
    await adminApi.childResetLevelProgress(current.value.id, resetLevelId.value)
    ElMessage.success('该关进度已重置')
    resetOpen.value = false
  } finally {
    saving.value = false
  }
}

function onMore(cmd: string, row: Child) {
  const [action, idStr] = cmd.split(':')
  const id = Number(idStr)
  if (action === 'resetOne') {
    void openResetOne(row)
  } else if (action === 'resetAll') {
    ElMessageBox.confirm(`确定重置「${row.nickname}」的全部闯关进度吗？所有关卡回到初始状态（仅第 1 关解锁）。`, '重置全部进度', {
      type: 'warning',
      confirmButtonText: '确认重置',
      cancelButtonText: '取消'
    })
      .then(async () => {
        await adminApi.childResetAllProgress(id)
        ElMessage.success('全部进度已重置')
      })
      .catch(() => undefined)
  } else if (action === 'delete') {
    ElMessageBox.confirm(`确定删除「${row.nickname}」的档案吗？有答题/兑换记录的档案无法删除，可改为停用。`, '删除档案', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
      .then(async () => {
        await adminApi.childDelete(id)
        ElMessage.success('已删除')
        await load()
      })
      .catch(() => undefined)
  }
}

// 级联选中最后一级时同步关卡 id
watch(resetCascade, (val) => {
  resetLevelId.value = val && val.length === 3 ? val[2] : null
})

onMounted(() => load())
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.tip {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}
</style>
