<template>
  <div class="page exemption-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">免考管理</h2>
        <p class="page-subtitle">办理免考申请、审核处理和流程详情查询。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新增申请</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="申请编号、姓名、课程" clearable @keyup.enter="handleSearch" />
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
        <el-table-column prop="courseCode" label="免考课程代码" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.courseCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="courseName" label="免考课程" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sourceCourseName" label="证明来源课程" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sourceCourseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="applicationStatusTag(row.applicationStatus)">
              {{ applicationStatusText(row.applicationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="360" fixed="right">
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
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="warning"
              :icon="RefreshLeft"
              @click="openWithdrawDialog(row)"
            >
              撤回
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

    <el-drawer v-model="formDrawerVisible" :title="formMode === 'create' ? '新增免考申请' : '编辑免考申请'" size="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item v-if="formMode === 'create'" label="考籍档案ID" prop="recordId">
          <el-input-number v-model="form.recordId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="免考课程代码" prop="courseCode">
          <el-input v-model="form.courseCode" placeholder="例如：00015" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="免考课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="例如：英语（二）" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="来源课程代码">
          <el-input v-model="form.sourceCourseCode" placeholder="例如：CET4" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="来源课程名称">
          <el-input v-model="form.sourceCourseName" placeholder="例如：大学英语四级" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="免考原因" prop="exemptionReason">
          <el-input v-model="form.exemptionReason" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="材料ID">
          <el-input v-model="materialIdsText" placeholder="多个材料ID用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="免考申请详情" size="760px">
      <template v-if="selectedApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ selectedApplication.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ applicationStatusText(selectedApplication.applicationStatus) }}</el-descriptions-item>
          <el-descriptions-item label="考生姓名">{{ selectedApplication.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考籍号">{{ selectedApplication.recordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="免考课程">
            {{ formatCourse(selectedApplication.courseCode, selectedApplication.courseName) }}
          </el-descriptions-item>
          <el-descriptions-item label="来源课程">
            {{ formatCourse(selectedApplication.sourceCourseCode, selectedApplication.sourceCourseName) }}
          </el-descriptions-item>
          <el-descriptions-item label="材料ID" :span="2">
            {{ selectedApplication.materialIds?.length ? selectedApplication.materialIds.join('、') : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="免考原因" :span="2">
            {{ selectedApplication.exemptionReason || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">
            {{ selectedApplication.auditOpinion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ selectedApplication.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="section-title">流程记录</h3>
        <el-empty v-if="!flowRecords.length" description="暂无流程记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="record in flowRecords"
            :key="record.id"
            :timestamp="record.operationTime"
            placement="top"
          >
            <div class="timeline-title">
              {{ flowActionText(record.auditAction) }}
              <el-tag size="small" :type="applicationStatusTag(record.afterStatus)">
                {{ applicationStatusText(record.afterStatus) }}
              </el-tag>
            </div>
            <p class="timeline-content">{{ record.auditOpinion || '-' }}</p>
            <p class="timeline-meta">操作人：{{ record.auditorName || '-' }}</p>
          </el-timeline-item>
        </el-timeline>

        <ApplicationMaterialAuditPanel :application-id="selectedApplication.id" />
      </template>
    </el-drawer>

    <el-dialog v-model="auditDialogVisible" :title="auditMode === 'approve' ? '审核通过' : '审核驳回'" width="880px">
      <ApplicationMaterialAuditPanel
        v-if="selectedApplication"
        :application-id="selectedApplication.id"
        title="审核前智能核验"
        description="审核通过或驳回前，先查看材料缺失和异常提醒。"
      />
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

    <el-dialog v-model="withdrawDialogVisible" title="撤回免考申请" width="520px">
      <el-form :model="withdrawForm" label-width="90px">
        <el-form-item label="撤回原因">
          <el-input v-model="withdrawForm.withdrawReason" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitWithdraw">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose, Edit, Plus, Refresh, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import ApplicationMaterialAuditPanel from '../../components/ai/ApplicationMaterialAuditPanel.vue'
import {
  approveExemptionApplication,
  getExemptionApplicationDetail,
  listExemptionFlowRecords,
  pageExemptionApplications,
  rejectExemptionApplication,
  submitExemptionApplication,
  updateExemptionApplication,
  withdrawExemptionApplication,
  type ExemptionApplication,
  type ExemptionFlowRecord
} from '../../api/exemption'

const loading = ref(false)
const saving = ref(false)
const applications = ref<ExemptionApplication[]>([])
const total = ref(0)
const formDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const auditDialogVisible = ref(false)
const withdrawDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const auditMode = ref<'approve' | 'reject'>('approve')
const selectedApplication = ref<ExemptionApplication | null>(null)
const flowRecords = ref<ExemptionFlowRecord[]>([])
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
  courseCode: '',
  courseName: '',
  sourceCourseCode: '',
  sourceCourseName: '',
  exemptionReason: '',
  remark: ''
})

const auditForm = reactive({
  auditOpinion: ''
})

const withdrawForm = reactive({
  withdrawReason: ''
})

const formRules: FormRules = {
  recordId: [{ required: true, message: '请输入考籍档案ID', trigger: 'blur' }],
  courseCode: [{ required: true, message: '请输入免考课程代码', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入免考课程名称', trigger: 'blur' }],
  exemptionReason: [{ required: true, message: '请输入免考原因', trigger: 'blur' }]
}

/**
 * @brief 加载免考申请分页列表。
 */
async function loadApplications() {
  loading.value = true
  try {
    const result = await pageExemptionApplications({
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
  Object.assign(form, {
    id: 0,
    recordId: undefined,
    courseCode: '',
    courseName: '',
    sourceCourseCode: '',
    sourceCourseName: '',
    exemptionReason: '',
    remark: ''
  })
  materialIdsText.value = ''
  formDrawerVisible.value = true
  formRef.value?.clearValidate()
}

async function openEditDrawer(row: ExemptionApplication) {
  formMode.value = 'edit'
  selectedApplication.value = await getExemptionApplicationDetail(row.id)
  Object.assign(form, {
    id: selectedApplication.value.id,
    recordId: selectedApplication.value.recordId,
    courseCode: selectedApplication.value.courseCode || '',
    courseName: selectedApplication.value.courseName || '',
    sourceCourseCode: selectedApplication.value.sourceCourseCode || '',
    sourceCourseName: selectedApplication.value.sourceCourseName || '',
    exemptionReason: selectedApplication.value.exemptionReason || '',
    remark: selectedApplication.value.remark || ''
  })
  materialIdsText.value = (selectedApplication.value.materialIds || []).join(',')
  formDrawerVisible.value = true
  formRef.value?.clearValidate()
}

async function openDetailDrawer(row: ExemptionApplication) {
  selectedApplication.value = await getExemptionApplicationDetail(row.id)
  flowRecords.value = await listExemptionFlowRecords(row.id)
  detailDrawerVisible.value = true
}

function openAuditDrawer(row: ExemptionApplication, mode: 'approve' | 'reject') {
  selectedApplication.value = row
  auditMode.value = mode
  auditForm.auditOpinion = mode === 'approve' ? '证明材料真实有效，同意该课程免考。' : ''
  auditDialogVisible.value = true
}

function openWithdrawDialog(row: ExemptionApplication) {
  selectedApplication.value = row
  withdrawForm.withdrawReason = ''
  withdrawDialogVisible.value = true
}

async function submitForm() {
  if (saving.value) return
  if (!formRef.value) {
    ElMessage.error('表单未初始化，请关闭后重新打开')
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先完善必填信息')
    return
  }
  saving.value = true
  try {
    const payload = {
      recordId: form.recordId,
      courseCode: form.courseCode,
      courseName: form.courseName,
      sourceCourseCode: form.sourceCourseCode || undefined,
      sourceCourseName: form.sourceCourseName || undefined,
      exemptionReason: form.exemptionReason,
      materialIds: parseMaterialIds(),
      remark: form.remark || undefined
    }
    if (formMode.value === 'create') {
      await submitExemptionApplication(payload)
    } else {
      await updateExemptionApplication(form.id, payload)
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
      await approveExemptionApplication(selectedApplication.value.id, auditForm)
    } else {
      await rejectExemptionApplication(selectedApplication.value.id, auditForm)
    }
    ElMessage.success('审核完成')
    auditDialogVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

async function submitWithdraw() {
  if (!selectedApplication.value) return
  if (!withdrawForm.withdrawReason.trim()) {
    ElMessage.warning('请输入撤回原因')
    return
  }
  saving.value = true
  try {
    await withdrawExemptionApplication(selectedApplication.value.id, withdrawForm)
    ElMessage.success('撤回成功')
    withdrawDialogVisible.value = false
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

function formatCourse(code?: string, name?: string) {
  if (code && name) return `${code} / ${name}`
  return code || name || '-'
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
.exemption-page {
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

.section-title {
  margin: 18px 0 12px;
  font-size: 16px;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.timeline-content,
.timeline-meta {
  margin: 6px 0 0;
  color: #64748b;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
