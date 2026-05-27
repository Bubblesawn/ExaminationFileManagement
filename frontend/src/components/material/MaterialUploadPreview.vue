<template>
  <div class="material-upload-preview">
    <div class="material-toolbar">
      <div>
        <h3 class="material-title">档案材料</h3>
        <p class="material-subtitle">支持上传 JPG、JPEG、PNG 图片和 PDF 材料，可在线预览、下载和删除。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadMaterials">刷新</el-button>
    </div>

    <el-alert
      v-if="!recordId"
      title="请先选择考籍档案后再上传材料"
      type="info"
      show-icon
      :closable="false"
      class="material-alert"
    />

    <el-form class="upload-form" label-position="top" @submit.prevent>
      <el-form-item label="材料类型">
        <el-select v-model="selectedMaterialType" placeholder="请选择材料类型" filterable :disabled="!recordId">
          <el-option
            v-for="item in materialTypes"
            :key="item.typeCode"
            :label="item.typeName"
            :value="item.typeCode"
          />
        </el-select>
      </el-form-item>

      <el-upload
        drag
        action="#"
        :disabled="!recordId || !selectedMaterialType"
        :show-file-list="false"
        :http-request="handleUploadRequest"
        :before-upload="validateBeforeUpload"
        accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或点击选择文件</div>
        <template #tip>
          <div class="el-upload__tip">单个文件不超过 20MB，仅支持图片和 PDF。</div>
        </template>
      </el-upload>
    </el-form>

    <el-table v-loading="loading" :data="materials" border class="material-table">
      <el-table-column prop="originalFileName" label="文件名称" min-width="190" show-overflow-tooltip />
      <el-table-column label="材料类型" min-width="140">
        <template #default="{ row }">{{ materialTypeName(row.materialType) }}</template>
      </el-table-column>
      <el-table-column label="格式" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ (row.fileSuffix || '-').toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="审核状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="auditStatusTag(row.auditStatus)" size="small">{{ auditStatusText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="openPreview(row)">预览</el-button>
          <el-button link type="primary" :icon="Download" @click="downloadMaterial(row)">下载</el-button>
          <el-button link type="danger" :icon="Delete" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && materials.length === 0" description="当前档案暂无材料" class="material-empty" />

    <el-dialog v-model="previewDialogVisible" :title="previewTitle" width="860px" destroy-on-close @closed="clearPreview">
      <div v-loading="previewLoading" class="preview-body">
        <img v-if="previewKind === 'image' && previewObjectUrl" :src="previewObjectUrl" alt="材料图片预览" />
        <iframe v-else-if="previewKind === 'pdf' && previewObjectUrl" :src="previewObjectUrl" title="材料 PDF 预览" />
        <el-empty v-else description="当前文件暂不支持在线预览，请下载后查看" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions, type UploadRawFile } from 'element-plus'
import { Delete, Download, Refresh, UploadFilled, View } from '@element-plus/icons-vue'
import {
  deleteRecordMaterial,
  downloadRecordMaterial,
  listEnabledMaterialTypes,
  listRecordMaterials,
  previewRecordMaterial,
  uploadRecordMaterial,
  type MaterialType,
  type RecordMaterial
} from '../../api/material'

const props = defineProps<{
  recordId?: number | null
}>()

const emit = defineEmits<{
  uploaded: [material: RecordMaterial]
  deleted: [materialId: number]
}>()

const allowedMimeTypes = new Set(['image/jpeg', 'image/png', 'application/pdf'])
const allowedSuffixes = new Set(['jpg', 'jpeg', 'png', 'pdf'])
const maxFileSize = 20 * 1024 * 1024

const loading = ref(false)
const previewLoading = ref(false)
const materialTypes = ref<MaterialType[]>([])
const materials = ref<RecordMaterial[]>([])
const selectedMaterialType = ref('')
const previewDialogVisible = ref(false)
const previewObjectUrl = ref('')
const previewMaterial = ref<RecordMaterial | null>(null)

const previewTitle = computed(() => previewMaterial.value?.originalFileName || '材料预览')
const previewKind = computed(() => {
  const suffix = previewMaterial.value?.fileSuffix?.toLowerCase()
  const mimeType = previewMaterial.value?.mimeType
  if (mimeType?.startsWith('image/') || ['jpg', 'jpeg', 'png'].includes(suffix || '')) {
    return 'image'
  }
  if (mimeType === 'application/pdf' || suffix === 'pdf') {
    return 'pdf'
  }
  return 'unknown'
})

/**
 * @brief 加载材料类型和当前档案材料列表。
 */
async function loadInitialData() {
  await loadMaterialTypes()
  await loadMaterials()
}

/**
 * @brief 加载启用材料类型，并自动选中第一项。
 */
async function loadMaterialTypes() {
  materialTypes.value = await listEnabledMaterialTypes()
  if (!selectedMaterialType.value && materialTypes.value.length > 0) {
    selectedMaterialType.value = materialTypes.value[0].typeCode
  }
}

/**
 * @brief 加载当前档案的材料列表。
 */
async function loadMaterials() {
  if (!props.recordId) {
    materials.value = []
    return
  }
  loading.value = true
  try {
    materials.value = await listRecordMaterials({ recordId: props.recordId })
  } finally {
    loading.value = false
  }
}

/**
 * @brief 上传前校验材料格式和大小。
 *
 * @param file Element Plus 上传原始文件。
 * @return 是否允许继续上传。
 */
function validateBeforeUpload(file: UploadRawFile) {
  const suffix = extractSuffix(file.name)
  const mimeAllowed = !file.type || allowedMimeTypes.has(file.type)
  const suffixAllowed = allowedSuffixes.has(suffix)
  if (!mimeAllowed || !suffixAllowed) {
    ElMessage.warning('材料文件仅支持 JPG、JPEG、PNG 和 PDF 格式')
    return false
  }
  if (file.size > maxFileSize) {
    ElMessage.warning('单个材料文件不能超过 20MB')
    return false
  }
  return true
}

/**
 * @brief 执行材料上传请求。
 *
 * @param options Element Plus 自定义上传参数。
 */
async function handleUploadRequest(options: UploadRequestOptions) {
  if (!props.recordId || !selectedMaterialType.value) {
    ElMessage.warning('请先选择档案和材料类型')
    options.onError(new Error('缺少档案或材料类型') as never)
    return
  }

  try {
    const material = await uploadRecordMaterial(props.recordId, selectedMaterialType.value, options.file)
    materials.value.unshift(material)
    emit('uploaded', material)
    options.onSuccess(material)
    ElMessage.success('材料上传成功')
  } catch (error) {
    options.onError((error instanceof Error ? error : new Error('材料上传失败')) as never)
  }
}

/**
 * @brief 在线预览图片或 PDF 材料。
 *
 * @param material 待预览材料。
 */
async function openPreview(material: RecordMaterial) {
  previewMaterial.value = material
  previewDialogVisible.value = true
  previewLoading.value = true
  clearPreviewUrl()
  try {
    const blob = await previewRecordMaterial(material.id)
    previewObjectUrl.value = URL.createObjectURL(blob)
  } finally {
    previewLoading.value = false
  }
}

/**
 * @brief 下载指定材料文件。
 *
 * @param material 待下载材料。
 */
async function downloadMaterial(material: RecordMaterial) {
  const blob = await downloadRecordMaterial(material.id)
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = material.originalFileName || material.fileName
  link.click()
  URL.revokeObjectURL(objectUrl)
}

/**
 * @brief 二次确认并删除单条材料。
 *
 * @param material 待删除材料。
 */
async function confirmDelete(material: RecordMaterial) {
  await ElMessageBox.confirm(`确认删除材料“${material.originalFileName}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteRecordMaterial(material.id)
  materials.value = materials.value.filter((item) => item.id !== material.id)
  emit('deleted', material.id)
  ElMessage.success('材料已删除')
}

function materialTypeName(typeCode: string) {
  return materialTypes.value.find((item) => item.typeCode === typeCode)?.typeName || typeCode
}

function auditStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    PENDING: '待审核',
    PASSED: '已通过',
    REJECTED: '已退回'
  }
  return statusMap[status || ''] || status || '-'
}

function auditStatusTag(status?: string) {
  if (status === 'PASSED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

function formatFileSize(size?: number) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function extractSuffix(fileName: string) {
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.slice(index + 1).toLowerCase() : ''
}

function clearPreviewUrl() {
  if (previewObjectUrl.value) {
    URL.revokeObjectURL(previewObjectUrl.value)
    previewObjectUrl.value = ''
  }
}

function clearPreview() {
  clearPreviewUrl()
  previewMaterial.value = null
}

watch(
  () => props.recordId,
  () => {
    loadMaterials()
  }
)

onMounted(loadInitialData)
onBeforeUnmount(clearPreviewUrl)
</script>

<style scoped>
.material-upload-preview {
  display: grid;
  gap: 14px;
}

.material-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.material-title {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.material-subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.material-alert {
  margin-bottom: 2px;
}

.upload-form {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(0, 1fr);
  gap: 12px;
  align-items: end;
}

.upload-form :deep(.el-select) {
  width: 100%;
}

.upload-form :deep(.el-upload),
.upload-form :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-icon {
  color: #409eff;
  font-size: 32px;
}

.material-table {
  width: 100%;
}

.material-empty {
  padding: 8px 0;
}

.preview-body {
  min-height: 420px;
}

.preview-body img,
.preview-body iframe {
  display: block;
  width: 100%;
  min-height: 420px;
  border: 0;
}

.preview-body img {
  max-height: 70vh;
  object-fit: contain;
  background: #f8fafc;
}

@media (max-width: 768px) {
  .material-toolbar,
  .upload-form {
    grid-template-columns: 1fr;
  }

  .material-toolbar {
    flex-direction: column;
  }
}
</style>
