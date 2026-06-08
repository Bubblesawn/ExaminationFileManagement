<template>
  <div class="page material-audit-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">材料审核</h2>
        <p class="page-subtitle">按业务编号或考生考籍定位免考、课程顶替、转入转出和毕业业务，上传材料后自动同步到对应业务。</p>
      </div>
      <el-tag v-if="businessBundle" :type="applicationStatusTag(businessBundle.applicationStatus)" effect="light">
        {{ applicationStatusText(businessBundle.applicationStatus) }}
      </el-tag>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="查询方式">
          <el-radio-group v-model="queryMode">
            <el-radio-button label="business">业务编号</el-radio-button>
            <el-radio-button label="record">考生</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="queryMode === 'business'" label="业务编号">
          <el-input
            v-model="businessNo"
            clearable
            placeholder="请输入业务编号或业务ID"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item v-else label="考生考籍">
          <el-select
            v-model="selectedRecordId"
            class="record-select"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchStudentRecords"
            :loading="recordLoading"
            placeholder="请输入考籍号、姓名或身份证号搜索"
            @keyup.enter="handleSearch"
            @change="handleRecordChange"
            @visible-change="handleRecordSelectVisible"
          >
            <el-option
              v-for="record in recordOptions"
              :key="record.id"
              :label="formatRecordOption(record)"
              :value="record.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">{{ queryButtonText }}</el-button>
          <el-button :icon="Refresh" @click="resetPage">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="queryMode === 'record' && recordBundles.length > 0" shadow="never" class="result-card">
      <template #header>
        <div class="card-header">
          <span>考生相关业务</span>
          <span class="material-count">共 {{ recordBundles.length }} 条</span>
        </div>
      </template>
      <el-table :data="recordBundles" border highlight-current-row @row-click="selectBusinessBundle">
        <el-table-column prop="applicationNo" label="业务编号" min-width="140" show-overflow-tooltip />
        <el-table-column label="业务类型" min-width="120">
          <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="applicationTitle" label="业务标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applyUserName" label="提交人" min-width="120" show-overflow-tooltip />
        <el-table-column prop="auditUserName" label="审核人" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="applicationStatusTag(row.applicationStatus)" size="small">
              {{ applicationStatusText(row.applicationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="材料数" width="90" align="center">
          <template #default="{ row }">{{ row.materialIds.length }}/{{ row.materials.length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click.stop="selectBusinessBundle(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-if="!businessBundle && !loading" :description="emptyDescription" />

    <template v-else-if="businessBundle">
      <el-row :gutter="16">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <span>业务信息</span>
                <el-tag effect="plain">{{ businessTypeText(businessBundle.businessType) }}</el-tag>
              </div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="业务编号">{{ businessBundle.applicationNo }}</el-descriptions-item>
              <el-descriptions-item label="业务ID">{{ businessBundle.id }}</el-descriptions-item>
              <el-descriptions-item label="考籍档案ID">{{ businessBundle.recordId }}</el-descriptions-item>
              <el-descriptions-item label="业务标题">{{ businessBundle.applicationTitle || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前节点">{{ businessBundle.currentNodeName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="提交人">{{ businessBundle.applyUserName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ businessBundle.submitTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="审核人">{{ businessBundle.auditUserName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="审核时间">{{ businessBundle.auditTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="upload-card">
            <template #header>
              <div class="card-header">
                <span>上传材料</span>
                <span class="material-count">已绑定 {{ businessBundle.materialIds.length }} 份</span>
              </div>
            </template>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="材料类型">
                <el-select
                  v-model="selectedMaterialType"
                  :loading="materialTypeLoading"
                  placeholder="请选择材料类型"
                  filterable
                >
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
                accept=".jpg,.jpeg,.png,.pdf"
                :disabled="uploadDisabled"
                :show-file-list="false"
                :http-request="handleUploadRequest"
                :before-upload="validateBeforeUpload"
              >
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽材料到此处，或点击选择文件</div>
                <template #tip>
                  <div class="el-upload__tip">支持 JPG、JPEG、PNG、PDF，单个文件不超过 20MB。</div>
                </template>
              </el-upload>
              <el-alert
                v-if="uploadDisabledReason"
                :title="uploadDisabledReason"
                type="warning"
                show-icon
                :closable="false"
                class="upload-alert"
              />
            </el-form>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-header">
                <span>业务材料</span>
                <el-button text type="primary" :icon="Refresh" :loading="loading" @click="refreshCurrentMaterials">
                  刷新
                </el-button>
              </div>
            </template>
            <el-alert
              type="success"
              show-icon
              :closable="false"
              title="材料上传后会自动写入该业务的材料ID，免考、课程顶替、转入转出和毕业管理页面可直接查看并审核。"
            />
            <el-table :data="businessMaterials" border class="material-table">
              <el-table-column prop="id" label="材料ID" width="90" align="center" />
              <el-table-column prop="originalFileName" label="文件名称" min-width="190" show-overflow-tooltip />
              <el-table-column label="材料类型" min-width="150">
                <template #default="{ row }">{{ materialTypeName(row.materialType) }}</template>
              </el-table-column>
              <el-table-column label="绑定业务" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="isBoundMaterial(row.id) ? 'success' : 'info'" size="small">
                    {{ isBoundMaterial(row.id) ? '已绑定' : '档案材料' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="110" align="right">
                <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="materialAuditStatusTag(row.auditStatus)" size="small">
                    {{ materialAuditStatusText(row.auditStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="上传时间" min-width="170" show-overflow-tooltip />
              <el-table-column label="操作" width="300" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="View" @click="previewMaterial(row)">预览</el-button>
                  <el-button link type="primary" :icon="Download" @click="downloadMaterial(row)">下载</el-button>
                  <el-button
                    v-if="row.auditStatus !== 'APPROVED'"
                    link
                    type="success"
                    @click="approveMaterial(row)"
                  >
                    审核通过
                  </el-button>
                  <el-button link type="danger" :icon="Delete" @click="deleteMaterial(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="businessMaterials.length === 0" description="该业务暂无材料，请在左侧上传" />
          </el-card>

          <el-card v-if="latestRecognition" shadow="never" class="recognition-card">
            <template #header>
              <div class="card-header">
                <span>上传即时识别</span>
                <el-tag :type="actionTagType(latestRecognition.suggested_action)" effect="light">
                  {{ actionText(latestRecognition.suggested_action) }}
                </el-tag>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="识别类别">{{ latestRecognition.category_name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="类别编码">{{ latestRecognition.category_code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="置信度">{{ formatPercent(latestRecognition.confidence) }}</el-descriptions-item>
              <el-descriptions-item label="需复核">{{ latestRecognition.need_manual_review ? '是' : '否' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <ApplicationMaterialAuditPanel
            :application-id="businessBundle.id"
            title="材料算法审核"
            description="对当前业务已绑定材料发起智能核验，检查材料缺失、分类和异常风险。"
          />
        </el-col>
      </el-row>
    </template>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="80%" top="6vh" destroy-on-close @closed="revokePreviewUrl">
      <div class="preview-body">
        <img v-if="previewKind === 'image' && previewObjectUrl" :src="previewObjectUrl" alt="材料图片预览" />
        <iframe v-else-if="previewKind === 'pdf' && previewObjectUrl" :src="previewObjectUrl" title="材料 PDF 预览" />
        <el-empty v-else description="该文件类型暂不支持在线预览，请下载查看" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions, type UploadRawFile } from 'element-plus'
import { Delete, Download, Refresh, Search, UploadFilled, View } from '@element-plus/icons-vue'
import ApplicationMaterialAuditPanel from '../../components/ai/ApplicationMaterialAuditPanel.vue'
import { recognizeImage, type AiRecognitionData, type SuggestedAction } from '../../api/ai'
import { pageStudentRecords, type StudentRecord } from '../../api/record'
import {
  downloadRecordMaterial,
  deleteRecordMaterial,
  approveRecordMaterial,
  getBusinessMaterials,
  listRecordBusinessMaterials,
  listEnabledMaterialTypes,
  previewRecordMaterial,
  uploadBusinessMaterial,
  type BusinessMaterialBundle,
  type MaterialType,
  type RecordMaterial
} from '../../api/material'

const maxFileSize = 20 * 1024 * 1024
const allowedSuffixes = new Set(['jpg', 'jpeg', 'png', 'pdf'])
const allowedMimeTypes = new Set(['image/jpeg', 'image/png', 'application/pdf'])

const businessNo = ref('')
const selectedRecordId = ref<number | undefined>()
const queryMode = ref<'business' | 'record'>('business')
const loading = ref(false)
const uploading = ref(false)
const recordLoading = ref(false)
const materialTypeLoading = ref(false)
const businessBundle = ref<BusinessMaterialBundle | null>(null)
const recordBundles = ref<BusinessMaterialBundle[]>([])
const recordOptions = ref<StudentRecord[]>([])
const materialTypes = ref<MaterialType[]>([])
const selectedMaterialType = ref('')
const previewVisible = ref(false)
const previewObjectUrl = ref('')
const currentPreviewMaterial = ref<RecordMaterial | null>(null)
const latestRecognition = ref<AiRecognitionData | null>(null)

const queryButtonText = computed(() => (queryMode.value === 'business' ? '查询业务' : '查询考生材料'))
const emptyDescription = computed(() =>
  queryMode.value === 'business' ? '请输入业务编号查询待上传材料的业务' : '请选择考籍档案查询该考生的业务材料'
)
const businessMaterials = computed(() => {
  const bundle = businessBundle.value
  if (!bundle) return []
  const boundIds = new Set(bundle.materialIds)
  return [...bundle.materials].sort((left, right) => {
    const leftBound = boundIds.has(left.id) ? 0 : 1
    const rightBound = boundIds.has(right.id) ? 0 : 1
    if (leftBound !== rightBound) return leftBound - rightBound
    return right.id - left.id
  })
})
const uploadDisabled = computed(() => Boolean(uploadDisabledReason.value))
const previewTitle = computed(() => currentPreviewMaterial.value?.originalFileName || '材料预览')
const previewKind = computed(() => {
  const suffix = currentPreviewMaterial.value?.fileSuffix?.toLowerCase()
  if (suffix === 'pdf') return 'pdf'
  if (suffix && ['jpg', 'jpeg', 'png'].includes(suffix)) return 'image'
  return 'other'
})
const uploadDisabledReason = computed(() => {
  if (!businessBundle.value) return '请先查询业务后再上传材料。'
  if (materialTypeLoading.value) return '正在加载材料类型，请稍候。'
  if (materialTypes.value.length === 0) return '暂无可用材料类型，请先维护材料类型。'
  if (!selectedMaterialType.value) return '请选择材料类型后再上传。'
  if (uploading.value) return '材料正在上传，请稍候。'
  return ''
})

/**
 * @brief 初始化材料类型。
 */
onMounted(loadMaterialTypes)

/**
 * @brief 根据当前查询方式加载材料审核数据。
 */
function handleSearch() {
  if (queryMode.value === 'business') {
    loadBusinessMaterials()
    return
  }
  loadRecordBusinessMaterials()
}

/**
 * @brief 查询业务申请和材料列表。
 */
async function loadBusinessMaterials() {
  const normalizedBusinessNo = businessNo.value.trim()
  if (!normalizedBusinessNo) {
    ElMessage.warning('请输入业务编号或业务ID')
    return
  }
  loading.value = true
  try {
    businessBundle.value = await getBusinessMaterials(normalizedBusinessNo)
    businessNo.value = businessBundle.value.applicationNo || normalizedBusinessNo
    recordBundles.value = []
    latestRecognition.value = null
    ElMessage.success('业务材料已加载')
  } finally {
    loading.value = false
  }
}

/**
 * @brief 按考籍档案查询业务材料列表。
 */
async function loadRecordBusinessMaterials() {
  if (!selectedRecordId.value) {
    ElMessage.warning('请选择考籍档案')
    return
  }
  loading.value = true
  try {
    recordBundles.value = await listRecordBusinessMaterials(selectedRecordId.value)
    businessBundle.value = recordBundles.value[0] ?? null
    if (businessBundle.value) {
      businessNo.value = businessBundle.value.applicationNo
    }
    latestRecognition.value = null
    ElMessage.success(`已加载 ${recordBundles.value.length} 条考生相关业务`)
  } finally {
    loading.value = false
  }
}

/**
 * @brief 远程搜索考籍档案，供考生材料查询下拉选择。
 *
 * @param keyword 考籍号、考生姓名或身份证号关键字。
 */
async function searchStudentRecords(keyword: string) {
  recordLoading.value = true
  try {
    const result = await pageStudentRecords({
      pageNo: 1,
      pageSize: 20,
      keyword: keyword || undefined,
      recordStatus: 'NORMAL',
      archiveStatus: 'UNARCHIVED'
    })
    recordOptions.value = result.records ?? []
  } finally {
    recordLoading.value = false
  }
}

/**
 * @brief 首次展开考籍档案下拉框时加载可选档案。
 *
 * @param visible 下拉框是否展开。
 */
function handleRecordSelectVisible(visible: boolean) {
  if (visible && recordOptions.value.length === 0) {
    searchStudentRecords('')
  }
}

/**
 * @brief 切换考籍档案时清空已选业务材料包。
 *
 * @param recordId 考籍档案ID。
 */
function handleRecordChange(recordId?: number) {
  selectedRecordId.value = recordId
  recordBundles.value = []
  businessBundle.value = null
  latestRecognition.value = null
}

/**
 * @brief 加载可用材料类型。
 */
async function loadMaterialTypes() {
  materialTypeLoading.value = true
  try {
    materialTypes.value = await listEnabledMaterialTypes()
    if (!selectedMaterialType.value && materialTypes.value.length > 0) {
      selectedMaterialType.value = materialTypes.value[0].typeCode
    }
  } finally {
    materialTypeLoading.value = false
  }
}

/**
 * @brief 重置页面查询和材料列表。
 */
function resetPage() {
  businessNo.value = ''
  selectedRecordId.value = undefined
  recordBundles.value = []
  businessBundle.value = null
  latestRecognition.value = null
}

/**
 * @brief 选择考生查询结果中的业务材料包。
 *
 * @param bundle 业务申请材料包。
 */
function selectBusinessBundle(bundle: BusinessMaterialBundle) {
  businessBundle.value = bundle
  businessNo.value = bundle.applicationNo || String(bundle.id)
  latestRecognition.value = null
}

function formatRecordOption(record: StudentRecord) {
  const candidateName = record.candidateName || '未知考生'
  const idCard = record.idCard ? ` / ${record.idCard}` : ''
  const majorName = record.majorName ? ` / ${record.majorName}` : ''
  return `${record.recordNo} / ${candidateName}${idCard}${majorName}`
}

/**
 * @brief 刷新当前选中业务材料。
 */
async function refreshCurrentMaterials() {
  if (!businessBundle.value) return
  const refreshedBundle = await getBusinessMaterials(businessBundle.value.applicationNo || String(businessBundle.value.id))
  businessBundle.value = refreshedBundle
  const index = recordBundles.value.findIndex((item) => item.id === refreshedBundle.id)
  if (index >= 0) {
    recordBundles.value.splice(index, 1, refreshedBundle)
  }
}

/**
 * @brief 上传前校验文件格式和大小。
 *
 * @param file Element Plus 上传原始文件。
 * @return 是否允许上传。
 */
function validateBeforeUpload(file: UploadRawFile) {
  const suffix = extractSuffix(file.name)
  const mimeAllowed = !file.type || allowedMimeTypes.has(file.type)
  if (!allowedSuffixes.has(suffix) || !mimeAllowed) {
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
 * @brief 按业务编号上传材料并刷新业务材料包。
 *
 * @param options Element Plus 自定义上传参数。
 */
async function handleUploadRequest(options: UploadRequestOptions) {
  if (!businessBundle.value || !selectedMaterialType.value) {
    options.onError(new Error('缺少业务或材料类型') as never)
    return
  }
  uploading.value = true
  try {
    businessBundle.value = await uploadBusinessMaterial(
      businessBundle.value.applicationNo || String(businessBundle.value.id),
      selectedMaterialType.value,
      options.file
    )
    syncSelectedBundleToRecordResults()
    await recognizeUploadedMaterial(options.file)
    options.onSuccess(businessBundle.value as never)
    ElMessage.success('材料上传成功，已同步到业务申请')
  } catch (error) {
    options.onError((error instanceof Error ? error : new Error('材料上传失败')) as never)
  } finally {
    uploading.value = false
  }
}

/**
 * @brief 对刚上传材料执行单图分类识别。
 *
 * @param file 用户刚上传的材料文件。
 */
async function recognizeUploadedMaterial(file: File) {
  const uploadedMaterial = businessMaterials.value.find((item) => item.originalFileName === file.name) ?? businessMaterials.value[0]
  if (!businessBundle.value || !uploadedMaterial) return
  if (!['jpg', 'jpeg', 'png'].includes(uploadedMaterial.fileSuffix?.toLowerCase() || '')) {
    latestRecognition.value = null
    return
  }
  const response = await recognizeImage('classify', {
    businessId: businessBundle.value.id,
    scene: 'MATERIAL_AUDIT',
    fileUrl: uploadedMaterial.previewUrl,
    fileName: uploadedMaterial.originalFileName,
    materialTypeHint: selectedMaterialType.value
  })
  if (response.code !== 200 || !response.data) {
    ElMessage.warning(response.message || '材料即时识别失败，请稍后在算法审核中复核')
    return
  }
  latestRecognition.value = response.data
}

/**
 * @brief 预览材料文件。
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
 * @brief 下载材料文件。
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
 * @brief 审核通过当前业务下的单条材料。
 *
 * @param material 待审核通过的材料。
 */
async function approveMaterial(material: RecordMaterial) {
  await ElMessageBox.confirm(`确认将材料“${material.originalFileName}”审核通过吗？`, '审核确认', {
    type: 'warning',
    confirmButtonText: '通过',
    cancelButtonText: '取消'
  })
  await approveRecordMaterial(material.id)
  await refreshCurrentMaterials()
  ElMessage.success('材料已审核通过')
}

/**
 * @brief 删除当前业务材料并刷新业务材料包。
 *
 * @param material 待删除材料。
 */
async function deleteMaterial(material: RecordMaterial) {
  if (!businessBundle.value) return
  await ElMessageBox.confirm(`确认删除材料“${material.originalFileName}”吗？删除后该材料会从业务申请中解绑。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteRecordMaterial(material.id)
  latestRecognition.value = null
  businessBundle.value = await getBusinessMaterials(businessBundle.value.applicationNo || String(businessBundle.value.id))
  syncSelectedBundleToRecordResults()
  ElMessage.success('材料已删除')
}

/**
 * @brief 将当前业务材料包同步回考生查询结果列表。
 */
function syncSelectedBundleToRecordResults() {
  if (!businessBundle.value) return
  const index = recordBundles.value.findIndex((item) => item.id === businessBundle.value?.id)
  if (index >= 0) {
    recordBundles.value.splice(index, 1, businessBundle.value)
  }
}

function revokePreviewUrl() {
  if (previewObjectUrl.value) {
    URL.revokeObjectURL(previewObjectUrl.value)
    previewObjectUrl.value = ''
  }
}

function isBoundMaterial(materialId: number) {
  return Boolean(businessBundle.value?.materialIds.includes(materialId))
}

function materialTypeName(typeCode?: string) {
  return materialTypes.value.find((item) => item.typeCode === typeCode)?.typeName || typeCode || '-'
}

function businessTypeText(type?: string) {
  const map: Record<string, string> = {
    EXEMPTION: '免考管理',
    COURSE_REPLACE: '课程顶替',
    TRANSFER: '转入转出',
    GRADUATION: '毕业管理'
  }
  return map[type || ''] || type || '-'
}

function applicationStatusText(status?: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    AUDITING: '审核中',
    APPROVED: '审核通过',
    REJECTED: '审核驳回',
    WITHDRAWN: '已撤回'
  }
  return map[status || ''] || status || '-'
}

function applicationStatusTag(status?: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'WITHDRAWN') return 'info'
  return 'warning'
}

function materialAuditStatusText(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return map[status || ''] || status || '-'
}

function materialAuditStatusTag(status?: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

function actionText(action?: SuggestedAction) {
  const map: Record<SuggestedAction, string> = {
    ACCEPT: '建议通过',
    REVIEW: '建议复核',
    REJECT: '建议驳回'
  }
  return map[action || 'REVIEW']
}

function actionTagType(action?: SuggestedAction) {
  if (action === 'ACCEPT') return 'success'
  if (action === 'REJECT') return 'danger'
  return 'warning'
}

function formatPercent(value?: number) {
  return `${Number(((value ?? 0) * 100).toFixed(1))}%`
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
</script>

<style scoped>
.material-audit-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-header,
.card-header {
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
.result-card,
.info-card,
.upload-card,
.table-card {
  border-radius: 8px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.query-form :deep(.el-input) {
  width: 280px;
}

.query-form :deep(.record-select) {
  width: 320px;
}

.upload-card {
  margin-top: 16px;
}

.result-card {
  margin-top: 0;
}

.upload-card :deep(.el-select),
.upload-card :deep(.el-upload),
.upload-card :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-icon {
  color: #2563eb;
  font-size: 38px;
}

.upload-alert,
.material-table,
.recognition-card {
  margin-top: 14px;
}

.material-count {
  color: #64748b;
  font-size: 13px;
  font-weight: 400;
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

@media (max-width: 768px) {
  .page-header,
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .query-form :deep(.el-input) {
    width: 100%;
  }
}
</style>
