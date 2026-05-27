<template>
  <div class="page candidate-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">考生管理</h2>
        <p class="page-subtitle">维护考生基础身份、报考和联系方式，作为考籍档案建档基础。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增考生</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="姓名、身份证号、准考证号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="candidates" border>
        <el-table-column prop="name" label="姓名" min-width="110" fixed="left" />
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">{{ row.gender || '-' }}</template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="admissionNo" label="准考证号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.admissionNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="majorName" label="报考专业" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.majorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" min-width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="candidateStatusTag(row.status)">{{ candidateStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetailDrawer(row)">详情</el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadCandidates"
          @current-change="loadCandidates"
        />
      </div>
    </el-card>

    <el-dialog v-model="formDialogVisible" :title="formDialogTitle" width="720px" destroy-on-close>
      <el-form ref="candidateFormRef" :model="candidateForm" :rules="candidateRules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="candidateForm.name" maxlength="64" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="candidateForm.gender" placeholder="请选择性别" clearable>
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="candidateForm.idCard" maxlength="18" placeholder="请输入身份证号" />
          </el-form-item>
          <el-form-item label="准考证号" prop="admissionNo">
            <el-input v-model="candidateForm.admissionNo" maxlength="64" placeholder="请输入准考证号" />
          </el-form-item>
          <el-form-item label="出生日期" prop="birthDate">
            <el-date-picker
              v-model="candidateForm.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择出生日期"
            />
          </el-form-item>
          <el-form-item label="民族" prop="nation">
            <el-input v-model="candidateForm.nation" maxlength="32" placeholder="请输入民族" />
          </el-form-item>
          <el-form-item label="政治面貌" prop="politicalStatus">
            <el-input v-model="candidateForm.politicalStatus" maxlength="64" placeholder="请输入政治面貌" />
          </el-form-item>
          <el-form-item label="学历层次" prop="educationLevel">
            <el-select v-model="candidateForm.educationLevel" placeholder="请选择学历层次" clearable>
              <el-option label="专科" value="专科" />
              <el-option label="本科" value="本科" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="报考专业" prop="majorName">
            <el-input v-model="candidateForm.majorName" maxlength="128" placeholder="请输入报考专业" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="candidateForm.phone" maxlength="32" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="电子邮箱" prop="email">
            <el-input v-model="candidateForm.email" maxlength="128" placeholder="请输入电子邮箱" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="candidateForm.status">
              <el-radio-button label="NORMAL">正常</el-radio-button>
              <el-radio-button label="LOCKED">锁定</el-radio-button>
              <el-radio-button label="DISABLED">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="联系地址" prop="address">
          <el-input v-model="candidateForm.address" type="textarea" maxlength="256" show-word-limit :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCandidate">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" title="考生详情" size="520px" destroy-on-close>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="candidateDetail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="姓名">{{ candidateDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ candidateDetail.gender || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ candidateDetail.idCard }}</el-descriptions-item>
          <el-descriptions-item label="准考证号">{{ candidateDetail.admissionNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ candidateDetail.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="民族">{{ candidateDetail.nation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="政治面貌">{{ candidateDetail.politicalStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学历层次">{{ candidateDetail.educationLevel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报考专业">{{ candidateDetail.majorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ candidateDetail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电子邮箱">{{ candidateDetail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="candidateStatusTag(candidateDetail.status)">
              {{ candidateStatusText(candidateDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系地址">{{ candidateDetail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ candidateDetail.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ candidateDetail.updateTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import {
  createCandidate,
  getCandidateDetail,
  pageCandidates,
  updateCandidate,
  type Candidate,
  type CandidateCreatePayload
} from '../../api/candidate'

type CandidateFormModel = Required<Pick<CandidateCreatePayload, 'name' | 'idCard'>> &
  Omit<CandidateCreatePayload, 'name' | 'idCard'>

const loading = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const candidates = ref<Candidate[]>([])
const candidateDetail = ref<Candidate | null>(null)
const total = ref(0)
const editingCandidateId = ref<number | null>(null)
const formDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const candidateFormRef = ref<FormInstance>()

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const candidateForm = reactive<CandidateFormModel>({
  name: '',
  gender: '',
  idCard: '',
  admissionNo: '',
  birthDate: '',
  nation: '',
  politicalStatus: '',
  educationLevel: '',
  majorName: '',
  phone: '',
  email: '',
  address: '',
  status: 'NORMAL'
})

const candidateRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { min: 15, max: 18, message: '身份证号长度应为 15 至 18 位', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '请输入正确的电子邮箱', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const formDialogTitle = computed(() => (editingCandidateId.value ? '编辑考生' : '新增考生'))

function candidateStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    NORMAL: '正常',
    LOCKED: '锁定',
    DISABLED: '停用'
  }
  return statusMap[status || ''] || status || '-'
}

function candidateStatusTag(status?: string) {
  if (status === 'NORMAL') {
    return 'success'
  }
  if (status === 'LOCKED') {
    return 'warning'
  }
  return 'danger'
}

function resetCandidateForm() {
  editingCandidateId.value = null
  Object.assign(candidateForm, {
    name: '',
    gender: '',
    idCard: '',
    admissionNo: '',
    birthDate: '',
    nation: '',
    politicalStatus: '',
    educationLevel: '',
    majorName: '',
    phone: '',
    email: '',
    address: '',
    status: 'NORMAL'
  })
  candidateFormRef.value?.clearValidate()
}

async function loadCandidates() {
  loading.value = true
  try {
    const result = await pageCandidates({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined
    })
    candidates.value = result.records ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNo = 1
  loadCandidates()
}

function resetQuery() {
  query.pageNo = 1
  query.keyword = ''
  loadCandidates()
}

function openCreateDialog() {
  resetCandidateForm()
  formDialogVisible.value = true
}

async function openEditDialog(row: Candidate) {
  resetCandidateForm()
  editingCandidateId.value = row.id
  const detail = await getCandidateDetail(row.id)
  Object.assign(candidateForm, {
    name: detail.name,
    gender: detail.gender || '',
    idCard: detail.idCard,
    admissionNo: detail.admissionNo || '',
    birthDate: detail.birthDate || '',
    nation: detail.nation || '',
    politicalStatus: detail.politicalStatus || '',
    educationLevel: detail.educationLevel || '',
    majorName: detail.majorName || '',
    phone: detail.phone || '',
    email: detail.email || '',
    address: detail.address || '',
    status: detail.status || 'NORMAL'
  })
  formDialogVisible.value = true
}

async function openDetailDrawer(row: Candidate) {
  detailDrawerVisible.value = true
  detailLoading.value = true
  candidateDetail.value = null
  try {
    candidateDetail.value = await getCandidateDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

function buildPayload(): CandidateCreatePayload {
  return {
    name: candidateForm.name,
    gender: candidateForm.gender || undefined,
    idCard: candidateForm.idCard,
    admissionNo: candidateForm.admissionNo || undefined,
    birthDate: candidateForm.birthDate || undefined,
    nation: candidateForm.nation || undefined,
    politicalStatus: candidateForm.politicalStatus || undefined,
    educationLevel: candidateForm.educationLevel || undefined,
    majorName: candidateForm.majorName || undefined,
    phone: candidateForm.phone || undefined,
    email: candidateForm.email || undefined,
    address: candidateForm.address || undefined,
    status: candidateForm.status || 'NORMAL'
  }
}

async function submitCandidate() {
  const valid = await candidateFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    if (editingCandidateId.value) {
      await updateCandidate(editingCandidateId.value, buildPayload())
    } else {
      await createCandidate(buildPayload())
    }
    ElMessage.success('保存成功')
    formDialogVisible.value = false
    await loadCandidates()
  } finally {
    submitting.value = false
  }
}

onMounted(loadCandidates)
</script>

<style scoped>
.candidate-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-subtitle {
  margin: -8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.query-card,
.table-card {
  border-radius: 8px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
}

.form-grid :deep(.el-date-editor.el-input),
.form-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
