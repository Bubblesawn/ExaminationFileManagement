<template>
  <section class="recognition-panel">
    <div class="panel-header">
      <div>
        <h3>智能识别结果</h3>
        <p>{{ resultTitle }}</p>
      </div>
      <div class="status-group">
        <el-tag :type="actionTagType" effect="light">{{ actionText }}</el-tag>
        <el-tag v-if="editableResult.need_manual_review" type="warning" effect="plain">需人工复核</el-tag>
      </div>
    </div>

    <el-empty v-if="!hasResult" description="暂无识别结果" />

    <template v-else>
      <el-row :gutter="16">
        <el-col :xs="24" :md="10">
          <div class="summary-block">
            <div class="preview-box">
              <img v-if="editableResult.file_url" :src="editableResult.file_url" alt="材料预览" />
              <div v-else class="preview-placeholder">未提供图片地址</div>
            </div>

            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="业务编号">{{ editableResult.business_id ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="业务场景">{{ editableResult.scene || '-' }}</el-descriptions-item>
              <el-descriptions-item label="图片质量">
                <el-tag :type="editableResult.quality?.readable ? 'success' : 'danger'" size="small">
                  {{ editableResult.quality?.readable ? '可用' : '需处理' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="editableResult.quality?.issues?.length" class="issue-list">
              <span v-for="issue in editableResult.quality.issues" :key="issue">{{ issue }}</span>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :md="14">
          <el-form label-position="top" class="confirm-form">
            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-form-item label="材料类别编码">
                  <el-input v-model="editableResult.category_code" placeholder="请输入类别编码" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="材料类别名称">
                  <el-input v-model="editableResult.category_name" placeholder="请输入类别名称" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-form-item label="识别置信度">
                  <el-input-number v-model="confidencePercent" :min="0" :max="100" :precision="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="处理结论">
                  <el-select v-model="editableResult.suggested_action" placeholder="请选择处理结论">
                    <el-option label="确认通过" value="ACCEPT" />
                    <el-option label="人工复核" value="REVIEW" />
                    <el-option label="退回处理" value="REJECT" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="人工修正说明">
              <el-input
                v-model="manualRemark"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="记录人工确认或修正原因"
              />
            </el-form-item>
          </el-form>

          <div v-if="candidateRows.length" class="result-section">
            <div class="section-title">候选类别</div>
            <el-table :data="candidateRows" size="small" border>
              <el-table-column prop="category_code" label="编码" min-width="120" />
              <el-table-column prop="category_name" label="名称" min-width="140" />
              <el-table-column label="置信度" width="120">
                <template #default="{ row }">{{ formatPercent(row.confidence) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="92">
                <template #default="{ row }">
                  <el-button text type="primary" @click="applyCandidate(row)">采用</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <div v-if="objectRows.length" class="result-section">
        <div class="section-title">关键区域检测</div>
        <el-table :data="objectRows" size="small" border>
          <el-table-column prop="object_code" label="区域编码" min-width="120" />
          <el-table-column prop="object_name" label="区域名称" min-width="140" />
          <el-table-column label="置信度" width="110">
            <template #default="{ row }">{{ formatPercent(row.confidence) }}</template>
          </el-table-column>
          <el-table-column label="风险" width="100">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.risk_level)" size="small">{{ riskText(row.risk_level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="坐标" min-width="170">
            <template #default="{ row }">{{ formatBbox(row.bbox) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" />
        </el-table>
      </div>

      <div v-if="segmentRows.length" class="result-section">
        <div class="section-title">分割区域</div>
        <el-table :data="segmentRows" size="small" border>
          <el-table-column prop="segment_code" label="区域编码" min-width="120" />
          <el-table-column prop="segment_name" label="区域名称" min-width="140" />
          <el-table-column prop="segment_type" label="类型" width="110" />
          <el-table-column label="面积占比" width="110">
            <template #default="{ row }">{{ formatPercent(row.area_ratio) }}</template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="{ row }">{{ row.extraction_priority }}</template>
          </el-table-column>
          <el-table-column label="需复核" width="90">
            <template #default="{ row }">{{ row.need_manual_review ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" />
        </el-table>
      </div>

      <div class="panel-footer">
        <el-button @click="resetResult">恢复识别值</el-button>
        <el-button type="primary" @click="confirmResult">确认结果</el-button>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type {
  AiRecognitionData,
  DetectedObject,
  MaterialCategoryCandidate,
  MaterialSegment,
  ObjectBoundingBox,
  SuggestedAction
} from '../../api/ai'

const props = defineProps<{
  result: AiRecognitionData | null
  taskName?: string
}>()

const emit = defineEmits<{
  confirm: [payload: { result: AiRecognitionData; remark: string }]
}>()

const editableResult = ref<AiRecognitionData>({})
const manualRemark = ref('')

const hasResult = computed(() => Boolean(props.result))
const resultTitle = computed(() => props.taskName || '请先发起图片识别')
const candidateRows = computed<MaterialCategoryCandidate[]>(() => editableResult.value.candidates ?? [])
const objectRows = computed<DetectedObject[]>(() => editableResult.value.objects ?? [])
const segmentRows = computed<MaterialSegment[]>(() => editableResult.value.segments ?? [])

const confidencePercent = computed({
  get: () => Number(((editableResult.value.confidence ?? 0) * 100).toFixed(1)),
  set: (value: number | undefined) => {
    editableResult.value.confidence = Number(((value ?? 0) / 100).toFixed(4))
  }
})

const actionTextMap: Record<SuggestedAction, string> = {
  ACCEPT: '建议通过',
  REVIEW: '建议复核',
  REJECT: '建议退回'
}

const actionText = computed(() => actionTextMap[editableResult.value.suggested_action ?? 'REVIEW'])
const actionTagType = computed(() => {
  const action = editableResult.value.suggested_action
  if (action === 'ACCEPT') return 'success'
  if (action === 'REJECT') return 'danger'
  return 'warning'
})

watch(
  () => props.result,
  (value) => {
    editableResult.value = cloneResult(value)
    manualRemark.value = ''
  },
  { immediate: true, deep: true }
)

/**
 * @brief 深拷贝识别结果，隔离父组件原始算法返回值。
 *
 * @param value 原始识别结果。
 * @return 可供表单编辑的识别结果副本。
 */
function cloneResult(value: AiRecognitionData | null): AiRecognitionData {
  return value ? JSON.parse(JSON.stringify(value)) : {}
}

function applyCandidate(candidate: MaterialCategoryCandidate) {
  editableResult.value.category_code = candidate.category_code
  editableResult.value.category_name = candidate.category_name
  editableResult.value.confidence = candidate.confidence
}

function resetResult() {
  editableResult.value = cloneResult(props.result)
  manualRemark.value = ''
}

function confirmResult() {
  emit('confirm', {
    result: cloneResult(editableResult.value),
    remark: manualRemark.value.trim()
  })
}

function formatPercent(value?: number) {
  return `${Number(((value ?? 0) * 100).toFixed(1))}%`
}

function formatBbox(bbox?: ObjectBoundingBox) {
  if (!bbox) return '-'
  return `x:${bbox.x} y:${bbox.y} w:${bbox.width} h:${bbox.height}`
}

function riskText(level: DetectedObject['risk_level']) {
  const map: Record<DetectedObject['risk_level'], string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }
  return map[level]
}

function riskTagType(level: DetectedObject['risk_level']) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}
</script>

<style scoped>
.recognition-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.panel-header,
.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-header {
  margin-bottom: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.panel-header p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.status-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.summary-block {
  display: grid;
  gap: 12px;
}

.preview-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.preview-box img {
  display: block;
  width: 100%;
  max-height: 260px;
  object-fit: contain;
}

.preview-placeholder {
  color: #64748b;
  font-size: 13px;
}

.issue-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.issue-list span {
  padding: 4px 8px;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  font-size: 12px;
}

.confirm-form :deep(.el-input-number),
.confirm-form :deep(.el-select) {
  width: 100%;
}

.result-section {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.panel-footer {
  margin-top: 18px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .panel-header,
  .panel-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .status-group {
    justify-content: flex-start;
  }
}
</style>
