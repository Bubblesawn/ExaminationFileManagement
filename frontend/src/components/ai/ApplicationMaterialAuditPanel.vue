<template>
  <section class="application-audit-panel">
    <div class="audit-header">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="header-actions">
        <el-tag :type="actionTagType(auditResult?.suggested_action ?? 'REVIEW')" effect="light">
          {{ actionText(auditResult?.suggested_action ?? 'REVIEW') }}
        </el-tag>
        <el-button type="primary" plain :loading="loading" @click="loadAuditResult">智能核验</el-button>
      </div>
    </div>

    <el-alert
      v-if="!applicationId"
      class="state-alert"
      title="请先选择业务申请，再发起材料智能核验。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-empty v-else-if="!auditResult" description="点击智能核验后展示材料分类、缺失材料和异常提醒" />

    <template v-else>
      <div class="metric-grid">
        <div class="metric-item">
          <span>已绑定材料</span>
          <strong>{{ auditResult.summary.material_count ?? 0 }}</strong>
        </div>
        <div class="metric-item danger">
          <span>缺失材料</span>
          <strong>{{ auditResult.summary.missing_count ?? 0 }}</strong>
        </div>
        <div class="metric-item warning">
          <span>异常提醒</span>
          <strong>{{ auditResult.summary.abnormal_count ?? 0 }}</strong>
        </div>
        <div class="metric-item">
          <span>需复核</span>
          <strong>{{ auditResult.summary.manual_review_count ?? 0 }}</strong>
        </div>
      </div>

      <div class="result-section">
        <div class="section-title">缺失材料提示</div>
        <el-alert
          v-if="auditResult.missing_materials.length === 0"
          title="必交材料已齐全"
          type="success"
          show-icon
          :closable="false"
        />
        <el-table v-else :data="auditResult.missing_materials" size="small" border>
          <el-table-column prop="category_name" label="缺失材料" min-width="140" />
          <el-table-column label="等级" width="90">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.severity)" size="small">{{ riskText(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="提示" min-width="220" />
        </el-table>
      </div>

      <div class="result-section">
        <div class="section-title">异常材料提醒</div>
        <el-alert
          v-if="auditResult.abnormal_materials.length === 0"
          title="暂未发现异常材料"
          type="success"
          show-icon
          :closable="false"
        />
        <el-table v-else :data="auditResult.abnormal_materials" size="small" border>
          <el-table-column prop="category_name" label="材料类别" min-width="130" />
          <el-table-column prop="abnormal_type" label="异常类型" min-width="140" />
          <el-table-column label="风险" width="90">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.risk_level)" size="small">{{ riskText(row.risk_level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="异常说明" min-width="220" />
          <el-table-column prop="suggestion" label="处理建议" min-width="220" />
        </el-table>
      </div>

      <div class="result-section">
        <div class="section-title">材料分类结果</div>
        <el-table :data="auditResult.classified_materials" size="small" border>
          <el-table-column prop="file_name" label="文件名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="category_name" label="识别类别" min-width="130" />
          <el-table-column label="置信度" width="100">
            <template #default="{ row }">{{ formatPercent(row.confidence) }}</template>
          </el-table-column>
          <el-table-column label="图片质量" width="100">
            <template #default="{ row }">
              <el-tag :type="row.quality.readable ? 'success' : 'danger'" size="small">
                {{ row.quality.readable ? '可用' : '需处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="建议" width="110">
            <template #default="{ row }">
              <el-tag :type="actionTagType(row.suggested_action)" size="small">
                {{ actionText(row.suggested_action) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="audit-footer">
        <el-button @click="copyAuditSummary">复制核验摘要</el-button>
        <el-button type="primary" @click="confirmAuditResult">确认核验结果</el-button>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  auditApplicationMaterialsByApplicationId,
  type ApplicationMaterialAuditData,
  type SuggestedAction
} from '../../api/ai'

type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

const props = withDefaults(
  defineProps<{
    applicationId?: number
    title?: string
    description?: string
    autoLoad?: boolean
  }>(),
  {
    title: '智能辅助核验',
    description: '联调业务流程与智能辅助结果，辅助审核材料完整性和异常风险。',
    autoLoad: false
  }
)

const actionTextMap: Record<SuggestedAction, string> = {
  ACCEPT: '建议通过',
  REVIEW: '建议复核',
  REJECT: '建议退回'
}

const loading = ref(false)
const auditResult = ref<ApplicationMaterialAuditData | null>(null)

watch(
  () => props.applicationId,
  (value) => {
    auditResult.value = null
    if (value && props.autoLoad) {
      loadAuditResult()
    }
  }
)

/**
 * @brief 按业务申请 ID 发起材料智能核验，并将算法结果展示到当前流程页面。
 *
 * @details
 * 该方法调用后端自动组装材料的联调接口，避免前端重复拼接材料明细；
 * 审核人员可在详情页或审核弹窗中直接查看缺失材料、异常材料和分类建议。
 */
async function loadAuditResult() {
  if (!props.applicationId) {
    ElMessage.warning('请先选择业务申请')
    return
  }

  loading.value = true
  try {
    const response = await auditApplicationMaterialsByApplicationId(props.applicationId)
    if (response.code !== 200) {
      ElMessage.error(response.message || '申请材料智能核验失败')
      return
    }
    auditResult.value = response.data
    ElMessage.success('智能辅助核验完成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能辅助服务暂不可用')
  } finally {
    loading.value = false
  }
}

/**
 * @brief 人工确认当前智能核验结果。
 */
function confirmAuditResult() {
  ElMessage.success('核验结果已确认，可继续办理业务审核')
}

async function copyAuditSummary() {
  if (!auditResult.value) return
  const summary = `申请类型：${auditResult.value.application_type}；材料：${auditResult.value.summary.material_count ?? 0}；缺失：${auditResult.value.summary.missing_count ?? 0}；异常：${auditResult.value.summary.abnormal_count ?? 0}；建议：${actionText(auditResult.value.suggested_action)}`
  try {
    await navigator.clipboard.writeText(summary)
    ElMessage.success('核验摘要已复制')
  } catch {
    ElMessage.info(summary)
  }
}

function actionText(action: SuggestedAction) {
  return actionTextMap[action]
}

function actionTagType(action: SuggestedAction) {
  if (action === 'ACCEPT') return 'success'
  if (action === 'REJECT') return 'danger'
  return 'warning'
}

function riskText(level: RiskLevel) {
  const map: Record<RiskLevel, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }
  return map[level]
}

function riskTagType(level: RiskLevel) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

function formatPercent(value?: number) {
  return `${Number(((value ?? 0) * 100).toFixed(1))}%`
}
</script>

<style scoped>
.application-audit-panel {
  min-width: 0;
  margin-top: 18px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.audit-header,
.header-actions,
.audit-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.audit-header {
  justify-content: space-between;
  margin-bottom: 14px;
}

.audit-header h3 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.audit-header p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
}

.header-actions,
.audit-footer {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.state-alert {
  margin-top: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.metric-item span {
  display: block;
  color: #667085;
  font-size: 13px;
}

.metric-item strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 24px;
}

.metric-item.warning strong {
  color: #b45309;
}

.metric-item.danger strong {
  color: #b42318;
}

.result-section {
  min-width: 0;
  margin-top: 16px;
  overflow-x: auto;
}

.section-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.audit-footer {
  margin-top: 16px;
}

@media (max-width: 768px) {
  .audit-header,
  .audit-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
