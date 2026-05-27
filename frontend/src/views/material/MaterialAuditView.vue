<template>
  <div class="page material-audit-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">材料审核</h2>
        <p>联调业务申请材料分类、缺失材料提示和异常材料提醒。</p>
      </div>
      <el-tag :type="overallTagType" effect="light">{{ overallActionText }}</el-tag>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <section class="panel">
          <div class="panel-title">申请信息</div>
          <el-form label-position="top">
            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-form-item label="业务编号">
                  <el-input-number v-model="form.businessId" :min="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="申请人">
                  <el-input v-model="form.applicantName" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="申请类型">
              <el-select v-model="form.applicationType">
                <el-option label="免考申请" value="EXEMPTION" />
                <el-option label="课程顶替" value="COURSE_REPLACE" />
                <el-option label="考籍转入转出" value="TRANSFER" />
                <el-option label="毕业申请" value="GRADUATION" />
              </el-select>
            </el-form-item>
          </el-form>
        </section>

        <section class="panel">
          <div class="panel-title">
            <span>申请材料</span>
            <el-button text type="primary" @click="addMaterial">新增材料</el-button>
          </div>

          <div class="material-list">
            <div v-for="(item, index) in materials" :key="item.localId" class="material-item">
              <div class="material-header">
                <span>材料 {{ index + 1 }}</span>
                <el-button text type="danger" :disabled="materials.length === 1" @click="removeMaterial(index)">
                  删除
                </el-button>
              </div>
              <el-form label-position="top">
                <el-form-item label="材料文件">
                  <div class="upload-field">
                    <el-upload
                      accept=".jpg,.jpeg,.png,.bmp,.webp"
                      :auto-upload="false"
                      :show-file-list="false"
                      :on-change="createMaterialUploadHandler(item)"
                    >
                      <el-button :loading="item.uploading" type="primary" plain>
                        {{ item.fileName ? '重新上传' : '上传材料' }}
                      </el-button>
                    </el-upload>
                    <span class="upload-name">{{ item.fileName || '支持 jpg、jpeg、png、bmp、webp' }}</span>
                  </div>
                </el-form-item>
                <el-form-item v-if="item.fileUrl" label="材料访问地址">
                  <el-input v-model="item.fileUrl" readonly />
                </el-form-item>
                <el-row :gutter="10">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="文件名称">
                      <el-input v-model="item.fileName" clearable />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="登记类别">
                      <el-select v-model="item.uploadedCategoryCode" clearable>
                        <el-option
                          v-for="option in materialTypeOptions"
                          :key="option.value"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="类型提示">
                  <el-input v-model="item.materialTypeHint" clearable placeholder="例如 身份证、成绩单、毕业证" />
                </el-form-item>
                <el-row :gutter="10">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="MIME 类型">
                      <el-input v-model="item.contentType" clearable placeholder="例如 image/jpeg" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="文件大小(KB)">
                      <el-input-number v-model="item.fileSizeKb" :min="0" controls-position="right" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </div>
          </div>

          <el-button type="primary" :loading="loading" class="submit-button" @click="submitAudit">
            发起智能核验
          </el-button>
        </section>
      </el-col>

      <el-col :xs="24" :lg="15">
        <section class="panel preprocess-panel">
          <div class="panel-title">材料预处理结果</div>
          <el-empty v-if="preprocessResults.length === 0" description="发起智能核验后展示格式、清晰度和分类结果" />
          <el-table v-else :data="preprocessResults" size="small" border>
            <el-table-column prop="file_name" label="文件名称" min-width="150" />
            <el-table-column label="格式校验" width="110">
              <template #default="{ row }">
                <el-tag :type="row.format_validation.valid ? 'success' : 'danger'" size="small">
                  {{ row.format_validation.valid ? '通过' : '不通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="文件后缀" width="90">
              <template #default="{ row }">{{ row.format_validation.file_suffix || '-' }}</template>
            </el-table-column>
            <el-table-column label="清晰度" min-width="150">
              <template #default="{ row }">
                <div class="clarity-cell">
                  <el-tag :type="clarityTagType(row.clarity.level)" size="small">
                    {{ clarityText(row.clarity.level) }}
                  </el-tag>
                  <span>{{ formatPercent(row.clarity.score) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="category_name" label="基础分类" min-width="130" />
            <el-table-column label="置信度" width="100">
              <template #default="{ row }">{{ formatPercent(row.confidence) }}</template>
            </el-table-column>
            <el-table-column label="建议" width="110">
              <template #default="{ row }">
                <el-tag :type="actionTagType(row.suggested_action)" size="small">
                  {{ actionText(row.suggested_action) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="问题提示" min-width="220">
              <template #default="{ row }">{{ formatPreprocessIssues(row) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section class="panel result-overview">
          <div class="panel-title">智能辅助结果</div>
          <el-empty v-if="!auditResult" description="请先发起申请材料智能核验" />

          <template v-else>
            <div class="metric-grid">
              <div class="metric-item">
                <span>已上传</span>
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
              <div class="section-title">必交材料清单</div>
              <div class="required-list">
                <el-tag v-for="item in auditResult.required_categories" :key="item.category_code" effect="plain">
                  {{ item.category_name }}
                </el-tag>
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
                <el-table-column prop="message" label="提示" min-width="260" />
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
                <el-table-column prop="abnormal_type" label="异常类型" min-width="150" />
                <el-table-column label="风险" width="90">
                  <template #default="{ row }">
                    <el-tag :type="riskTagType(row.risk_level)" size="small">{{ riskText(row.risk_level) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="message" label="异常说明" min-width="240" />
                <el-table-column prop="suggestion" label="处理建议" min-width="240" />
              </el-table>
            </div>
          </template>
        </section>

        <section v-if="auditResult" class="panel">
          <div class="panel-title">材料分类结果</div>
          <el-table :data="auditResult.classified_materials" size="small" border>
            <el-table-column prop="file_name" label="文件名称" min-width="160" />
            <el-table-column prop="category_name" label="识别类别" min-width="130" />
            <el-table-column label="登记类别" min-width="130">
              <template #default="{ row }">{{ formatUploadedCategory(row.uploaded_category_code) }}</template>
            </el-table-column>
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

          <div class="result-actions">
            <el-button @click="copySummary">复制联调摘要</el-button>
            <el-button type="primary" @click="confirmAudit">确认核验结果</el-button>
          </div>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import {
  auditApplicationMaterials,
  preprocessMaterial,
  recognizeImage,
  uploadMaterialFile,
  type ApplicationMaterialAuditData,
  type ImageClarityResult,
  type MaterialPreprocessData,
  type SuggestedAction
} from '../../api/ai'

interface EditableMaterial {
  localId: number
  materialId?: number
  fileUrl: string
  fileName: string
  materialTypeHint: string
  uploadedCategoryCode: string
  contentType: string
  fileSizeKb: number
  uploading?: boolean
}

type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
type ClarityLevel = ImageClarityResult['level']

const materialTypeOptions = [
  { label: '身份证材料', value: 'ID_CARD' },
  { label: '准考证材料', value: 'ADMISSION_TICKET' },
  { label: '学历证书材料', value: 'DIPLOMA' },
  { label: '成绩单材料', value: 'TRANSCRIPT' },
  { label: '免考证明材料', value: 'EXEMPTION_CERTIFICATE' },
  { label: '考生照片', value: 'PHOTO' }
]

const materialRequirementOrder: Record<string, string[]> = {
  EXEMPTION: ['ID_CARD', 'TRANSCRIPT', 'EXEMPTION_CERTIFICATE'],
  COURSE_REPLACE: ['ID_CARD', 'TRANSCRIPT', 'DIPLOMA'],
  TRANSFER: ['ID_CARD', 'ADMISSION_TICKET', 'TRANSCRIPT'],
  GRADUATION: ['ID_CARD', 'DIPLOMA', 'TRANSCRIPT', 'PHOTO']
}

const actionTextMap: Record<SuggestedAction, string> = {
  ACCEPT: '建议通过',
  REVIEW: '建议复核',
  REJECT: '建议退回'
}

const form = reactive({
  businessId: 2026052701,
  applicationType: 'EXEMPTION',
  applicantName: '张三'
})

const materials = ref<EditableMaterial[]>([
  {
    localId: 1,
    fileUrl: '',
    fileName: '',
    materialTypeHint: '',
    uploadedCategoryCode: '',
    contentType: '',
    fileSizeKb: 0
  }
])
const loading = ref(false)
const auditResult = ref<ApplicationMaterialAuditData | null>(null)
const preprocessResults = ref<MaterialPreprocessData[]>([])

const overallActionText = computed(() => actionText(auditResult.value?.suggested_action ?? 'REVIEW'))
const overallTagType = computed(() => actionTagType(auditResult.value?.suggested_action ?? 'REVIEW'))

/**
 * @brief 新增一条可编辑申请材料。
 */
function addMaterial() {
  materials.value.push({
    localId: Date.now(),
    fileUrl: '',
    fileName: '',
    materialTypeHint: '',
    uploadedCategoryCode: '',
    contentType: '',
    fileSizeKb: 0
  })
}

/**
 * @brief 移除一条申请材料。
 *
 * @param index 材料下标。
 */
function removeMaterial(index: number) {
  materials.value.splice(index, 1)
}

/**
 * @brief 创建当前材料行的上传回调。
 *
 * @param item 当前正在编辑的材料记录。
 * @return Element Plus 上传控件变更处理函数。
 */
function createMaterialUploadHandler(item: EditableMaterial) {
  return (uploadFile: UploadFile) => handleMaterialFileChange(uploadFile.raw, item)
}

/**
 * @brief 上传申请材料文件并回填识别所需地址。
 *
 * @param file 用户在上传控件中选择的材料文件。
 * @param item 当前正在编辑的材料记录。
 */
async function handleMaterialFileChange(file: File | undefined, item: EditableMaterial) {
  if (!file) return

  item.uploading = true
  try {
    const uploadResult = await uploadMaterialFile(file)
    item.fileName = uploadResult.fileName || file.name
    item.fileUrl = uploadResult.fileUrl
    item.contentType = uploadResult.contentType || file.type
    item.fileSizeKb = Math.ceil((uploadResult.size || file.size) / 1024)
    if (!item.materialTypeHint) {
      item.materialTypeHint = inferMaterialHint(item.fileName)
    }
    await autoRecognizeMaterial(item)
    ElMessage.success('材料上传成功，已自动识别材料类别')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '材料上传失败')
  } finally {
    item.uploading = false
  }
}

/**
 * @brief 上传后立即调用单图分类接口并回填材料类别。
 *
 * @param item 当前已上传的材料记录。
 */
async function autoRecognizeMaterial(item: EditableMaterial) {
  const response = await recognizeImage('classify', {
    businessId: form.businessId,
    fileUrl: item.fileUrl,
    fileName: item.fileName,
    materialTypeHint: item.materialTypeHint
  })

  if (response.code !== 200 || !response.data) {
    throw new Error(response.message || '材料类别自动识别失败')
  }

  const categoryCode = response.data.category_code
  if (categoryCode && categoryCode !== 'UNKNOWN') {
    item.uploadedCategoryCode = categoryCode
    item.materialTypeHint = categoryNameToHint(categoryCode) || item.materialTypeHint
    return
  }

  const fallbackCategoryCode = inferExpectedCategory(item)
  if (fallbackCategoryCode) {
    item.uploadedCategoryCode = fallbackCategoryCode
    item.materialTypeHint = categoryNameToHint(fallbackCategoryCode) || item.materialTypeHint
  }
}

/**
 * @brief 发起申请材料智能核验联调请求。
 */
async function submitAudit() {
  const invalidItem = materials.value.find((item) => !item.fileUrl.trim())
  if (invalidItem) {
    ElMessage.warning('请先上传所有申请材料')
    return
  }

  loading.value = true
  try {
    const preprocessResponses = await Promise.all(
      materials.value.map((item) =>
        preprocessMaterial({
          businessId: form.businessId,
          scene: 'MATERIAL_AUDIT',
          fileUrl: item.fileUrl,
          fileName: item.fileName,
          materialTypeHint: item.materialTypeHint,
          contentType: item.contentType || undefined,
          fileSizeKb: item.fileSizeKb
        })
      )
    )
    const preprocessFailure = preprocessResponses.find((item) => item.code !== 200 || item.data.code !== 200)
    if (preprocessFailure) {
      ElMessage.error(preprocessFailure.data?.message || preprocessFailure.message || '材料预处理失败')
      return
    }
    preprocessResults.value = preprocessResponses.map((item) => item.data.data)

    const response = await auditApplicationMaterials({
      businessId: form.businessId,
      applicationType: form.applicationType,
      applicantName: form.applicantName,
      materials: materials.value.map((item) => ({
        materialId: item.materialId,
        fileUrl: item.fileUrl,
        fileName: item.fileName,
        materialTypeHint: item.materialTypeHint,
        uploadedCategoryCode: item.uploadedCategoryCode || undefined
      }))
    })

    if (response.code !== 200) {
      ElMessage.error(response.message || '申请材料智能核验失败')
      return
    }

    auditResult.value = response.data
    ElMessage.success('申请材料智能核验完成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能辅助服务暂不可用')
  } finally {
    loading.value = false
  }
}

/**
 * @brief 记录人工确认结果，后续可对接业务审核保存接口。
 */
function confirmAudit() {
  ElMessage.success('核验结果已确认，可继续流转业务审核')
}

async function copySummary() {
  if (!auditResult.value) return
  const summary = `申请类型：${auditResult.value.application_type}；已上传：${auditResult.value.summary.material_count ?? 0}；缺失：${auditResult.value.summary.missing_count ?? 0}；异常：${auditResult.value.summary.abnormal_count ?? 0}；建议：${actionText(auditResult.value.suggested_action)}`
  try {
    await navigator.clipboard.writeText(summary)
    ElMessage.success('联调摘要已复制')
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

function clarityText(level: ClarityLevel) {
  const map: Record<ClarityLevel, string> = {
    CLEAR: '清晰',
    REVIEW: '待复核',
    BLURRY: '模糊',
    NOT_IMAGE: '非图片'
  }
  return map[level]
}

function clarityTagType(level: ClarityLevel) {
  if (level === 'CLEAR') return 'success'
  if (level === 'BLURRY') return 'danger'
  if (level === 'REVIEW') return 'warning'
  return 'info'
}

function formatPreprocessIssues(row: MaterialPreprocessData) {
  const issues = [...row.format_validation.issues, ...row.clarity.issues]
  return issues.length ? issues.join('、') : row.clarity.suggestion
}

function formatPercent(value?: number) {
  return `${Number(((value ?? 0) * 100).toFixed(1))}%`
}

function formatUploadedCategory(code?: string) {
  return materialTypeOptions.find((item) => item.value === code)?.label ?? code ?? '-'
}

/**
 * @brief 将算法类别编码转换为前端类型提示。
 *
 * @param categoryCode 算法识别出的材料类别编码。
 * @return 可再次传给算法服务的中文材料类型提示。
 */
function categoryNameToHint(categoryCode: string) {
  const map: Record<string, string> = {
    ID_CARD: '身份证',
    ADMISSION_TICKET: '准考证',
    DIPLOMA: '毕业证',
    TRANSCRIPT: '成绩单',
    EXEMPTION_CERTIFICATE: '免考证明',
    PHOTO: '照片'
  }
  return map[categoryCode] ?? ''
}

/**
 * @brief 当单图分类证据不足时，按申请类型和上传顺序推断当前应补材料。
 *
 * @param currentItem 当前已上传的材料记录。
 * @return 当前申请下最可能的材料类别编码。
 */
function inferExpectedCategory(currentItem: EditableMaterial) {
  const requiredCategories = materialRequirementOrder[form.applicationType] ?? materialRequirementOrder.EXEMPTION
  const usedCategories = new Set(
    materials.value
      .filter((item) => item.localId !== currentItem.localId)
      .map((item) => item.uploadedCategoryCode)
      .filter(Boolean)
  )
  return requiredCategories.find((categoryCode) => !usedCategories.has(categoryCode)) ?? requiredCategories[0] ?? ''
}

/**
 * @brief 根据材料文件名推断材料类型提示。
 *
 * @param fileName 上传材料文件名。
 * @return 可传递给算法服务的材料类型提示。
 */
function inferMaterialHint(fileName: string) {
  const normalizedName = fileName.toLowerCase()
  if (normalizedName.includes('id') || normalizedName.includes('身份证')) return '身份证'
  if (normalizedName.includes('transcript') || normalizedName.includes('score') || normalizedName.includes('成绩')) return '成绩单'
  if (normalizedName.includes('diploma') || normalizedName.includes('毕业') || normalizedName.includes('学历')) return '毕业证'
  if (normalizedName.includes('exemption') || normalizedName.includes('免考')) return '免考证明'
  if (normalizedName.includes('ticket') || normalizedName.includes('准考证')) return '准考证'
  if (normalizedName.includes('photo') || normalizedName.includes('照片')) return '照片'
  return ''
}
</script>

<style scoped>
.material-audit-page {
  display: grid;
  gap: 16px;
}

.material-audit-page :deep(.el-col) {
  min-width: 0;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-heading p {
  margin: 6px 0 0;
  color: #667085;
}

.panel {
  min-width: 0;
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.panel + .panel {
  margin-top: 16px;
}

.panel-title,
.material-header,
.result-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  margin-bottom: 14px;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.material-list {
  display: grid;
  gap: 12px;
}

.material-item {
  min-width: 0;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.material-header {
  margin-bottom: 8px;
  color: #1f2937;
  font-weight: 700;
}

.upload-field {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.upload-field :deep(.el-upload) {
  flex: 0 0 auto;
}

.upload-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #667085;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.submit-button,
.panel :deep(.el-input-number),
.panel :deep(.el-select) {
  width: 100%;
}

.submit-button {
  margin-top: 14px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 14px;
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
  font-size: 26px;
}

.metric-item.warning strong {
  color: #b45309;
}

.metric-item.danger strong {
  color: #b42318;
}

.result-section {
  min-width: 0;
  margin-top: 18px;
  overflow-x: auto;
}

.section-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.required-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.result-actions {
  justify-content: flex-end;
  margin-top: 16px;
}

.preprocess-panel {
  margin-bottom: 16px;
}

.clarity-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .page-heading,
  .result-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
