<template>
  <div class="page transfer-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">考籍转入转出</h2>
        <p class="page-subtitle">办理考籍转入、考籍转出申请、审核和流程记录查看。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新增申请</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="申请编号、姓名、考籍号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="转考类型">
          <el-select v-model="query.transferType" clearable placeholder="全部">
            <el-option label="考籍转入" value="TRANSFER_IN" />
            <el-option label="考籍转出" value="TRANSFER_OUT" />
          </el-select>
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
        <el-table-column label="转考类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="transferTypeTag(row.businessType)">{{ transferTypeText(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="candidateName" label="考生姓名" min-width="110">
          <template #default="{ row }">{{ row.candidateName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="recordNo" label="考籍号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.recordNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sourceProvince" label="原考籍省份" min-width="130">
          <template #default="{ row }">{{ row.sourceProvince || '-' }}</template>
        </el-table-column>
        <el-table-column prop="targetProvince" label="目标省份" min-width="130">
          <template #default="{ row }">{{ row.targetProvince || '-' }}</template>
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
              type="warning"
              :icon="RefreshLeft"
              @click="openWithdrawDialog(row)"
            >
              撤回
            </el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="success"
              :icon="CircleCheck"
              @click="openAuditDialog(row, 'approve')"
            >
              通过
            </el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="danger"
              :icon="CircleClose"
              @click="openAuditDialog(row, 'reject')"
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

    <el-drawer v-model="formDrawerVisible" :title="formMode === 'create' ? '新增转考申请' : '编辑转考申请'" size="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item v-if="formMode === 'create'" label="考籍档案ID" prop="recordId">
          <el-input-number v-model="form.recordId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="转考类型" prop="transferType">
          <el-radio-group v-model="form.transferType" :disabled="formMode === 'edit'" @change="handleTransferTypeChange">
            <el-radio-button label="TRANSFER_IN">考籍转入</el-radio-button>
            <el-radio-button label="TRANSFER_OUT">考籍转出</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.transferType === 'TRANSFER_IN'">
          <el-form-item label="原考籍省份" prop="sourceProvince">
            <el-input v-model="form.sourceProvince" placeholder="例如：湖南省" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="原考籍单位">
            <el-input v-model="form.sourceSchool" placeholder="例如：湖南省自考办" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item label="原考籍号">
            <el-input v-model="form.sourceRecordNo" maxlength="64" show-word-limit />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="目标省份" prop="targetProvince">
            <el-input v-model="form.targetProvince" placeholder="例如：浙江省" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="目标接收单位">
            <el-input v-model="form.targetSchool" placeholder="例如：浙江省自考办" maxlength="128" show-word-limit />
          </el-form-item>
        </template>
        <el-form-item label="转考原因" prop="transferReason">
          <el-input v-model="form.transferReason" type="textarea" :rows="4" maxlength="512" show-word-limit />
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

    <el-drawer v-model="detailDrawerVisible" title="转考申请详情" size="780px">
      <template v-if="selectedApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ selectedApplication.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ applicationStatusText(selectedApplication.applicationStatus) }}</el-descriptions-item>
          <el-descriptions-item label="转考类型">{{ transferTypeText(selectedApplication.businessType) }}</el-descriptions-item>
          <el-descriptions-item label="考籍档案ID">{{ selectedApplication.recordId }}</el-descriptions-item>
          <el-descriptions-item label="考生姓名">{{ selectedApplication.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考籍号">{{ selectedApplication.recordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原考籍省份">{{ selectedApplication.sourceProvince || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原考籍单位">{{ selectedApplication.sourceSchool || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原考籍号">{{ selectedApplication.sourceRecordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标省份">{{ selectedApplication.targetProvince || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标接收单位">{{ selectedApplication.targetSchool || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请材料ID">{{ selectedApplication.materialIds?.join(', ') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转考原因" :span="2">{{ selectedApplication.transferReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ selectedApplication.auditOpinion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ selectedApplication.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="section-title">流程记录</h3>
        <el-timeline>
          <el-timeline-item
            v-for="record in flowRecords"
            :key="record.id"
            :timestamp="record.operationTime"
            placement="top"
          >
            <div class="flow-item">
              <span>{{ flowActionText(record.auditAction) }}</span>
              <span class="flow-status">{{ applicationStatusText(record.beforeStatus) }} → {{ applicationStatusText(record.afterStatus) }}</span>
            </div>
            <p class="flow-opinion">{{ record.auditOpinion || '-' }}</p>
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
        description="审核转考申请前，查看材料完整性和异常材料提醒。"
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

    <el-dialog v-model="withdrawDialogVisible" title="撤回申请" width="520px">
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
  approveTransferApplication,
  getTransferApplicationDetail,
  listTransferFlowRecords,
  pageTransferApplications,
  rejectTransferApplication,
  submitTransferApplication,
  updateTransferApplication,
  withdrawTransferApplication,
  type TransferApplication,
  type TransferFlowRecord
} from '../../api/transfer'

const loading = ref(false)
const saving = ref(false)
const applications = ref<TransferApplication[]>([])
const total = ref(0)
const formDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const auditDialogVisible = ref(false)
const withdrawDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const auditMode = ref<'approve' | 'reject'>('approve')
const selectedApplication = ref<TransferApplication | null>(null)
const flowRecords = ref<TransferFlowRecord[]>([])
const formRef = ref<FormInstance>()
const materialIdsText = ref('')

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  transferType: '',
  applicationStatus: ''
})

const form = reactive({
  id: 0,
  recordId: undefined as number | undefined,
  transferType: 'TRANSFER_IN',
  sourceProvince: '',
  sourceSchool: '',
  sourceRecordNo: '',
  targetProvince: '',
  targetSchool: '',
  transferReason: '',
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
  transferType: [{ required: true, message: '请选择转考类型', trigger: 'change' }],
  sourceProvince: [
    {
      validator: (_rule, value, callback) => {
        if (form.transferType === 'TRANSFER_IN' && !String(value || '').trim()) {
          callback(new Error('请输入原考籍省份'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  targetProvince: [
    {
      validator: (_rule, value, callback) => {
        if (form.transferType === 'TRANSFER_OUT' && !String(value || '').trim()) {
          callback(new Error('请输入目标省份'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  transferReason: [{ required: true, message: '请输入转考原因', trigger: 'blur' }]
}

/**
 * @brief 加载考籍转入转出申请分页列表。
 */
async function loadApplications() {
  loading.value = true
  try {
    const result = await pageTransferApplications({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      transferType: query.transferType || undefined,
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
  query.transferType = ''
  query.applicationStatus = ''
  loadApplications()
}

function openCreateDrawer() {
  formMode.value = 'create'
  selectedApplication.value = null
  resetForm()
  formDrawerVisible.value = true
}

async function openEditDrawer(row: TransferApplication) {
  formMode.value = 'edit'
  selectedApplication.value = await getTransferApplicationDetail(row.id)
  Object.assign(form, {
    id: selectedApplication.value.id,
    recordId: selectedApplication.value.recordId,
    transferType: selectedApplication.value.businessType || 'TRANSFER_IN',
    sourceProvince: selectedApplication.value.sourceProvince || '',
    sourceSchool: selectedApplication.value.sourceSchool || '',
    sourceRecordNo: selectedApplication.value.sourceRecordNo || '',
    targetProvince: selectedApplication.value.targetProvince || '',
    targetSchool: selectedApplication.value.targetSchool || '',
    transferReason: selectedApplication.value.transferReason || '',
    remark: selectedApplication.value.remark || ''
  })
  materialIdsText.value = (selectedApplication.value.materialIds || []).join(',')
  formDrawerVisible.value = true
}

async function openDetailDrawer(row: TransferApplication) {
  selectedApplication.value = await getTransferApplicationDetail(row.id)
  flowRecords.value = await listTransferFlowRecords(row.id)
  detailDrawerVisible.value = true
}

function openAuditDialog(row: TransferApplication, mode: 'approve' | 'reject') {
  selectedApplication.value = row
  auditMode.value = mode
  auditForm.auditOpinion = mode === 'approve' ? '材料齐全，符合转考办理要求。' : ''
  auditDialogVisible.value = true
}

function openWithdrawDialog(row: TransferApplication) {
  selectedApplication.value = row
  withdrawForm.withdrawReason = ''
  withdrawDialogVisible.value = true
}

function handleTransferTypeChange() {
  form.sourceProvince = ''
  form.sourceSchool = ''
  form.sourceRecordNo = ''
  form.targetProvince = ''
  form.targetSchool = ''
  formRef.value?.clearValidate(['sourceProvince', 'targetProvince'])
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      recordId: form.recordId,
      transferType: form.transferType,
      sourceProvince: form.sourceProvince || undefined,
      sourceSchool: form.sourceSchool || undefined,
      sourceRecordNo: form.sourceRecordNo || undefined,
      targetProvince: form.targetProvince || undefined,
      targetSchool: form.targetSchool || undefined,
      transferReason: form.transferReason,
      materialIds: parseMaterialIds(),
      remark: form.remark || undefined
    }
    if (formMode.value === 'create') {
      await submitTransferApplication(payload)
    } else {
      await updateTransferApplication(form.id, payload)
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
      await approveTransferApplication(selectedApplication.value.id, auditForm)
    } else {
      await rejectTransferApplication(selectedApplication.value.id, auditForm)
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
    await withdrawTransferApplication(selectedApplication.value.id, withdrawForm)
    ElMessage.success('撤回成功')
    withdrawDialogVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: 0,
    recordId: undefined,
    transferType: 'TRANSFER_IN',
    sourceProvince: '',
    sourceSchool: '',
    sourceRecordNo: '',
    targetProvince: '',
    targetSchool: '',
    transferReason: '',
    remark: ''
  })
  materialIdsText.value = ''
  formRef.value?.clearValidate()
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

function transferTypeText(type?: string) {
  const typeMap: Record<string, string> = {
    TRANSFER_IN: '考籍转入',
    TRANSFER_OUT: '考籍转出'
  }
  return typeMap[type || ''] || type || '-'
}

function transferTypeTag(type?: string) {
  return type === 'TRANSFER_OUT' ? 'warning' : 'primary'
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
.transfer-page {
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

.flow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}

.flow-status,
.flow-opinion {
  color: #64748b;
  font-weight: 400;
}

.flow-opinion {
  margin: 6px 0 0;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
