<template>
  <div class="page graduation-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">毕业申请</h2>
        <p class="page-subtitle">办理毕业申请、资格校验、审核和结果查询。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新增申请</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="申请编号、姓名、考籍号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="申请状态">
          <el-select v-model="query.applicationStatus" clearable placeholder="全部">
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="审核通过" value="APPROVED" />
            <el-option label="审核驳回" value="REJECTED" />
            <el-option label="已撤回" value="WITHDRAWN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="applications" border>
        <el-table-column prop="applicationNo" label="申请编号" min-width="170" fixed="left" show-overflow-tooltip />
        <el-table-column prop="candidateName" label="考生姓名" min-width="110">
          <template #default="{ row }">{{ row.candidateName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="recordNo" label="考籍号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.recordNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="majorName" label="专业" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.majorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="graduationBatch" label="毕业批次" min-width="130">
          <template #default="{ row }">{{ row.graduationBatch || '-' }}</template>
        </el-table-column>
        <el-table-column label="资格校验" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.eligibilityPassed ? 'success' : 'danger'">
              {{ row.eligibilityPassed ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="applicationStatusTag(row.applicationStatus)">
              {{ applicationStatusText(row.applicationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetailDrawer(row)">详情</el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="primary"
              :icon="Edit"
              @click="openEditDrawer(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="success"
              :icon="CircleCheck"
              @click="openAuditDrawer(row, 'approve')"
            >
              通过
            </el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="danger"
              :icon="CircleClose"
              @click="openAuditDrawer(row, 'reject')"
            >
              驳回
            </el-button>
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
          @size-change="loadApplications"
          @current-change="loadApplications"
        />
      </div>
    </el-card>

    <el-drawer v-model="formDrawerVisible" :title="formMode === 'create' ? '新增毕业申请' : '编辑毕业申请'" size="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item v-if="formMode === 'create'" label="考籍档案ID" prop="recordId">
          <el-input-number v-model="form.recordId" :min="1" controls-position="right" />
          <el-button class="inline-action" :icon="CircleCheck" @click="handleEligibilityCheck">资格校验</el-button>
        </el-form-item>
        <el-form-item label="毕业批次" prop="graduationBatch">
          <el-input v-model="form.graduationBatch" placeholder="例如：2026年上半年" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="学位类型">
          <el-select v-model="form.degreeApplyType" clearable placeholder="请选择">
            <el-option label="不申请学位" value="NONE" />
            <el-option label="同时申请学位" value="DEGREE" />
            <el-option label="仅毕业证" value="DIPLOMA_ONLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因" prop="applyReason">
          <el-input v-model="form.applyReason" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="材料ID">
          <el-input v-model="materialIdsText" placeholder="多个材料ID用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="eligibility"
        class="eligibility-alert"
        :type="eligibility.eligible ? 'success' : 'error'"
        :title="eligibility.eligible ? '资格校验通过' : '资格校验未通过'"
        show-icon
        :closable="false"
      >
        <div class="eligibility-list">
          <p v-for="item in eligibility.passedItems" :key="`p-${item}`">通过：{{ item }}</p>
          <p v-for="item in eligibility.failedItems" :key="`f-${item}`">未通过：{{ item }}</p>
          <p v-for="item in eligibility.warningItems" :key="`w-${item}`">提醒：{{ item }}</p>
        </div>
      </el-alert>

      <template #footer>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="毕业申请详情" size="760px">
      <template v-if="selectedApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ selectedApplication.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ applicationStatusText(selectedApplication.applicationStatus) }}</el-descriptions-item>
          <el-descriptions-item label="考生姓名">{{ selectedApplication.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考籍号">{{ selectedApplication.recordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ selectedApplication.majorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="毕业批次">{{ selectedApplication.graduationBatch || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资格校验" :span="2">
            {{ selectedApplication.eligibilitySummary || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">
            {{ selectedApplication.auditOpinion || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="section-title">流程记录</h3>
        <el-timeline>
          <el-timeline-item
            v-for="record in flowRecords"
            :key="record.id"
            :timestamp="record.operationTime"
            placement="top"
          >
            {{ flowActionText(record.auditAction) }}：{{ record.auditOpinion || '-' }}
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>

    <el-dialog v-model="auditDialogVisible" :title="auditMode === 'approve' ? '审核通过' : '审核驳回'" width="520px">
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.auditOpinion" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import {
  approveGraduationApplication,
  checkGraduationEligibility,
  getGraduationApplicationDetail,
  listGraduationFlowRecords,
  pageGraduationApplications,
  rejectGraduationApplication,
  submitGraduationApplication,
  updateGraduationApplication,
  type GraduationApplication,
  type GraduationEligibility,
  type GraduationFlowRecord
} from '../../api/graduation'

const loading = ref(false)
const saving = ref(false)
const applications = ref<GraduationApplication[]>([])
const total = ref(0)
const formDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const auditDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const auditMode = ref<'approve' | 'reject'>('approve')
const selectedApplication = ref<GraduationApplication | null>(null)
const eligibility = ref<GraduationEligibility | null>(null)
const flowRecords = ref<GraduationFlowRecord[]>([])
const formRef = ref<FormInstance>()
const materialIdsText = ref('')

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  applicationStatus: ''
})

const form = reactive({
  id: 0,
  recordId: undefined as number | undefined,
  graduationBatch: '',
  degreeApplyType: '',
  applyReason: '',
  remark: ''
})

const auditForm = reactive({
  auditOpinion: ''
})

const formRules: FormRules = {
  recordId: [{ required: true, message: '请输入考籍档案ID', trigger: 'blur' }],
  graduationBatch: [{ required: true, message: '请输入毕业批次', trigger: 'blur' }],
  applyReason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }]
}

/**
 * @brief 加载毕业申请分页列表。
 */
async function loadApplications() {
  loading.value = true
  try {
    const result = await pageGraduationApplications({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      applicationStatus: query.applicationStatus || undefined
    })
    applications.value = result.records ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNo = 1
  loadApplications()
}

function resetQuery() {
  query.pageNo = 1
  query.keyword = ''
  query.applicationStatus = ''
  loadApplications()
}

function openCreateDrawer() {
  formMode.value = 'create'
  selectedApplication.value = null
  eligibility.value = null
  Object.assign(form, {
    id: 0,
    recordId: undefined,
    graduationBatch: '',
    degreeApplyType: '',
    applyReason: '',
    remark: ''
  })
  materialIdsText.value = ''
  formDrawerVisible.value = true
}

async function openEditDrawer(row: GraduationApplication) {
  formMode.value = 'edit'
  selectedApplication.value = await getGraduationApplicationDetail(row.id)
  eligibility.value = null
  Object.assign(form, {
    id: selectedApplication.value.id,
    recordId: selectedApplication.value.recordId,
    graduationBatch: selectedApplication.value.graduationBatch || '',
    degreeApplyType: selectedApplication.value.degreeApplyType || '',
    applyReason: selectedApplication.value.applyReason || '',
    remark: selectedApplication.value.remark || ''
  })
  materialIdsText.value = (selectedApplication.value.materialIds || []).join(',')
  formDrawerVisible.value = true
}

async function openDetailDrawer(row: GraduationApplication) {
  selectedApplication.value = await getGraduationApplicationDetail(row.id)
  flowRecords.value = await listGraduationFlowRecords(row.id)
  detailDrawerVisible.value = true
}

function openAuditDrawer(row: GraduationApplication, mode: 'approve' | 'reject') {
  selectedApplication.value = row
  auditMode.value = mode
  auditForm.auditOpinion = mode === 'approve' ? '毕业资格和材料审核通过。' : ''
  auditDialogVisible.value = true
}

async function handleEligibilityCheck() {
  if (!form.recordId) {
    ElMessage.warning('请先输入考籍档案ID')
    return
  }
  eligibility.value = await checkGraduationEligibility(form.recordId)
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      recordId: form.recordId,
      graduationBatch: form.graduationBatch,
      degreeApplyType: form.degreeApplyType || undefined,
      applyReason: form.applyReason,
      materialIds: parseMaterialIds(),
      remark: form.remark || undefined
    }
    if (formMode.value === 'create') {
      await submitGraduationApplication(payload)
    } else {
      await updateGraduationApplication(form.id, payload)
    }
    ElMessage.success('保存成功')
    formDrawerVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

async function submitAudit() {
  if (!selectedApplication.value) return
  if (!auditForm.auditOpinion.trim()) {
    ElMessage.warning('请输入审核意见')
    return
  }
  saving.value = true
  try {
    if (auditMode.value === 'approve') {
      await approveGraduationApplication(selectedApplication.value.id, auditForm)
    } else {
      await rejectGraduationApplication(selectedApplication.value.id, auditForm)
    }
    ElMessage.success('审核完成')
    auditDialogVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

function parseMaterialIds() {
  if (!materialIdsText.value.trim()) {
    return []
  }
  return materialIdsText.value
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)
}

function applicationStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    SUBMITTED: '已提交',
    AUDITING: '审核中',
    APPROVED: '审核通过',
    REJECTED: '审核驳回',
    WITHDRAWN: '已撤回'
  }
  return statusMap[status || ''] || status || '-'
}

function applicationStatusTag(status?: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'WITHDRAWN') return 'info'
  return 'warning'
}

function flowActionText(action?: string) {
  const actionMap: Record<string, string> = {
    SUBMIT: '提交',
    UPDATE: '修改',
    APPROVE: '通过',
    REJECT: '驳回',
    WITHDRAW: '撤回'
  }
  return actionMap[action || ''] || action || '-'
}

onMounted(loadApplications)
</script>

<style scoped>
.graduation-page {
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

.query-form :deep(.el-select) {
  width: 180px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.inline-action {
  margin-left: 10px;
}

.eligibility-alert {
  margin-top: 16px;
}

.eligibility-list p {
  margin: 4px 0;
}

.section-title {
  margin: 18px 0 12px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
