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
        <template #default="{ row }">{{ row.rating > 0 ? '★ ' + row.rating : '-' }}</template>
      </el-table-column>
      <el-table-column prop="theme" label="主题" width="120" show-overflow-tooltip />
      <el-table-column prop="note" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="上架" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
        </template>
      </el-table-column>
      <el-table-column label="默认观看" width="90">
        <template #default="{ row }">
          <el-tag :type="row.watched ? 'success' : 'warning'" size="small">{{ row.watched ? '已看' : '未看' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="ArrowUp" circle title="上移" @click="move(row, 'up')" />
          <el-button size="small" :icon="ArrowDown" circle title="下移" @click="move(row, 'down')" />
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
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="form.name" maxlength="100" style="flex:1" @keyup.enter="searchDouban" />
            <el-button type="warning" :loading="searching" @click="searchDouban">🔍 检索</el-button>
          </div>
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

    <!-- 豆瓣候选弹层 -->
    <el-dialog v-model="doubanOpen" title="选择匹配的电影" width="520px" append-to-body>
      <div v-loading="searching">
        <div v-if="candidates.length === 0 && !searching" class="empty-tip">
          未找到相关电影，请手动填写
        </div>
        <div
          v-for="(c, i) in candidates"
          :key="i"
          class="candidate-item"
          @click="selectCandidate(c)"
        >
          <img :src="resolveImg(c.posterUrl)" class="candidate-poster" referrerpolicy="no-referrer" />
          <div class="candidate-info">
            <div class="candidate-title">{{ c.title }}</div>
            <div class="candidate-meta">{{ c.year }} · ★{{ c.rating }} · {{ c.director }}</div>
            <div class="candidate-summary">{{ c.summary }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import type { DoubanCandidate, Movie } from '@/types/api'

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

// 豆瓣检索
const doubanOpen = ref(false)
const searching = ref(false)
const fetching = ref(false)
const candidates = ref<DoubanCandidate[]>([])

async function searchDouban() {
  if (!form.name.trim()) {
    ElMessage.warning('请先输入电影名称')
    return
  }
  searching.value = true
  doubanOpen.value = true
  candidates.value = []
  try {
    candidates.value = await adminApi.movieSearch(form.name.trim())
    if (candidates.value.length === 0) {
      ElMessage.info('未找到相关电影，请手动填写')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '检索失败，请手动填写')
    doubanOpen.value = false
  } finally {
    searching.value = false
  }
}

async function selectCandidate(c: DoubanCandidate) {
  doubanOpen.value = false
  fetching.value = true
  ElMessage.info(`正在获取「${c.title}」的详细信息...`)
  try {
    const info = await adminApi.movieFetch(c.subjectUrl)
    // 自动填充表单
    if (info.name) form.name = info.name
    if (info.type) form.type = info.type
    if (info.duration) form.duration = info.duration
    if (info.rating) form.rating = info.rating
    if (info.theme) form.theme = info.theme
    if (info.note) form.note = info.note
    if (info.cover) form.cover = info.cover
    ElMessage.success('信息已填充，请按需修改推荐主题后保存')
  } catch (e: any) {
    ElMessage.error(e.message || '获取详情失败，请手动填写')
  } finally {
    fetching.value = false
  }
}

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
    id: 0, no: total.value + 1, name: '', type: '', duration: 90, rating: 8.0,
    theme: '', note: '', cover: '', watched: false, enabled: true, sortOrder: total.value
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

/** 行内快捷切换上架/下架，失败时回滚显示 */
async function toggleEnabled(row: Movie) {
  try {
    await adminApi.movieUpdate(row.id, {
      no: row.no, name: row.name, type: row.type, duration: row.duration,
      rating: row.rating, theme: row.theme, note: row.note, cover: row.cover,
      watched: row.watched, enabled: row.enabled, sortOrder: row.sortOrder
    })
    ElMessage.success(row.enabled ? '已上架' : '已下架')
  } catch {
    row.enabled = !row.enabled
  }
}

/** 上移/下移排序（后端全序交换，跨页也生效） */
async function move(row: Movie, dir: 'up' | 'down') {
  await adminApi.movieMove(row.id, dir)
  await load()
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

/* 豆瓣候选弹层 */
.empty-tip {
  text-align: center;
  color: #999;
  padding: 30px 0;
}

.candidate-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  border: 1px solid #e8eaf1;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.candidate-item:hover {
  border-color: #409eff;
  background: #f0f7ff;
}

.candidate-poster {
  width: 52px;
  height: 74px;
  border-radius: 4px;
  object-fit: cover;
  flex: none;
}

.candidate-info {
  flex: 1;
  min-width: 0;
}

.candidate-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}

.candidate-meta {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.candidate-summary {
  font-size: 12px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
