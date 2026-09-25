<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="按名称搜索" clearable style="width: 220px" @keyup.enter="load(1)" @clear="load(1)" />
      <el-select v-model="enabledFilter" placeholder="上架状态" clearable style="width: 140px" @change="load(1)">
        <el-option label="已上架" :value="true" />
        <el-option label="已下架" :value="false" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button type="success" @click="openCreate">新增奖品</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="图片" width="90">
        <template #default="{ row }">
          <el-image v-if="row.image" :src="resolveImg(row.image)" fit="contain" style="width: 60px; height: 60px" />
          <span v-else>🎁</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="pointsCost" label="所需积分" width="100" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="上架" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="toggleEnabled(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
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

    <el-dialog v-model="editOpen" :title="form.id ? '编辑奖品' : '新增奖品'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-area">
            <el-image v-if="form.image" :src="resolveImg(form.image)" fit="contain" class="preview" />
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept="image/png,image/jpeg,image/gif,image/webp"
              :on-change="onFilePick"
            >
              <el-button>选择图片上传</el-button>
            </el-upload>
            <el-input v-model="form.image" placeholder="或直接填图片地址" />
          </div>
        </el-form-item>
        <el-form-item label="所需积分" required>
          <el-input-number v-model="form.pointsCost" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="库存" required>
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { Prize } from '@/types/api'

const list = ref<Prize[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const enabledFilter = ref<boolean | undefined>()
const loading = ref(false)
const saving = ref(false)

const editOpen = ref(false)
const form = reactive({
  id: 0, name: '', image: '', pointsCost: 50, stock: 1, enabled: true, description: '', sortOrder: 0
})

function resolveImg(image: string): string {
  if (/^https?:\/\//.test(image)) return image
  return image.startsWith('/') ? image : `/${image}`
}

async function load(targetPage?: number) {
  if (targetPage) page.value = targetPage
  loading.value = true
  try {
    const data = await adminApi.prizeList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      enabled: enabledFilter.value
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: 0, name: '', image: '', pointsCost: 50, stock: 1, enabled: true, description: '', sortOrder: list.value.length })
  editOpen.value = true
}

function openEdit(row: Prize) {
  Object.assign(form, {
    id: row.id, name: row.name, image: row.image, pointsCost: row.pointsCost,
    stock: row.stock, enabled: row.enabled, description: row.description, sortOrder: row.sortOrder
  })
  editOpen.value = true
}

async function onFilePick(file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  const res = await adminApi.upload(raw)
  form.image = res.url
  ElMessage.success('图片已上传')
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写奖品名称')
    return
  }
  saving.value = true
  try {
    const data = {
      name: form.name.trim(),
      image: form.image,
      pointsCost: form.pointsCost,
      stock: form.stock,
      enabled: form.enabled,
      description: form.description,
      sortOrder: form.sortOrder
    }
    if (form.id) await adminApi.prizeUpdate(form.id, data)
    else await adminApi.prizeCreate(data)
    ElMessage.success('已保存')
    editOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row: Prize) {
  await adminApi.prizeUpdate(row.id, {
    name: row.name, image: row.image, pointsCost: row.pointsCost, stock: row.stock,
    enabled: !row.enabled, description: row.description, sortOrder: row.sortOrder
  })
  ElMessage.success(row.enabled ? '已下架' : '已上架')
  await load()
}

function del(row: Prize) {
  ElMessageBox.confirm(`删除奖品「${row.name}」？有兑换记录的奖品无法删除，可改为下架。`, '删除奖品', { type: 'warning' })
    .then(async () => {
      await adminApi.prizeDelete(row.id)
      ElMessage.success('已删除')
      await load()
    })
    .catch(() => undefined)
}

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

.upload-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.preview {
  width: 64px;
  height: 64px;
  border: 1px solid #e8eaf1;
  border-radius: 6px;
}
</style>
