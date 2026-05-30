<template>
  <section class="business-material-list">
    <div class="material-header">
      <h3>{{ title }}</h3>
      <el-button text type="primary" :icon="Refresh" :loading="loading" @click="loadMaterials">刷新</el-button>
    </div>

    <el-alert
      v-if="!businessNo"
      title="暂无业务编号，无法加载业务材料。"
      type="info"
      show-icon
      :closable="false"
    />
    <el-table v-else v-loading="loading" :data="materials" border size="small">
      <el-table-column prop="id" label="材料ID" width="88" align="center" />
      <el-table-column prop="originalFileName" label="文件名称" min-width="190" show-overflow-tooltip />
      <el-table-column label="材料类型" min-width="140">
        <template #default="{ row }">{{ materialTypeName(row.materialType) }}</template>
      </el-table-column>
      <el-table-column label="业务绑定" width="96" align="center">
        <template #default="{ row }">
          <el-tag :type="boundMaterialIds.includes(row.id) ? 'success' : 'info'" size="small">
            {{ boundMaterialIds.includes(row.id) ? '已绑定' : '档案材料' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="104" align="right">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="auditStatusTag(row.auditStatus)" size="small">{{ auditStatusText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="previewMaterial(row)">预览</el-button>
          <el-button link type="primary" :icon="Download" @click="downloadMaterial(row)">下载</el-button>
          <el-button link type="danger" :icon="Delete" @click="deleteMaterial(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="businessNo && !loading && materials.length === 0" description="当前业务暂无材料" />

    <el-dialog v-model="previewVisible" :title="previewTitle" width="80%" top="6vh" destroy-on-close @closed="revokePreviewUrl">
      <div class="preview-body">
        <img v-if="previewKind === 'image' && previewObjectUrl" :src="previewObjectUrl" alt="材料图片预览" />
        <iframe v-else-if="previewKind === 'pdf' && previewObjectUrl" :src="previewObjectUrl" title="材料 PDF 预览" />
        <el-empty v-else description="该文件类型暂不支持在线预览，请下载查看" />
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Refresh, View } from '@element-plus/icons-vue'
import {
  deleteRecordMaterial,
  downloadRecordMaterial,
  getBusinessMaterials,
  listEnabledMaterialTypes,
  previewRecordMaterial,
  type MaterialType,
  type RecordMaterial
} from '../../api/material'

const props = withDefaults(
  defineProps<{
    businessNo?: string
    title?: string
  }>(),
  {
    title: '申请材料'
  }
)

const loading = ref(false)
const materials = ref<RecordMaterial[]>([])
const boundMaterialIds = ref<number[]>([])
const materialTypes = ref<MaterialType[]>([])
const previewVisible = ref(false)
const previewObjectUrl = ref('')
const currentPreviewMaterial = ref<RecordMaterial | null>(null)

const previewTitle = computed(() => currentPreviewMaterial.value?.originalFileName || '材料预览')
const previewKind = computed(() => {
  const suffix = currentPreviewMaterial.value?.fileSuffix?.toLowerCase()
  if (suffix === 'pdf') return 'pdf'
  if (suffix && ['jpg', 'jpeg', 'png'].includes(suffix)) return 'image'
  return 'other'
})

watch(
  () => props.businessNo,
  () => loadMaterials(),
  { immediate: false }
)

/**
 * @brief 初始化材料类型和业务材料。
 */
onMounted(async () => {
  materialTypes.value = await listEnabledMaterialTypes()
  await loadMaterials()
})

/**
 * @brief 按业务编号加载当前业务材料。
 */
async function loadMaterials() {
  if (!props.businessNo) {
    materials.value = []
    boundMaterialIds.value = []
    return
  }
  loading.value = true
  try {
    const bundle = await getBusinessMaterials(props.businessNo)
    materials.value = bundle.materials ?? []
    boundMaterialIds.value = bundle.materialIds ?? []
  } finally {
    loading.value = false
  }
}

/**
 * @brief 预览业务材料。
 *
 * @param material 待预览材料。
 */
async function previewMaterial(material: RecordMaterial) {
  currentPreviewMaterial.value = material
  revokePreviewUrl()
  if (!['jpg', 'jpeg', 'png', 'pdf'].includes(material.fileSuffix?.toLowerCase() || '')) {
    previewVisible.value = true
    return
  }
  const blob = await previewRecordMaterial(material.id)
  previewObjectUrl.value = URL.createObjectURL(blob)
  previewVisible.value = true
}

/**
 * @brief 下载业务材料。
 *
 * @param material 待下载材料。
 */
async function downloadMaterial(material: RecordMaterial) {
  const blob = await downloadRecordMaterial(material.id)
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = material.originalFileName || material.fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

/**
 * @brief 删除业务材料并刷新列表。
 *
 * @param material 待删除材料。
 */
async function deleteMaterial(material: RecordMaterial) {
  await ElMessageBox.confirm(`确认删除材料“${material.originalFileName}”吗？删除后该材料会从业务申请中解绑。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteRecordMaterial(material.id)
  ElMessage.success('材料已删除')
  await loadMaterials()
}

function revokePreviewUrl() {
  if (previewObjectUrl.value) {
    URL.revokeObjectURL(previewObjectUrl.value)
    previewObjectUrl.value = ''
  }
}

function materialTypeName(typeCode?: string) {
  return materialTypes.value.find((item) => item.typeCode === typeCode)?.typeName || typeCode || '-'
}

function auditStatusText(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return map[status || ''] || status || '-'
}

function auditStatusTag(status?: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

function formatFileSize(size?: number) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.business-material-list {
  min-width: 0;
  margin-top: 18px;
}

.material-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.material-header h3 {
  margin: 0;
  font-size: 16px;
}

.preview-body {
  display: flex;
  justify-content: center;
  min-height: 420px;
  background: #f8fafc;
}

.preview-body img,
.preview-body iframe {
  width: 100%;
  max-height: 72vh;
  border: 0;
  object-fit: contain;
}
</style>
