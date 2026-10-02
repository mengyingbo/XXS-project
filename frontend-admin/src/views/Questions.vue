<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="filter.subject" placeholder="按科目筛选" clearable style="width: 130px" @change="onSubjectFilter">
        <el-option label="语文" value="chinese" />
        <el-option label="数学" value="math" />
        <el-option label="英语" value="english" />
      </el-select>
      <el-select v-model="filter.lessonId" placeholder="按课文筛选" clearable filterable style="width: 220px" @change="onLessonFilter">
        <el-option v-for="l in filterLessons" :key="l.id" :label="l.title" :value="l.id" />
      </el-select>
      <el-select v-model="filter.levelId" placeholder="按关卡筛选" clearable style="width: 180px" :disabled="!filter.lessonId">
        <el-option v-for="lv in filterLevels" :key="lv.id" :label="`${lv.levelNo}关 ${lv.name || ''}`" :value="lv.id" />
      </el-select>
      <el-select v-model="filter.type" placeholder="按题型筛选" clearable style="width: 140px">
        <el-option label="单选题" value="SINGLE" />
        <el-option label="判断题" value="JUDGE" />
        <el-option label="填空题" value="BLANK" />
        <el-option label="排序题" value="ORDER" />
        <el-option label="词语手写" value="HAND" />
      </el-select>
      <el-input v-model="filter.keyword" placeholder="题干关键词" clearable style="width: 200px" @keyup.enter="load(1)" @clear="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button type="success" @click="openCreate">新增题目</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="题型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ TYPE_LABEL[row.type] ?? row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
      <el-table-column label="答案" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ fmtAnswer(row.type, row.answer, row.options) }}</template>
      </el-table-column>
      <el-table-column prop="knowledgePoint" label="知识点" width="120" show-overflow-tooltip />
      <el-table-column label="难度" width="80">
        <template #default="{ row }">
          <el-rate :model-value="row.difficulty" disabled :max="3" />
        </template>
      </el-table-column>
      <el-table-column label="所属课文" width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ lessonTitle(row.lessonId) }}</template>
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

    <!-- 题目编辑 -->
    <el-dialog v-model="editOpen" :title="form.id ? '编辑题目' : '新增题目'" width="680px" top="5vh">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属课文" required>
              <el-select v-model="form.lessonId" filterable style="width: 100%" @change="onFormLesson">
                <el-option v-for="l in allLessons" :key="l.id" :label="l.title" :value="l.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属关卡">
              <el-select v-model="form.levelId" clearable style="width: 100%">
                <el-option v-for="lv in formLevels" :key="lv.id" :label="`${lv.levelNo}关 ${lv.name || ''}`" :value="lv.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="题型" required>
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="单选题" value="SINGLE" />
                <el-option label="判断题" value="JUDGE" />
                <el-option label="填空题" value="BLANK" />
                <el-option label="排序题" value="ORDER" />
                <el-option label="词语手写" value="HAND" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" required>
              <el-rate v-model="form.difficulty" :max="3" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="知识点">
              <el-input v-model="form.knowledgePoint" maxlength="50" placeholder="如：字音" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>

        <!-- SINGLE / ORDER：选项编辑 -->
        <el-form-item v-if="form.type === 'SINGLE' || form.type === 'ORDER'" label="选项" required>
          <div class="options-box">
            <div v-for="(_, i) in optionList" :key="i" class="option-row">
              <span class="opt-idx">{{ i + 1 }}.</span>
              <el-input v-model="optionList[i]" maxlength="100" />
              <el-button size="small" text type="danger" @click="removeOption(i)">✕</el-button>
            </div>
            <el-button size="small" @click="addOption">+ 添加选项</el-button>
          </div>
        </el-form-item>

        <!-- SINGLE：正确项 -->
        <el-form-item v-if="form.type === 'SINGLE'" label="正确选项" required>
          <el-radio-group v-model="form.singleAnswer">
            <el-radio v-for="(_, i) in optionList" :key="i" :value="i">选项 {{ i + 1 }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- JUDGE：正确答案 -->
        <el-form-item v-if="form.type === 'JUDGE'" label="正确答案" required>
          <el-radio-group v-model="form.judgeAnswer">
            <el-radio :value="true">✓ 正确</el-radio>
            <el-radio :value="false">✗ 错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- BLANK / HAND：可接受答案 -->
        <el-form-item v-if="form.type === 'BLANK' || form.type === 'HAND'" label="参考答案" required>
          <div class="options-box">
            <div class="hint">可填多个可接受答案（判分忽略空格/标点/大小写）</div>
            <div v-for="(_, i) in blankAnswers" :key="i" class="option-row">
              <el-input v-model="blankAnswers[i]" maxlength="200" :placeholder="i === 0 ? '标准答案' : '其他可接受的答案'" />
              <el-button size="small" text type="danger" @click="blankAnswers.splice(i, 1)">✕</el-button>
            </div>
            <el-button size="small" @click="blankAnswers.push('')">+ 添加可接受答案</el-button>
          </div>
        </el-form-item>

        <!-- ORDER：正确顺序 -->
        <el-form-item v-if="form.type === 'ORDER'" label="正确顺序" required>
          <div class="options-box">
            <div class="hint">用 ↑↓ 把选项排成正确顺序（答案为下标排列）</div>
            <div v-for="(idx, pos) in orderAnswer" :key="idx" class="option-row">
              <span class="opt-idx">{{ pos + 1 }}.</span>
              <span class="order-text">{{ optionList[idx] || '（空）' }}</span>
              <el-button size="small" text :disabled="pos === 0" @click="moveOrder(pos, -1)">↑</el-button>
              <el-button size="small" text :disabled="pos === orderAnswer.length - 1" @click="moveOrder(pos, 1)">↓</el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="答错时展示给孩子" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { QUESTION_TYPE_LABEL as TYPE_LABEL, fmtAnswer } from '@/utils/format'
import type { Level, Lesson, Question, QuestionType, Unit } from '@/types/api'

const list = ref<Question[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const saving = ref(false)

const allLessons = ref<Lesson[]>([])
const allLevels = ref<Level[]>([])
const allUnits = ref<Unit[]>([])

const filter = reactive<{ subject?: string; lessonId?: number; levelId?: number; type?: QuestionType | ''; keyword: string }>({ keyword: '' })
/** 课文筛选下拉按科目联动（v2.0） */
const filterLessons = computed(() =>
  filter.subject ? allLessons.value.filter((l) => lessonSubject(l.id) === filter.subject) : allLessons.value
)
const filterLevels = computed(() => (filter.lessonId ? allLevels.value.filter((l) => l.lessonId === filter.lessonId) : []))

function lessonSubject(lessonId: number): string | undefined {
  const lesson = allLessons.value.find((l) => l.id === lessonId)
  const unit = lesson ? allUnits.value.find((u) => u.id === lesson.unitId) : undefined
  return unit?.subject
}

const editOpen = ref(false)
const formLevels = ref<Level[]>([])

const form = reactive({
  id: 0,
  lessonId: undefined as number | undefined,
  levelId: null as number | null,
  type: 'SINGLE' as QuestionType,
  stem: '',
  analysis: '',
  knowledgePoint: '',
  difficulty: 1,
  singleAnswer: 0,
  judgeAnswer: true
})
const optionList = ref<string[]>(['', ''])
const blankAnswers = ref<string[]>([''])
/** 排序题正确顺序：存选项下标的排列 */
const orderAnswer = ref<number[]>([])

const lessonTitle = (id: number) => allLessons.value.find((l) => l.id === id)?.title ?? `课文#${id}`

async function load(targetPage?: number) {
  if (targetPage) page.value = targetPage
  loading.value = true
  try {
    const data = await adminApi.questionList({
      page: page.value,
      size: size.value,
      subject: filter.subject || undefined,
      lessonId: filter.lessonId || undefined,
      levelId: filter.levelId || undefined,
      type: filter.type || undefined,
      keyword: filter.keyword || undefined
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onSubjectFilter() {
  filter.lessonId = undefined
  filter.levelId = undefined
  load(1)
}

function onLessonFilter() {
  filter.levelId = undefined
  load(1)
}

function onFormLesson() {
  form.levelId = null
  formLevels.value = form.lessonId ? allLevels.value.filter((l) => l.lessonId === form.lessonId) : []
}

function addOption() {
  optionList.value.push('')
  orderAnswer.value.push(orderAnswer.value.length)
}

function removeOption(i: number) {
  optionList.value.splice(i, 1)
  // 重建顺序排列
  orderAnswer.value = optionList.value.map((_, idx) => idx)
  if (form.singleAnswer >= optionList.value.length) form.singleAnswer = 0
}

function moveOrder(pos: number, dir: -1 | 1) {
  const target = pos + dir
  const arr = orderAnswer.value
  if (target < 0 || target >= arr.length) return
  ;[arr[pos], arr[target]] = [arr[target], arr[pos]]
  orderAnswer.value = [...arr]
}

function resetForm() {
  Object.assign(form, {
    id: 0, lessonId: undefined, levelId: null, type: 'SINGLE', stem: '',
    analysis: '', knowledgePoint: '', difficulty: 1, singleAnswer: 0, judgeAnswer: true
  })
  optionList.value = ['', '']
  blankAnswers.value = ['']
  orderAnswer.value = [0, 1]
  formLevels.value = []
}

function openCreate() {
  resetForm()
  editOpen.value = true
}

function openEdit(row: Question) {
  resetForm()
  form.id = row.id
  form.lessonId = row.lessonId
  form.levelId = row.levelId
  form.type = row.type
  form.stem = row.stem
  form.analysis = row.analysis
  form.knowledgePoint = row.knowledgePoint
  form.difficulty = row.difficulty || 1
  formLevels.value = allLevels.value.filter((l) => l.lessonId === row.lessonId)

  try {
    const options: unknown = row.options ? JSON.parse(row.options) : null
    const answer: unknown = JSON.parse(row.answer)
    if (form.type === 'SINGLE') {
      optionList.value = Array.isArray(options) ? (options as string[]) : ['', '']
      form.singleAnswer = typeof answer === 'number' ? answer : 0
    } else if (form.type === 'JUDGE') {
      form.judgeAnswer = answer === true
    } else if (form.type === 'BLANK' || form.type === 'HAND') {
      blankAnswers.value = Array.isArray(answer) ? (answer as string[]) : [String(answer)]
      if (blankAnswers.value.length === 0) blankAnswers.value = ['']
    } else if (form.type === 'ORDER') {
      optionList.value = Array.isArray(options) ? (options as string[]) : ['', '']
      orderAnswer.value = Array.isArray(answer) ? [...(answer as number[])] : optionList.value.map((_, i) => i)
    }
  } catch {
    ElMessage.warning('原有答案格式解析失败，请重新设置答案')
    optionList.value = ['', '']
    orderAnswer.value = [0, 1]
    blankAnswers.value = ['']
  }
  editOpen.value = true
}

/** 把表单序列化为后端 QuestionSaveReq */
function serialize() {
  let options: string | null = null
  let answer = ''
  if (form.type === 'SINGLE') {
    options = JSON.stringify(optionList.value)
    answer = JSON.stringify(form.singleAnswer)
  } else if (form.type === 'JUDGE') {
    answer = JSON.stringify(form.judgeAnswer)
  } else if (form.type === 'BLANK' || form.type === 'HAND') {
    const answers = blankAnswers.value.map((s) => s.trim()).filter((s) => s !== '')
    answer = answers.length === 1 ? JSON.stringify(answers[0]) : JSON.stringify(answers)
  } else {
    options = JSON.stringify(optionList.value)
    answer = JSON.stringify(orderAnswer.value)
  }
  return {
    lessonId: form.lessonId!,
    levelId: form.levelId,
    type: form.type,
    stem: form.stem.trim(),
    options,
    answer,
    analysis: form.analysis,
    knowledgePoint: form.knowledgePoint,
    difficulty: form.difficulty
  }
}

function validate(): string {
  if (!form.lessonId) return '请选择所属课文'
  if (!form.stem.trim()) return '请填写题干'
  if (form.type === 'SINGLE') {
    const opts = optionList.value.map((s) => s.trim())
    if (opts.length < 2 || opts.some((s) => s === '')) return '单选题需至少 2 个非空选项'
  } else if (form.type === 'ORDER') {
    const opts = optionList.value.map((s) => s.trim())
    if (opts.length < 2 || opts.some((s) => s === '')) return '排序题需至少 2 个非空待排序项'
    if (orderAnswer.value.length !== opts.length) return '请设置完整正确顺序'
  } else if (form.type === 'BLANK' || form.type === 'HAND') {
    if (blankAnswers.value.every((s) => s.trim() === '')) return '请填写至少一个参考答案'
  }
  return ''
}

async function save() {
  const err = validate()
  if (err) {
    ElMessage.warning(err)
    return
  }
  saving.value = true
  try {
    const data = serialize()
    if (form.id) await adminApi.questionUpdate(form.id, data)
    else await adminApi.questionCreate(data)
    ElMessage.success('已保存')
    editOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function del(row: Question) {
  ElMessageBox.confirm(`删除题目「${row.stem.slice(0, 20)}…」？`, '删除题目', { type: 'warning' })
    .then(async () => {
      await adminApi.questionDelete(row.id)
      ElMessage.success('已删除')
      await load()
    })
    .catch(() => undefined)
}

onMounted(async () => {
  loading.value = true
  try {
    const [lessons, levels, units] = await Promise.all([adminApi.lessonList(), adminApi.levelList(), adminApi.unitList()])
    allLessons.value = lessons
    allLevels.value = levels
    allUnits.value = units
    await load()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.options-box {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.opt-idx {
  width: 26px;
  text-align: right;
  color: #606266;
}

.order-text {
  flex: 1;
}

.hint {
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}
</style>
