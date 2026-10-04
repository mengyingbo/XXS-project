<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="按电影名搜索" clearable style="width: 220px" @keyup.enter="load(1)" @clear="load(1)" />
      <el-select v-model="enabledFilter" placeholder="上架状态" clearable style="width: 140px" @change="load(1)">
        <el-option label="已上架" :value="true" />
        <el-option label="已下架" :value="false" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button type="success" @click="openCreate">新增电影</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-image v-if="row.cover" :src="resolveImg(row.cover)" fit="cover" style="width: 48px; height: 64px" />
          <span v-else>🎬</span>
        </template>
      </el-table-column>
      <el-table-column prop="no" label="序号" width="70" />
      <el-table-column prop="name" label="电影名" min-width="140" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column label="片长" width="90">
        <template #default="{ row }">{{ row.duration }}分钟</template>
      </el-table-column>
      <el-table-column label="评分" width="80">
        <template #default="{ row }">★ {{ row.rating }}</template>
      </el-table-column>
      <el-table-column prop="theme" label="主题" width="120" show-overflow-tooltip />
      <el-table-column label="上架" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认观看" width="90">
        <template #default="{ row }">
          <el-tag :type="row.watched ? 'success' : 'warning'" size="small">{{ row.watched ? '已看' : '未看' }}</el-tag>
        </template>
      </el-table-column>
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

    <el-dialog v-model="editOpen" :title="form.id ? '编辑电影' : '新增电影'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="电影名称" required>
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="显示序号">
          <el-input-number v-model="form.no" :min="0" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.type" maxlength="50" placeholder="如：成长/音乐" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="片长(分钟)">
              <el-input-number v-model="form.duration" :min="0" :step="5" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="豆瓣评分">
              <el-input-number v-model="form.rating" :min="0" :max="10" :step="0.1" :precision="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="推荐主题">
          <el-input v-model="form.theme" maxlength="100" placeholder="如：包容与赏识" />
        </el-form-item>
        <el-form-item label="适龄看点">
          <el-input v-model="form.note" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="upload-area">
            <el-image v-if="form.cover" :src="resolveImg(form.cover)" fit="cover" class="preview" />
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept="image/png,image/jpeg,image/gif,image/webp"
              :on-change="onFilePick"
            >
              <el-button>选择图片上传</el-button>
            </el-upload>
            <el-input v-model="form.cover" placeholder="或直接填封面地址" />
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="默认观看">
              <el-switch v-model="form.watched" active-text="已看" inactive-text="未看" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否上架">
              <el-switch v-model="form.enabled" active-text="上架" inactive-text="下架" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排序权重">
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
import type { Movie } from '@/types/api'

const list = ref<Movie[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const enabledFilter = ref<boolean | undefined>()
const loading = ref(false)
const saving = ref(false)

const editOpen = ref(false)
const form = reactive({
  id: 0, no: 0, name: '', type: '', duration: 90, rating: 8.0,
  theme: '', note: '', cover: '', watched: false, enabled: true, sortOrder: 0
})

function resolveImg(image: string): string {
  if (/^https?:\/\//.test(image)) return image
  return image.startsWith('/') ? image : `/${image}`
}

async function load(targetPage?: number) {
  if (targetPage) page.value = targetPage
  loading.value = true
  try {
    const data = await adminApi.movieList({
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
  Object.assign(form, {
    id: 0, no: list.value.length + 1, name: '', type: '', duration: 90, rating: 8.0,
    theme: '', note: '', cover: '', watched: false, enabled: true, sortOrder: list.value.length
  })
  editOpen.value = true
}

function openEdit(row: Movie) {
  Object.assign(form, {
    id: row.id, no: row.no, name: row.name, type: row.type, duration: row.duration,
    rating: row.rating, theme: row.theme, note: row.note, cover: row.cover,
    watched: row.watched, enabled: row.enabled, sortOrder: row.sortOrder
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
  form.cover = res.url
  ElMessage.success('封面已上传')
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写电影名称')
    return
  }
  saving.value = true
  try {
    const data = {
      no: form.no,
      name: form.name.trim(),
      type: form.type,
      duration: form.duration,
      rating: form.rating,
      theme: form.theme,
      note: form.note,
      cover: form.cover,
      watched: form.watched,
      enabled: form.enabled,
      sortOrder: form.sortOrder
    }
    if (form.id) await adminApi.movieUpdate(form.id, data)
    else await adminApi.movieCreate(data)
    ElMessage.success('已保存')
    editOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function del(row: Movie) {
  ElMessageBox.confirm(`删除电影「${row.name}」？删除后 H5 端将不再显示该电影。`, '删除电影', { type: 'warning' })
    .then(async () => {
      await adminApi.movieDelete(row.id)
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
  width: 48px;
  height: 64px;
  border: 1px solid #e8eaf1;
  border-radius: 6px;
  object-fit: cover;
}
</style>
