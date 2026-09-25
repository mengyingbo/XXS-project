<template>
  <el-card>
    <template #header>批量导入题目（CSV）</template>

    <el-steps :active="step" simple style="margin-bottom: 20px">
      <el-step title="1. 上传 CSV" />
      <el-step title="2. 校验预览" />
      <el-step title="3. 确认导入" />
    </el-steps>

    <!-- 第 1 步：选择文件 + 默认课文/关卡 -->
    <template v-if="step === 1">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px"
        title="CSV 表头：lessonId,levelId,type,stem,options,answer,analysis,knowledgePoint,difficulty（可从模板下载后填写）" />

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form label-width="100px">
            <el-form-item label="下载模板">
              <el-button @click="downloadTemplate">下载 CSV 模板</el-button>
            </el-form-item>
            <el-form-item label="选择文件">
              <input ref="fileInput" type="file" accept=".csv,text/csv" @change="onFileChange" />
            </el-form-item>
            <el-form-item label="粘贴内容">
              <el-input v-model="csvText" type="textarea" :rows="8" placeholder="或直接把 CSV 内容粘贴到这里" />
            </el-form-item>
            <el-form-item label="默认课文">
              <el-select v-model="defaultLessonId" clearable filterable placeholder="行内未指定 lessonId 时使用" style="width: 100%">
                <el-option v-for="l in allLessons" :key="l.id" :label="l.title" :value="l.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="默认关卡">
              <el-select v-model="defaultLevelId" clearable placeholder="行内未指定 levelId 时使用" style="width: 100%" :disabled="!defaultLessonId">
                <el-option v-for="lv in defaultLevels" :key="lv.id" :label="`${lv.levelNo}关 ${lv.name || ''}`" :value="lv.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="validating" @click="doDryRun">校验并预览</el-button>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>格式说明</template>
            <ul class="fmt-list">
              <li><b>type</b>：SINGLE / JUDGE / BLANK / ORDER</li>
              <li><b>options</b>：SINGLE、ORDER 需填 JSON 数组，如 <code>["甲","乙","丙"]</code></li>
              <li><b>answer</b>：
                SINGLE 填下标数字（第1项=0）；JUDGE 填 true/false；
                BLANK 填文本（多个可接受答案填 JSON 数组）；ORDER 填下标排列如 <code>[2,0,1]</code></li>
              <li><b>difficulty</b>：1~3</li>
              <li>含逗号或引号的列请用双引号包裹，内部引号写成两个双引号</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 第 2/3 步：预览 -->
    <template v-if="step === 2">
      <el-descriptions :column="4" border style="margin-bottom: 14px">
        <el-descriptions-item label="总行数">{{ preview?.total }}</el-descriptions-item>
        <el-descriptions-item label="校验通过">
          <el-tag type="success">{{ preview?.validCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="校验失败">
          <el-tag :type="(preview?.invalidCount ?? 0) > 0 ? 'danger' : 'info'">{{ preview?.invalidCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="默认课文">
          {{ defaultLessonId ? allLessons.find(l => l.id === defaultLessonId)?.title : '未指定' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="preview?.rows ?? []" border max-height="440">
        <el-table-column prop="index" label="#" width="60" />
        <el-table-column prop="type" label="题型" width="90" />
        <el-table-column prop="stem" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column prop="lessonId" label="课文ID" width="80" />
        <el-table-column prop="levelId" label="关卡ID" width="80" />
        <el-table-column label="校验结果" min-width="220">
          <template #default="{ row }">
            <el-tag v-if="row.ok" type="success" size="small">通过</el-tag>
            <template v-else>
              <div v-for="(e, i) in row.errors" :key="i" class="err-line">{{ e }}</div>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="step-actions">
        <el-button @click="step = 1">← 重新编辑</el-button>
        <el-button
          type="primary"
          :disabled="(preview?.validCount ?? 0) === 0"
          :loading="importing"
          @click="doImport"
        >
          确认导入 {{ preview?.validCount }} 道题
        </el-button>
      </div>
    </template>

    <!-- 完成 -->
    <el-result v-if="step === 3" icon="success" title="导入完成"
      :sub-title="`本次成功入库 ${preview?.inserted} 道题目，可在「题库管理」中查看与校对`">
      <template #extra>
        <el-button type="primary" @click="reset">继续导入</el-button>
        <el-button @click="$router.push('/questions')">去题库管理</el-button>
      </template>
    </el-result>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { csvToImportRows } from '@/utils/csv'
import type { ImportResult, Level, Lesson } from '@/types/api'

const step = ref(1)
const csvText = ref('')
const fileInput = ref<HTMLInputElement>()
const validating = ref(false)
const importing = ref(false)
const preview = ref<ImportResult | null>(null)

const allLessons = ref<Lesson[]>([])
const defaultLessonId = ref<number | undefined>()
const defaultLevelId = ref<number | undefined>()
const defaultLevels = computed(() =>
  defaultLessonId.value ? (allLevels.value.filter((l) => l.lessonId === defaultLessonId.value) as Level[]) : []
)
const allLevels = ref<Level[]>([])

async function downloadTemplate() {
  const text = await adminApi.importTemplate()
  const blob = new Blob(['\uFEFF' + text], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'questions-template.csv'
  a.click()
  URL.revokeObjectURL(url)
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  csvText.value = await file.text()
  ElMessage.success(`已读取 ${file.name}`)
}

async function doDryRun() {
  const content = csvText.value.trim()
  if (!content) {
    ElMessage.warning('请选择 CSV 文件或粘贴内容')
    return
  }
  const { rows, error } = csvToImportRows(content)
  if (error) {
    ElMessage.error(error)
    return
  }
  validating.value = true
  try {
    preview.value = await adminApi.importQuestions({
      dryRun: true,
      defaultLessonId: defaultLessonId.value ?? null,
      defaultLevelId: defaultLevelId.value ?? null,
      rows
    })
    step.value = 2
  } finally {
    validating.value = false
  }
}

async function doImport() {
  if (!preview.value) return
  importing.value = true
  try {
    // 重新以 dryRun=false 提交（后端会再次校验，未通过行不入库）
    const content = csvText.value.trim()
    const { rows } = csvToImportRows(content)
    preview.value = await adminApi.importQuestions({
      dryRun: false,
      defaultLessonId: defaultLessonId.value ?? null,
      defaultLevelId: defaultLevelId.value ?? null,
      rows
    })
    step.value = 3
  } finally {
    importing.value = false
  }
}

function reset() {
  step.value = 1
  csvText.value = ''
  preview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

onMounted(async () => {
  const [lessons, levels] = await Promise.all([adminApi.lessonList(), adminApi.levelList()])
  allLessons.value = lessons
  allLevels.value = levels
})
</script>

<style scoped>
.fmt-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
  font-size: 13px;
}

.fmt-list code {
  background: #f5f7fa;
  padding: 1px 5px;
  border-radius: 4px;
}

.err-line {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.6;
}

.step-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
