<template>
  <el-card>
    <el-tabs v-model="tab">
      <!-- ==================== 单元 ==================== -->
      <el-tab-pane label="单元" name="unit">
        <div class="toolbar">
          <el-button type="primary" @click="openUnit()">新增单元</el-button>
        </div>
        <el-table :data="units" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="科目" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.subject === 'math' ? 'primary' : row.subject === 'english' ? 'warning' : 'success'">
                {{ SUBJECT_LABEL[row.subject] ?? row.subject }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="unitNo" label="单元号" width="90" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openUnit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delUnit(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ==================== 课文 ==================== -->
      <el-tab-pane label="课文 / 语文园地" name="lesson">
        <div class="toolbar">
          <el-select v-model="lessonUnitId" placeholder="全部单元" clearable style="width: 260px" @change="loadLessons">
            <el-option v-for="u in units" :key="u.id" :label="`[${SUBJECT_LABEL[u.subject] ?? u.subject}] 第${u.unitNo}单元 ${u.title}`" :value="u.id" />
          </el-select>
          <el-button type="primary" @click="openLesson()">新增课文</el-button>
        </div>
        <el-table :data="lessons" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="lessonNo" label="课序" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="row.lessonType === 'GARDEN' ? 'warning' : row.lessonType === 'TEXT' ? 'primary' : 'success'">
                {{ LESSON_TYPE_LABEL[row.lessonType] ?? row.lessonType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="略读" width="80">
            <template #default="{ row }">{{ row.isSkim ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openLesson(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delLesson(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ==================== 关卡（F-AD-06） ==================== -->
      <el-tab-pane label="关卡管理" name="level">
        <div class="toolbar">
          <el-select v-model="levelLessonId" placeholder="全部课文" clearable filterable style="width: 280px" @change="loadLevels">
            <el-option v-for="l in allLessons" :key="l.id" :label="l.title" :value="l.id" />
          </el-select>
          <el-button type="primary" @click="openLevel()">新增关卡</el-button>
          <span class="tip">可重命名关卡、调整每关题数（questionCount 决定本关抽题数）</span>
        </div>
        <el-table :data="levels" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="所属课文" min-width="160">
            <template #default="{ row }">{{ lessonTitle(row.lessonId) }}</template>
          </el-table-column>
          <el-table-column prop="levelNo" label="关号" width="80" />
          <el-table-column prop="name" label="关卡名" min-width="140" />
          <el-table-column prop="questionCount" label="每关题数" width="100" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openLevel(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delLevel(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 单元编辑 -->
    <el-dialog v-model="unitOpen" :title="unitForm.id ? '编辑单元' : '新增单元'" width="460px">
      <el-form :model="unitForm" label-width="90px">
        <el-form-item label="科目" required>
          <el-radio-group v-model="unitForm.subject">
            <el-radio value="chinese">语文</el-radio>
            <el-radio value="math">数学</el-radio>
            <el-radio value="english">英语</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="单元号" required><el-input-number v-model="unitForm.unitNo" :min="1" :max="20" /></el-form-item>
        <el-form-item label="标题" required><el-input v-model="unitForm.title" maxlength="50" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="unitForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="unitForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unitOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUnit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 课文编辑 -->
    <el-dialog v-model="lessonOpen" :title="lessonForm.id ? '编辑课文' : '新增课文'" width="460px">
      <el-form :model="lessonForm" label-width="90px">
        <el-form-item label="所属单元" required>
          <el-select v-model="lessonForm.unitId" style="width: 100%">
            <el-option v-for="u in units" :key="u.id" :label="`第${u.unitNo}单元 ${u.title}`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课序" required><el-input-number v-model="lessonForm.lessonNo" :min="1" :max="60" /></el-form-item>
        <el-form-item label="标题" required><el-input v-model="lessonForm.title" maxlength="50" /></el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="lessonForm.lessonType">
            <el-radio value="TEXT">课文</el-radio>
            <el-radio value="GARDEN">语文园地</el-radio>
            <el-radio value="PRACTICE">综合实践</el-radio>
            <el-radio value="FUN">数学好玩</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="略读"><el-switch v-model="lessonForm.isSkim" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="lessonForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lessonOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLesson">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关卡编辑 -->
    <el-dialog v-model="levelOpen" :title="levelForm.id ? '编辑关卡' : '新增关卡'" width="460px">
      <el-form :model="levelForm" label-width="100px">
        <el-form-item label="所属课文" required>
          <el-select v-model="levelForm.lessonId" filterable style="width: 100%">
            <el-option v-for="l in allLessons" :key="l.id" :label="l.title" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关号" required><el-input-number v-model="levelForm.levelNo" :min="1" :max="50" /></el-form-item>
        <el-form-item label="关卡名"><el-input v-model="levelForm.name" maxlength="50" placeholder="默认“第N关”" /></el-form-item>
        <el-form-item label="每关题数"><el-input-number v-model="levelForm.questionCount" :min="1" :max="20" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="levelForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLevel">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { Level, Lesson, Unit } from '@/types/api'

const tab = ref('unit')
const loading = ref(false)
const saving = ref(false)

const units = ref<Unit[]>([])
const lessons = ref<Lesson[]>([])
const levels = ref<Level[]>([])
const allLessons = ref<Lesson[]>([])

const lessonUnitId = ref<number | undefined>()
const levelLessonId = ref<number | undefined>()

const unitOpen = ref(false)
const lessonOpen = ref(false)
const levelOpen = ref(false)

const unitForm = reactive({ id: 0, subject: 'chinese' as string, unitNo: 1, title: '', description: '', sortOrder: 0 })
const lessonForm = reactive({ id: 0, unitId: 1, lessonNo: 1, title: '', lessonType: 'TEXT' as 'TEXT' | 'GARDEN' | 'PRACTICE' | 'FUN', isSkim: false, sortOrder: 0 })
const levelForm = reactive({ id: 0, lessonId: 1, levelNo: 1, name: '', questionCount: 5, sortOrder: 0 })

const lessonTitle = (id: number) => allLessons.value.find((l) => l.id === id)?.title ?? `课文#${id}`

const LESSON_TYPE_LABEL: Record<string, string> = {
  TEXT: '课文',
  GARDEN: '语文园地',
  PRACTICE: '综合实践',
  FUN: '数学好玩'
}

/** 科目显示名（v2.6：三科） */
const SUBJECT_LABEL: Record<string, string> = {
  chinese: '语文',
  math: '数学',
  english: '英语'
}

async function loadUnits() {
  units.value = await adminApi.unitList()
}

async function loadLessons() {
  loading.value = true
  try {
    lessons.value = await adminApi.lessonList(lessonUnitId.value ? { unitId: lessonUnitId.value } : undefined)
  } finally {
    loading.value = false
  }
}

async function loadLevels() {
  loading.value = true
  try {
    levels.value = await adminApi.levelList(levelLessonId.value ? { lessonId: levelLessonId.value } : undefined)
  } finally {
    loading.value = false
  }
}

function openUnit(row?: Unit) {
  Object.assign(unitForm, row
    ? { id: row.id, subject: row.subject ?? 'chinese', unitNo: row.unitNo, title: row.title, description: row.description, sortOrder: row.sortOrder }
    : { id: 0, subject: 'chinese', unitNo: units.value.length + 1, title: '', description: '', sortOrder: units.value.length })
  unitOpen.value = true
}

async function saveUnit() {
  if (!unitForm.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const data = { subject: unitForm.subject, unitNo: unitForm.unitNo, title: unitForm.title.trim(), description: unitForm.description, sortOrder: unitForm.sortOrder }
    if (unitForm.id) await adminApi.unitUpdate(unitForm.id, data)
    else await adminApi.unitCreate(data)
    ElMessage.success('已保存')
    unitOpen.value = false
    await loadUnits()
  } finally {
    saving.value = false
  }
}

function delUnit(row: Unit) {
  ElMessageBox.confirm(`删除单元「${row.title}」？其下课文与题目需先清理。`, '删除单元', { type: 'warning' })
    .then(async () => {
      await adminApi.unitDelete(row.id)
      ElMessage.success('已删除')
      await loadUnits()
    })
    .catch(() => undefined)
}

function openLesson(row?: Lesson) {
  Object.assign(lessonForm, row
    ? { id: row.id, unitId: row.unitId, lessonNo: row.lessonNo, title: row.title, lessonType: row.lessonType, isSkim: row.isSkim, sortOrder: row.sortOrder }
    : { id: 0, unitId: lessonUnitId.value ?? units.value[0]?.id ?? 1, lessonNo: lessons.value.length + 1, title: '', lessonType: 'TEXT', isSkim: false, sortOrder: lessons.value.length })
  lessonOpen.value = true
}

async function saveLesson() {
  if (!lessonForm.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const data = {
      unitId: lessonForm.unitId,
      lessonNo: lessonForm.lessonNo,
      title: lessonForm.title.trim(),
      lessonType: lessonForm.lessonType,
      isSkim: lessonForm.isSkim,
      sortOrder: lessonForm.sortOrder
    }
    if (lessonForm.id) await adminApi.lessonUpdate(lessonForm.id, data)
    else await adminApi.lessonCreate(data)
    ElMessage.success('已保存')
    lessonOpen.value = false
    await Promise.all([loadLessons(), loadAllLessons()])
  } finally {
    saving.value = false
  }
}

function delLesson(row: Lesson) {
  ElMessageBox.confirm(`删除课文「${row.title}」？其下关卡与题目需先清理。`, '删除课文', { type: 'warning' })
    .then(async () => {
      await adminApi.lessonDelete(row.id)
      ElMessage.success('已删除')
      await Promise.all([loadLessons(), loadAllLessons()])
    })
    .catch(() => undefined)
}

function openLevel(row?: Level) {
  Object.assign(levelForm, row
    ? { id: row.id, lessonId: row.lessonId, levelNo: row.levelNo, name: row.name, questionCount: row.questionCount, sortOrder: row.sortOrder }
    : { id: 0, lessonId: levelLessonId.value ?? allLessons.value[0]?.id ?? 1, levelNo: levels.value.length + 1, name: '', questionCount: 5, sortOrder: levels.value.length })
  levelOpen.value = true
}

async function saveLevel() {
  if (!levelForm.lessonId) {
    ElMessage.warning('请选择所属课文')
    return
  }
  saving.value = true
  try {
    const data = {
      lessonId: levelForm.lessonId,
      levelNo: levelForm.levelNo,
      name: levelForm.name,
      questionCount: levelForm.questionCount,
      sortOrder: levelForm.sortOrder
    }
    if (levelForm.id) await adminApi.levelUpdate(levelForm.id, data)
    else await adminApi.levelCreate(data)
    ElMessage.success('已保存')
    levelOpen.value = false
    await loadLevels()
  } finally {
    saving.value = false
  }
}

function delLevel(row: Level) {
  ElMessageBox.confirm(`删除关卡「${row.name || row.levelNo + '关'}」？其下题目需先清理。`, '删除关卡', { type: 'warning' })
    .then(async () => {
      await adminApi.levelDelete(row.id)
      ElMessage.success('已删除')
      await loadLevels()
    })
    .catch(() => undefined)
}

async function loadAllLessons() {
  allLessons.value = await adminApi.lessonList()
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadUnits(), loadAllLessons()])
    await Promise.all([loadLessons(), loadLevels()])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.tip {
  color: #909399;
  font-size: 12px;
}
</style>
