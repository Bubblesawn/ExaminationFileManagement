<template>
  <div class="page course-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">课程顶替</h2>
        <p class="page-subtitle">维护课程顶替规则，办理申请、审核和详情追踪。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Plus" @click="openRuleDrawer('create')">新增规则</el-button>
        <el-button type="primary" :icon="Plus" @click="openApplicationDrawer('create')">新增申请</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="course-tabs">
      <el-tab-pane label="规则维护" name="rules">
        <el-card shadow="never" class="query-card">
          <el-form :inline="true" class="query-form" @submit.prevent>
            <el-form-item label="关键字">
              <el-input v-model="ruleQuery.keyword" placeholder="课程、专业代码" clearable @keyup.enter="searchRules" />
            </el-form-item>
            <el-form-item label="规则状态">
              <el-select v-model="ruleQuery.ruleStatus" clearable placeholder="全部">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchRules">查询</el-button>
              <el-button :icon="Refresh" @click="resetRuleQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <el-table v-loading="ruleLoading" :data="rules" border>
            <el-table-column prop="sourceCourseCode" label="原课程代码" min-width="120" fixed="left" />
            <el-table-column prop="sourceCourseName" label="原课程名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="targetCourseCode" label="顶替课程代码" min-width="120" />
            <el-table-column prop="targetCourseName" label="顶替课程名称" min-width="190" show-overflow-tooltip />
            <el-table-column prop="majorCode" label="专业代码" min-width="120">
              <template #default="{ row }">{{ row.majorCode || '通用' }}</template>
            </el-table-column>
            <el-table-column prop="educationLevel" label="学历层次" min-width="110">
              <template #default="{ row }">{{ educationLevelText(row.educationLevel) }}</template>
            </el-table-column>
            <el-table-column prop="credit" label="学分" width="90" align="center">
              <template #default="{ row }">{{ row.credit ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="有效期" min-width="210" show-overflow-tooltip>
              <template #default="{ row }">{{ formatRuleDateRange(row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="row.ruleStatus === 'ENABLED' ? 'success' : 'info'">
                  {{ ruleStatusText(row.ruleStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" @click="openRuleDrawer('edit', row)">编辑</el-button>
                <el-button
                  link
                  :type="row.ruleStatus === 'ENABLED' ? 'warning' : 'success'"
                  :icon="SwitchButton"
                  @click="toggleRuleStatus(row)"
                >
                  {{ row.ruleStatus === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="primary" :icon="DocumentCopy" @click="useRuleForApplication(row)">
                  申请
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="ruleQuery.pageNo"
              v-model:page-size="ruleQuery.pageSize"
              :total="ruleTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadRules"
              @current-change="loadRules"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="申请审核" name="applications">
        <el-card shadow="never" class="query-card">
          <el-form :inline="true" class="query-form" @submit.prevent>
            <el-form-item label="关键字">
              <el-input
                v-model="applicationQuery.keyword"
                placeholder="申请编号、姓名、课程"
                clearable
                @keyup.enter="searchApplications"
              />
            </el-form-item>
            <el-form-item label="申请状态">
              <el-select v-model="applicationQuery.applicationStatus" clearable placeholder="全部">
                <el-option label="已提交" value="SUBMITTED" />
                <el-option label="审核通过" value="APPROVED" />
                <el-option label="审核驳回" value="REJECTED" />
                <el-option label="已撤回" value="WITHDRAWN" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchApplications">查询</el-button>
              <el-button :icon="Refresh" @click="resetApplicationQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <el-table v-loading="applicationLoading" :data="applications" border>
            <el-table-column prop="applicationNo" label="申请编号" min-width="170" fixed="left" show-overflow-tooltip />
            <el-table-column prop="candidateName" label="考生姓名" min-width="110">
              <template #default="{ row }">{{ row.candidateName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="recordNo" label="考籍号" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.recordNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="原课程" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">{{ courseLabel(row.sourceCourseCode, row.sourceCourseName) }}</template>
            </el-table-column>
            <el-table-column label="顶替课程" min-width="210" show-overflow-tooltip>
              <template #default="{ row }">{{ courseLabel(row.targetCourseCode, row.targetCourseName) }}</template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" min-width="170" show-overflow-tooltip />
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="applicationStatusTag(row.applicationStatus)">
                  {{ applicationStatusText(row.applicationStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="340" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="openDetailDrawer(row)">详情</el-button>
                <el-button
                  v-if="row.applicationStatus === 'SUBMITTED'"
                  link
                  type="primary"
                  :icon="Edit"
                  @click="openApplicationDrawer('edit', row)"
                >
                  编辑
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
                <el-button
                  v-if="row.applicationStatus === 'SUBMITTED'"
                  link
                  type="warning"
                  :icon="Back"
                  @click="openWithdrawDialog(row)"
                >
                  撤回
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="applicationQuery.pageNo"
              v-model:page-size="applicationQuery.pageSize"
              :total="applicationTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadApplications"
              @current-change="loadApplications"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="ruleDrawerVisible" :title="ruleFormMode === 'create' ? '新增课程顶替规则' : '编辑课程顶替规则'" size="680px">
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleFormRules" label-width="112px">
        <el-form-item label="原课程代码" prop="sourceCourseCode">
          <el-input v-model="ruleForm.sourceCourseCode" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="原课程名称" prop="sourceCourseName">
          <el-input v-model="ruleForm.sourceCourseName" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="顶替课程代码" prop="targetCourseCode">
          <el-input v-model="ruleForm.targetCourseCode" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="顶替课程名称" prop="targetCourseName">
          <el-input v-model="ruleForm.targetCourseName" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="适用专业">
          <el-input v-model="ruleForm.majorCode" placeholder="为空表示通用" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="学历层次">
          <el-select v-model="ruleForm.educationLevel" clearable placeholder="为空表示不限">
            <el-option label="专科" value="JUNIOR_COLLEGE" />
            <el-option label="本科" value="UNDERGRADUATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="ruleForm.credit" :min="0.5" :precision="1" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="ruleForm.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" />
        </el-form-item>
        <el-form-item label="失效日期">
          <el-date-picker v-model="ruleForm.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="长期有效" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ruleForm.remark" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRuleForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer
      v-model="applicationDrawerVisible"
      :title="applicationFormMode === 'create' ? '新增课程顶替申请' : '编辑课程顶替申请'"
      size="720px"
    >
      <el-form ref="applicationFormRef" :model="applicationForm" :rules="applicationFormRules" label-width="112px">
        <el-form-item v-if="applicationFormMode === 'create'" label="考籍档案ID" prop="recordId">
          <el-input-number v-model="applicationForm.recordId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="顶替规则ID" prop="ruleId">
          <el-input-number v-model="applicationForm.ruleId" :min="1" controls-position="right" />
          <el-button class="inline-action" :icon="View" @click="previewSelectedRule">查看规则</el-button>
        </el-form-item>
        <el-alert
          v-if="selectedRuleSummary"
          class="rule-alert"
          type="info"
          :title="selectedRuleSummary"
          show-icon
          :closable="false"
        />
        <el-form-item label="申请原因">
          <el-input v-model="applicationForm.applyReason" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="材料ID">
          <el-input v-model="materialIdsText" placeholder="多个材料ID用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="applicationForm.remark" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applicationDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitApplicationForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="课程顶替申请详情" size="760px">
      <template v-if="selectedApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ selectedApplication.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ applicationStatusText(selectedApplication.applicationStatus) }}</el-descriptions-item>
          <el-descriptions-item label="考生姓名">{{ selectedApplication.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考籍号">{{ selectedApplication.recordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原课程" :span="2">
            {{ courseLabel(selectedApplication.sourceCourseCode, selectedApplication.sourceCourseName) }}
          </el-descriptions-item>
          <el-descriptions-item label="顶替课程" :span="2">
            {{ courseLabel(selectedApplication.targetCourseCode, selectedApplication.targetCourseName) }}
          </el-descriptions-item>
          <el-descriptions-item label="适用专业">{{ selectedApplication.majorCode || '通用' }}</el-descriptions-item>
          <el-descriptions-item label="学历层次">{{ educationLevelText(selectedApplication.educationLevel) }}</el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ selectedApplication.applyReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="材料ID" :span="2">
            {{ (selectedApplication.materialIds || []).join(', ') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">
            {{ selectedApplication.auditOpinion || '-' }}
          </el-descriptions-item>
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
            {{ flowActionText(record.auditAction) }}：{{ record.auditOpinion || '-' }}
            <span class="timeline-operator">经办人：{{ record.auditorName || '-' }}</span>
          </el-timeline-item>
        </el-timeline>

        <BusinessMaterialList :business-no="selectedApplication.applicationNo" />
        <ApplicationMaterialAuditPanel :application-id="selectedApplication.id" />
      </template>
    </el-drawer>

    <el-dialog v-model="auditDialogVisible" :title="auditMode === 'approve' ? '审核通过' : '审核驳回'" width="880px">
      <ApplicationMaterialAuditPanel
        v-if="selectedApplication"
        :application-id="selectedApplication.id"
        title="审核前智能核验"
        description="审核课程顶替申请前，查看材料分类、缺失项和异常风险。"
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  CircleCheck,
  CircleClose,
  DocumentCopy,
  Edit,
  Plus,
  Refresh,
  Search,
  SwitchButton,
  View
} from '@element-plus/icons-vue'
import ApplicationMaterialAuditPanel from '../../components/ai/ApplicationMaterialAuditPanel.vue'
import BusinessMaterialList from '../../components/material/BusinessMaterialList.vue'
import {
  approveCourseReplacementApplication,
  createCourseReplacementRule,
  getCourseReplacementApplicationDetail,
  getCourseReplacementRuleDetail,
  listCourseReplacementFlowRecords,
  pageCourseReplacementApplications,
  pageCourseReplacementRules,
  rejectCourseReplacementApplication,
  submitCourseReplacementApplication,
  updateCourseReplacementApplication,
  updateCourseReplacementRule,
  updateCourseReplacementRuleStatus,
  withdrawCourseReplacementApplication,
  type CourseReplacementApplication,
  type CourseReplacementFlowRecord,
  type CourseReplacementRule
} from '../../api/course'

const activeTab = ref<'rules' | 'applications'>('rules')
const ruleLoading = ref(false)
const applicationLoading = ref(false)
const saving = ref(false)
const rules = ref<CourseReplacementRule[]>([])
const applications = ref<CourseReplacementApplication[]>([])
const ruleTotal = ref(0)
const applicationTotal = ref(0)
const ruleDrawerVisible = ref(false)
const applicationDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const auditDialogVisible = ref(false)
const withdrawDialogVisible = ref(false)
const ruleFormMode = ref<'create' | 'edit'>('create')
const applicationFormMode = ref<'create' | 'edit'>('create')
const auditMode = ref<'approve' | 'reject'>('approve')
const selectedApplication = ref<CourseReplacementApplication | null>(null)
const selectedRule = ref<CourseReplacementRule | null>(null)
const flowRecords = ref<CourseReplacementFlowRecord[]>([])
const ruleFormRef = ref<FormInstance>()
const applicationFormRef = ref<FormInstance>()
const materialIdsText = ref('')

const ruleQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  ruleStatus: ''
})

const applicationQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  applicationStatus: ''
})

const ruleForm = reactive({
  id: 0,
  sourceCourseCode: '',
  sourceCourseName: '',
  targetCourseCode: '',
  targetCourseName: '',
  majorCode: '',
  educationLevel: '',
  credit: undefined as number | undefined,
  effectiveDate: '',
  expireDate: '',
  remark: ''
})

const applicationForm = reactive({
  id: 0,
  recordId: undefined as number | undefined,
  ruleId: undefined as number | undefined,
  applyReason: '',
  remark: ''
})

const auditForm = reactive({
  auditOpinion: ''
})

const withdrawForm = reactive({
  withdrawReason: ''
})

const ruleFormRules: FormRules = {
  sourceCourseCode: [{ required: true, message: '请输入原课程代码', trigger: 'blur' }],
  sourceCourseName: [{ required: true, message: '请输入原课程名称', trigger: 'blur' }],
  targetCourseCode: [{ required: true, message: '请输入顶替课程代码', trigger: 'blur' }],
  targetCourseName: [{ required: true, message: '请输入顶替课程名称', trigger: 'blur' }]
}

const applicationFormRules: FormRules = {
  recordId: [{ required: true, message: '请输入考籍档案ID', trigger: 'blur' }],
  ruleId: [{ required: true, message: '请输入顶替规则ID', trigger: 'blur' }]
}

const selectedRuleSummary = computed(() => {
  if (!selectedRule.value) {
    return ''
  }
  return `${courseLabel(selectedRule.value.sourceCourseCode, selectedRule.value.sourceCourseName)} 可顶替 ${courseLabel(
    selectedRule.value.targetCourseCode,
    selectedRule.value.targetCourseName
  )}`
})

watch(activeTab, (tab) => {
  if (tab === 'rules' && rules.value.length === 0) {
    loadRules()
  }
  if (tab === 'applications' && applications.value.length === 0) {
    loadApplications()
  }
})

/**
 * @brief 加载课程顶替规则分页列表。
 */
async function loadRules() {
  ruleLoading.value = true
  try {
    const result = await pageCourseReplacementRules({
      pageNo: ruleQuery.pageNo,
      pageSize: ruleQuery.pageSize,
      keyword: ruleQuery.keyword || undefined,
      ruleStatus: ruleQuery.ruleStatus || undefined
    })
    rules.value = result.records ?? []
    ruleTotal.value = result.total ?? 0
  } finally {
    ruleLoading.value = false
  }
}

/**
 * @brief 加载课程顶替申请分页列表。
 */
async function loadApplications() {
  applicationLoading.value = true
  try {
    const result = await pageCourseReplacementApplications({
      pageNo: applicationQuery.pageNo,
      pageSize: applicationQuery.pageSize,
      keyword: applicationQuery.keyword || undefined,
      applicationStatus: applicationQuery.applicationStatus || undefined
    })
    applications.value = result.records ?? []
    applicationTotal.value = result.total ?? 0
  } finally {
    applicationLoading.value = false
  }
}

function searchRules() {
  ruleQuery.pageNo = 1
  loadRules()
}

function resetRuleQuery() {
  ruleQuery.pageNo = 1
  ruleQuery.keyword = ''
  ruleQuery.ruleStatus = ''
  loadRules()
}

function searchApplications() {
  applicationQuery.pageNo = 1
  loadApplications()
}

function resetApplicationQuery() {
  applicationQuery.pageNo = 1
  applicationQuery.keyword = ''
  applicationQuery.applicationStatus = ''
  loadApplications()
}

async function openRuleDrawer(mode: 'create' | 'edit', row?: CourseReplacementRule) {
  ruleFormMode.value = mode
  selectedRule.value = row ? await getCourseReplacementRuleDetail(row.id) : null
  Object.assign(ruleForm, {
    id: selectedRule.value?.id ?? 0,
    sourceCourseCode: selectedRule.value?.sourceCourseCode ?? '',
    sourceCourseName: selectedRule.value?.sourceCourseName ?? '',
    targetCourseCode: selectedRule.value?.targetCourseCode ?? '',
    targetCourseName: selectedRule.value?.targetCourseName ?? '',
    majorCode: selectedRule.value?.majorCode ?? '',
    educationLevel: selectedRule.value?.educationLevel ?? '',
    credit: selectedRule.value?.credit,
    effectiveDate: selectedRule.value?.effectiveDate ?? '',
    expireDate: selectedRule.value?.expireDate ?? '',
    remark: selectedRule.value?.remark ?? ''
  })
  ruleDrawerVisible.value = true
}

async function openApplicationDrawer(mode: 'create' | 'edit', row?: CourseReplacementApplication) {
  applicationFormMode.value = mode
  selectedRule.value = null
  selectedApplication.value = row ? await getCourseReplacementApplicationDetail(row.id) : null
  Object.assign(applicationForm, {
    id: selectedApplication.value?.id ?? 0,
    recordId: selectedApplication.value?.recordId,
    ruleId: selectedApplication.value?.ruleId,
    applyReason: selectedApplication.value?.applyReason ?? '',
    remark: selectedApplication.value?.remark ?? ''
  })
  materialIdsText.value = (selectedApplication.value?.materialIds ?? []).join(',')
  if (applicationForm.ruleId) {
    await loadSelectedRule(applicationForm.ruleId, false)
  }
  applicationDrawerVisible.value = true
}

async function useRuleForApplication(rule: CourseReplacementRule) {
  activeTab.value = 'applications'
  await openApplicationDrawer('create')
  applicationForm.ruleId = rule.id
  selectedRule.value = rule
}

async function previewSelectedRule() {
  if (!applicationForm.ruleId) {
    ElMessage.warning('请先输入顶替规则ID')
    return
  }
  await loadSelectedRule(applicationForm.ruleId, true)
}

async function loadSelectedRule(ruleId: number, showMessage: boolean) {
  selectedRule.value = await getCourseReplacementRuleDetail(ruleId)
  if (showMessage) {
    ElMessage.success('规则信息已加载')
  }
}

/**
 * @brief 保存课程顶替规则表单。
 */
async function submitRuleForm() {
  await ruleFormRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      sourceCourseCode: ruleForm.sourceCourseCode,
      sourceCourseName: ruleForm.sourceCourseName,
      targetCourseCode: ruleForm.targetCourseCode,
      targetCourseName: ruleForm.targetCourseName,
      majorCode: ruleForm.majorCode || undefined,
      educationLevel: ruleForm.educationLevel || undefined,
      credit: ruleForm.credit,
      effectiveDate: ruleForm.effectiveDate || undefined,
      expireDate: ruleForm.expireDate || undefined,
      remark: ruleForm.remark || undefined
    }
    if (ruleFormMode.value === 'create') {
      await createCourseReplacementRule(payload)
    } else {
      await updateCourseReplacementRule(ruleForm.id, payload)
    }
    ElMessage.success('规则保存成功')
    ruleDrawerVisible.value = false
    loadRules()
  } finally {
    saving.value = false
  }
}

/**
 * @brief 启用或停用课程顶替规则。
 *
 * @param row 当前规则行。
 */
async function toggleRuleStatus(row: CourseReplacementRule) {
  const nextStatus = row.ruleStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = nextStatus === 'ENABLED' ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText}该课程顶替规则吗？`, '规则状态确认', { type: 'warning' })
  await updateCourseReplacementRuleStatus(row.id, nextStatus)
  ElMessage.success(`${actionText}成功`)
  loadRules()
}

/**
 * @brief 保存课程顶替申请表单。
 */
async function submitApplicationForm() {
  await applicationFormRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      recordId: applicationForm.recordId,
      ruleId: applicationForm.ruleId,
      applyReason: applicationForm.applyReason || undefined,
      materialIds: parseMaterialIds(),
      remark: applicationForm.remark || undefined
    }
    if (applicationFormMode.value === 'create') {
      await submitCourseReplacementApplication(payload)
    } else {
      await updateCourseReplacementApplication(applicationForm.id, payload)
    }
    ElMessage.success('申请保存成功')
    applicationDrawerVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

async function openDetailDrawer(row: CourseReplacementApplication) {
  selectedApplication.value = await getCourseReplacementApplicationDetail(row.id)
  flowRecords.value = await listCourseReplacementFlowRecords(row.id)
  detailDrawerVisible.value = true
}

function openAuditDialog(row: CourseReplacementApplication, mode: 'approve' | 'reject') {
  selectedApplication.value = row
  auditMode.value = mode
  auditForm.auditOpinion = mode === 'approve' ? '材料完整，符合课程顶替规则。' : ''
  auditDialogVisible.value = true
}

function openWithdrawDialog(row: CourseReplacementApplication) {
  selectedApplication.value = row
  withdrawForm.withdrawReason = ''
  withdrawDialogVisible.value = true
}

/**
 * @brief 提交课程顶替审核结果。
 */
async function submitAudit() {
  if (!selectedApplication.value) return
  if (!auditForm.auditOpinion.trim()) {
    ElMessage.warning('请输入审核意见')
    return
  }
  saving.value = true
  try {
    if (auditMode.value === 'approve') {
      await approveCourseReplacementApplication(selectedApplication.value.id, auditForm)
    } else {
      await rejectCourseReplacementApplication(selectedApplication.value.id, auditForm)
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
    await withdrawCourseReplacementApplication(selectedApplication.value.id, withdrawForm)
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

function courseLabel(code?: string, name?: string) {
  if (!code && !name) {
    return '-'
  }
  return [code, name].filter(Boolean).join(' ')
}

function formatRuleDateRange(rule: CourseReplacementRule) {
  const start = rule.effectiveDate || '立即生效'
  const end = rule.expireDate || '长期有效'
  return `${start} 至 ${end}`
}

function ruleStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    ENABLED: '启用',
    DISABLED: '停用'
  }
  return statusMap[status || ''] || status || '-'
}

function educationLevelText(level?: string) {
  const levelMap: Record<string, string> = {
    JUNIOR_COLLEGE: '专科',
    UNDERGRADUATE: '本科'
  }
  return levelMap[level || ''] || level || '不限'
}

function applicationStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    SUBMITTED: '已提交',
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

onMounted(() => {
  loadRules()
  loadApplications()
})
</script>

<style scoped>
.course-page {
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

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.course-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.query-card,
.table-card {
  border-radius: 8px;
}

.query-card {
  margin-bottom: 14px;
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

.rule-alert {
  margin: 0 0 18px 112px;
  width: calc(100% - 112px);
}

.section-title {
  margin: 18px 0 12px;
  font-size: 16px;
}

.timeline-operator {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .rule-alert {
    margin-left: 0;
    width: 100%;
  }
}
</style>
