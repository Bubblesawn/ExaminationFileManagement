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
              <div v-if="previewImageUrl" class="preview-image-stage">
                <img ref="previewImageRef" :src="previewImageUrl" alt="材料预览" @load="updatePreviewMetrics" />
                <div v-if="detectionBoxStyles.length" class="detection-overlay" aria-label="目标检测标注">
                  <div
                    v-for="box in detectionBoxStyles"
                    :key="box.key"
                    class="detection-box"
                    :style="box.style"
                    :title="box.title"
                  >
                    <span class="detection-label">{{ box.label }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="preview-placeholder">未提供图片地址</div>
            </div>
            <div v-if="isSegmentResult && segmentationImageUrl" class="preview-actions">
              <el-tag type="success" effect="light">分割结果图</el-tag>
              <el-button text type="primary" @click="openPreviewImage">预览大图</el-button>
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
      <el-alert
        v-else-if="editableResult.segmentation_image_url === null && resultTitle === '图像分割'"
        class="result-section"
        type="warning"
        show-icon
        title="未检测到可分割材料区域，请上传单张、清晰的材料图片后重试。"
      />

      <div class="panel-footer">
        <el-button @click="resetResult">恢复识别值</el-button>
        <el-button type="primary" @click="confirmResult">确认结果</el-button>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const previewImageRef = ref<HTMLImageElement | null>(null)
const previewMetrics = ref({ displayWidth: 0, displayHeight: 0, naturalWidth: 0, naturalHeight: 0 })

const hasResult = computed(() => Boolean(props.result))
const resultTitle = computed(() => props.taskName || '请先发起图片识别')
const candidateRows = computed<MaterialCategoryCandidate[]>(() => editableResult.value.candidates ?? [])
const objectRows = computed<DetectedObject[]>(() => editableResult.value.objects ?? [])
const segmentRows = computed<MaterialSegment[]>(() => editableResult.value.segments ?? [])
const isSegmentResult = computed(() => segmentRows.value.length > 0)
const segmentationImageUrl = computed(() => buildPreviewImageUrl(editableResult.value.segmentation_image_url))
const previewImageUrl = computed(() => segmentationImageUrl.value || buildPreviewImageUrl(editableResult.value.file_url))
const detectionBoxStyles = computed(() => {
  const { displayWidth, displayHeight, naturalWidth, naturalHeight } = previewMetrics.value
  if (!displayWidth || !displayHeight || !naturalWidth || !naturalHeight) return []

  return objectRows.value
    .filter((item) => item.bbox)
    .map((item, index) => {
      const color = detectionColor(item.object_code, index)
      const left = clamp((item.bbox.x / naturalWidth) * displayWidth, 0, displayWidth)
      const top = clamp((item.bbox.y / naturalHeight) * displayHeight, 0, displayHeight)
      const width = clamp((item.bbox.width / naturalWidth) * displayWidth, 1, displayWidth - left)
      const height = clamp((item.bbox.height / naturalHeight) * displayHeight, 1, displayHeight - top)

      return {
        key: `${item.object_code}-${index}-${item.bbox.x}-${item.bbox.y}`,
        label: item.object_name,
        title: `${item.object_name} ${formatPercent(item.confidence)} ${formatBbox(item.bbox)}`,
        style: {
          left: `${left}px`,
          top: `${top}px`,
          width: `${width}px`,
          height: `${height}px`,
          borderColor: color,
          '--detect-color': color
        }
      }
    })
})

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
    nextTick(updatePreviewMetrics)
  },
  { immediate: true, deep: true }
)

onMounted(() => {
  window.addEventListener('resize', updatePreviewMetrics)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updatePreviewMetrics)
})

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

/**
 * @brief 在新标签页中打开当前预览图片，便于审核人员查看分割细节。
 */
function openPreviewImage() {
  if (previewImageUrl.value) {
    window.open(previewImageUrl.value, '_blank', 'noopener,noreferrer')
  }
}

/**
 * @brief 刷新材料预览图的真实尺寸和当前显示尺寸。
 *
 * @details
 * 目标检测坐标来自原图像素空间，预览图会按容器缩放显示；此函数用于建立原图坐标到页面像素坐标的换算关系。
 */
function updatePreviewMetrics() {
  const image = previewImageRef.value
  if (!image) {
    previewMetrics.value = { displayWidth: 0, displayHeight: 0, naturalWidth: 0, naturalHeight: 0 }
    return
  }

  previewMetrics.value = {
    displayWidth: image.clientWidth,
    displayHeight: image.clientHeight,
    naturalWidth: image.naturalWidth,
    naturalHeight: image.naturalHeight
  }
}

/**
 * @brief 生成可在当前前端页面中加载的材料预览地址。
 *
 * @details
 * 后端返回的上传地址是根路径形式，开发环境需要通过 Vite 的 /uploads 代理转发到后端；
 * 线上若返回完整地址则直接使用，避免破坏已有部署。
 *
 * @param fileUrl 后端或算法服务返回的材料文件地址。
 * @return 可绑定到 img src 的图片访问地址。
 */
function buildPreviewImageUrl(fileUrl?: string) {
  if (!fileUrl) return ''
  if (/^(https?:)?\/\//i.test(fileUrl) || fileUrl.startsWith('blob:') || fileUrl.startsWith('data:')) {
    return fileUrl
  }
  return fileUrl.startsWith('/') ? fileUrl : `/${fileUrl}`
}

function formatPercent(value?: number) {
  return `${Number(((value ?? 0) * 100).toFixed(1))}%`
}

function formatBbox(bbox?: ObjectBoundingBox) {
  if (!bbox) return '-'
  return `x:${bbox.x} y:${bbox.y} w:${bbox.width} h:${bbox.height}`
}

/**
 * @brief 按目标类别生成稳定的检测框颜色。
 *
 * @param objectCode 目标区域编码。
 * @param index 同类或无编码目标的兜底序号。
 * @return 用于边框和标签的十六进制颜色值。
 */
function detectionColor(objectCode: string, index: number) {
  const palette = ['#dc2626', '#2563eb', '#16a34a', '#ca8a04', '#7c3aed', '#0891b2', '#db2777', '#ea580c']
  const seed = Array.from(objectCode || `${index}`).reduce((sum, char) => sum + char.charCodeAt(0), index)
  return palette[seed % palette.length]
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(value, max))
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

.preview-image-stage {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  max-height: 260px;
}

.preview-box img {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 260px;
  object-fit: contain;
}

.preview-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.detection-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.detection-box {
  position: absolute;
  border: 2px solid var(--detect-color);
  box-shadow: 0 0 0 1px rgb(255 255 255 / 82%), 0 6px 14px rgb(15 23 42 / 16%);
}

.detection-label {
  position: absolute;
  left: -2px;
  bottom: 100%;
  max-width: 180px;
  padding: 2px 6px;
  overflow: hidden;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: var(--detect-color);
  border-radius: 4px 4px 4px 0;
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
