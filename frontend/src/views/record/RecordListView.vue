<template>
  <div class="page record-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">考籍档案</h2>
        <p class="page-subtitle">维护考籍档案列表，并为选中档案上传、预览、下载和删除材料。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建档案</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="档案编号、姓名、身份证号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="考籍状态">
          <el-select v-model="query.recordStatus" clearable placeholder="全部">
            <el-option label="正常" value="NORMAL" />
            <el-option label="暂停" value="SUSPENDED" />
            <el-option label="注销" value="CANCELLED" />
            <el-option label="已转出" value="TRANSFERRED_OUT" />
            <el-option label="已毕业" value="GRADUATED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item label="语音查询">
          <div class="voice-query">
            <el-upload
              accept=".wav,.mp3,.m4a,.aac,.flac,.ogg,.webm"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleVoiceQueryUploadChange"
            >
              <el-button :loading="voiceQueryLoading" plain>上传识别</el-button>
            </el-upload>
            <el-button :loading="voiceQueryLoading" plain type="primary" @click="toggleVoiceRecording">
              {{ recording ? '停止识别' : '开始语音' }}
            </el-button>
            <el-button :loading="ttsLoading" plain @click="playRecordQueryNotice">播报结果</el-button>
            <span>{{ voiceQueryText || voiceHintText }}</span>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="recordNo" label="档案编号" min-width="150" fixed="left" show-overflow-tooltip />
        <el-table-column prop="candidateName" label="考生姓名" min-width="110">
          <template #default="{ row }">{{ row.candidateName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.idCard || '-' }}</template>
        </el-table-column>
        <el-table-column prop="admissionNo" label="准考证号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.admissionNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="majorName" label="专业" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.majorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="考籍状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="recordStatusTag(row.recordStatus)">{{ recordStatusText(row.recordStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="归档状态" width="110" align="center">
          <template #default="{ row }">{{ archiveStatusText(row.archiveStatus) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="FolderOpened" @click="openMaterialDrawer(row)">材料管理</el-button>
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
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </div>
    </el-card>

    <el-drawer v-model="materialDrawerVisible" title="材料上传与预览" size="860px" destroy-on-close>
      <template v-if="selectedRecord">
        <el-descriptions :column="2" border class="record-summary">
          <el-descriptions-item label="档案编号">{{ selectedRecord.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="考生姓名">{{ selectedRecord.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ selectedRecord.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报考专业">{{ selectedRecord.majorName || '-' }}</el-descriptions-item>
        </el-descriptions>

        <MaterialUploadPreview
          :record-id="selectedRecord.id"
          @uploaded="handleMaterialChanged"
          @deleted="handleMaterialChanged"
        />
      </template>
    </el-drawer>

    <el-dialog v-model="createDialogVisible" title="新建考籍档案" width="720px" destroy-on-close>
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="考生" prop="candidateId">
            <el-select
              v-model="recordForm.candidateId"
              filterable
              remote
              clearable
              reserve-keyword
              :remote-method="searchCandidates"
              :loading="candidateLoading"
              placeholder="输入姓名、身份证号或准考证号搜索"
              @visible-change="handleCandidateSelectVisible"
              @change="handleCandidateChange"
            >
              <el-option
                v-for="candidate in candidateOptions"
                :key="candidate.id"
                :label="formatCandidateOption(candidate)"
                :value="candidate.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="考籍号" prop="recordNo">
            <el-input v-model="recordForm.recordNo" maxlength="64" placeholder="请输入考籍号" />
          </el-form-item>
          <el-form-item label="注册批次" prop="enrollBatch">
            <el-input v-model="recordForm.enrollBatch" maxlength="64" placeholder="如：2026年春季" />
          </el-form-item>
          <el-form-item label="考籍层次" prop="educationLevel">
            <el-select v-model="recordForm.educationLevel" clearable placeholder="请选择考籍层次">
              <el-option label="专科" value="专科" />
              <el-option label="本科" value="本科" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业代码" prop="majorCode">
            <el-input v-model="recordForm.majorCode" maxlength="64" placeholder="请输入专业代码" />
          </el-form-item>
          <el-form-item label="专业名称" prop="majorName">
            <el-input v-model="recordForm.majorName" maxlength="128" placeholder="请输入专业名称" />
          </el-form-item>
          <el-form-item label="考籍状态" prop="recordStatus">
            <el-select v-model="recordForm.recordStatus" placeholder="请选择考籍状态">
              <el-option label="正常" value="NORMAL" />
              <el-option label="暂停" value="SUSPENDED" />
              <el-option label="注销" value="CANCELLED" />
              <el-option label="已转出" value="TRANSFERRED_OUT" />
              <el-option label="已毕业" value="GRADUATED" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="recordForm.remark" type="textarea" maxlength="512" show-word-limit :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { FolderOpened, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { recognizeSpeech, synthesizeSpeech, uploadSpeechAudio } from '../../api/ai'
import { pageCandidates, type Candidate } from '../../api/candidate'
import MaterialUploadPreview from '../../components/material/MaterialUploadPreview.vue'
import {
  createStudentRecord,
  pageStudentRecords,
  type StudentRecord,
  type StudentRecordCreatePayload
} from '../../api/record'

type RecordFormModel = Required<Pick<StudentRecordCreatePayload, 'candidateId' | 'recordNo'>> &
  Omit<StudentRecordCreatePayload, 'candidateId' | 'recordNo'>

const loading = ref(false)
const submitting = ref(false)
const candidateLoading = ref(false)
const voiceQueryLoading = ref(false)
const ttsLoading = ref(false)
const records = ref<StudentRecord[]>([])
const candidateOptions = ref<Candidate[]>([])
const selectedRecord = ref<StudentRecord | null>(null)
const materialDrawerVisible = ref(false)
const createDialogVisible = ref(false)
const total = ref(0)
const voiceQueryText = ref('')
const lastNoticeText = ref('')
const voiceHintText = ref('点击开始语音后自动查询考籍')
const recording = ref(false)
const mediaRecorder = ref<MediaRecorder | null>(null)
const recordedChunks = ref<BlobPart[]>([])
const recordingStream = ref<MediaStream | null>(null)
const recordFormRef = ref<FormInstance>()

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  recordStatus: ''
})

const recordForm = reactive<RecordFormModel>({
  candidateId: undefined as unknown as number,
  recordNo: '',
  enrollBatch: '',
  educationLevel: '',
  majorCode: '',
  majorName: '',
  recordStatus: 'NORMAL',
  remark: ''
})

const recordRules: FormRules = {
  candidateId: [{ required: true, message: '请选择考生', trigger: 'change' }],
  recordNo: [{ required: true, message: '请输入考籍号', trigger: 'blur' }],
  recordStatus: [{ required: true, message: '请选择考籍状态', trigger: 'change' }]
}

/**
 * @brief 加载考籍档案分页列表。
 */
async function loadRecords() {
  loading.value = true
  try {
    const result = await pageStudentRecords({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      recordStatus: query.recordStatus || undefined
    })
    records.value = result.records ?? []
    total.value = result.total ?? 0
    lastNoticeText.value = buildRecordQueryNotice()
  } finally {
    loading.value = false
  }
}

/**
 * @brief 按查询条件重新检索档案。
 */
function handleSearch() {
  query.pageNo = 1
  loadRecords()
}

/**
 * @brief 清空查询条件并重新加载档案。
 */
function resetQuery() {
  query.pageNo = 1
  query.keyword = ''
  query.recordStatus = ''
  voiceQueryText.value = ''
  loadRecords()
}

/**
 * @brief 打开新建考籍档案弹窗，并预加载可选考生。
 */
function openCreateDialog() {
  resetRecordForm()
  createDialogVisible.value = true
  searchCandidates('')
}

/**
 * @brief 重置新建考籍档案表单。
 */
function resetRecordForm() {
  Object.assign(recordForm, {
    candidateId: undefined,
    recordNo: '',
    enrollBatch: '',
    educationLevel: '',
    majorCode: '',
    majorName: '',
    recordStatus: 'NORMAL',
    remark: ''
  })
  recordFormRef.value?.clearValidate()
}

/**
 * @brief 按关键字远程搜索考生，供建档表单选择考生。
 *
 * @param keyword 姓名、身份证号或准考证号关键字。
 */
async function searchCandidates(keyword: string) {
  candidateLoading.value = true
  try {
    const result = await pageCandidates({
      pageNo: 1,
      pageSize: 20,
      keyword: keyword || undefined
    })
    candidateOptions.value = result.records ?? []
  } finally {
    candidateLoading.value = false
  }
}

/**
 * @brief 首次展开考生下拉框时加载候选考生。
 *
 * @param visible 下拉框是否展开。
 */
function handleCandidateSelectVisible(visible: boolean) {
  if (visible && candidateOptions.value.length === 0) {
    searchCandidates('')
  }
}

/**
 * @brief 选择考生后用考生基础信息预填档案层次和专业名称。
 *
 * @param candidateId 选中的考生ID。
 */
function handleCandidateChange(candidateId?: number) {
  const candidate = candidateOptions.value.find((item) => item.id === candidateId)
  if (!candidate) return

  if (!recordForm.educationLevel) {
    recordForm.educationLevel = candidate.educationLevel || ''
  }
  if (!recordForm.majorName) {
    recordForm.majorName = candidate.majorName || ''
  }
}

/**
 * @brief 格式化考生下拉选项，帮助管理人员区分同名考生。
 *
 * @param candidate 考生信息。
 * @return 下拉框展示文本。
 */
function formatCandidateOption(candidate: Candidate) {
  const admissionNo = candidate.admissionNo ? ` / ${candidate.admissionNo}` : ''
  return `${candidate.name} / ${candidate.idCard}${admissionNo}`
}

/**
 * @brief 提交新建考籍档案表单，成功后刷新档案列表。
 */
async function submitRecord() {
  const valid = await recordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createStudentRecord({
      candidateId: recordForm.candidateId,
      recordNo: recordForm.recordNo,
      enrollBatch: recordForm.enrollBatch || undefined,
      educationLevel: recordForm.educationLevel || undefined,
      majorCode: recordForm.majorCode || undefined,
      majorName: recordForm.majorName || undefined,
      recordStatus: recordForm.recordStatus || undefined,
      remark: recordForm.remark || undefined
    })
    ElMessage.success('考籍档案已新建')
    createDialogVisible.value = false
    query.pageNo = 1
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '新建考籍档案失败')
  } finally {
    submitting.value = false
  }
}

/**
 * @brief 上传语音并调用 ASR，将识别文本作为考籍关键字执行查询。
 *
 * @param uploadFile Element Plus 上传文件对象。
 */
async function handleVoiceQueryUploadChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) return

  await recognizeRecordVoiceFile(file)
}

/**
 * @brief 启停浏览器录音，将麦克风语音直接转为考籍查询条件。
 */
async function toggleVoiceRecording() {
  if (recording.value) {
    mediaRecorder.value?.stop()
    return
  }

  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    ElMessage.warning('当前浏览器不支持直接录音，请使用上传识别')
    return
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recordingStream.value = stream
    recordedChunks.value = []
    const recorder = new MediaRecorder(stream)
    mediaRecorder.value = recorder

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        recordedChunks.value.push(event.data)
      }
    }
    recorder.onstop = () => {
      const voiceBlob = new Blob(recordedChunks.value, { type: recorder.mimeType || 'audio/webm' })
      stopRecordingStream()
      recording.value = false
      voiceHintText.value = '录音结束，正在识别查询条件'
      if (voiceBlob.size === 0) {
        voiceHintText.value = '未采集到语音内容，请重新开始语音'
        ElMessage.warning('未采集到语音内容')
        return
      }
      const voiceFile = new File([voiceBlob], `record-voice-${Date.now()}.webm`, { type: voiceBlob.type })
      recognizeRecordVoiceFile(voiceFile)
    }

    recorder.start()
    recording.value = true
    voiceHintText.value = '正在录音，请说出查询条件'
    ElMessage.success('开始录音，请说出查询条件')
  } catch (error) {
    stopRecordingStream()
    recording.value = false
    voiceHintText.value = getMicrophoneErrorMessage(error)
    ElMessage.error(voiceHintText.value)
  }
}

/**
 * @brief 调用 ASR 识别语音文件并刷新考籍档案列表。
 *
 * @param file 浏览器录音或用户上传的语音文件。
 */
async function recognizeRecordVoiceFile(file: File) {
  voiceQueryLoading.value = true
  try {
    const uploadResult = await uploadSpeechAudio(file)
    const response = await recognizeSpeech({
      audioUrl: uploadResult.fileUrl,
      scene: 'ARCHIVE',
      languageHint: 'zh-CN'
    })

    if (response.code !== 200) {
      ElMessage.error(response.message || '语音查询识别失败')
      return
    }

    voiceQueryText.value = response.data.text
    query.keyword = extractRecordKeyword(response.data.text)
    query.pageNo = 1
    await loadRecords()
    voiceHintText.value = '语音查询已完成'
    ElMessage.success('语音查询已完成')
  } catch (error) {
    voiceHintText.value = '语音查询失败，请重试或使用上传识别'
    ElMessage.error(error instanceof Error ? error.message : '语音查询服务暂不可用')
  } finally {
    voiceQueryLoading.value = false
  }
}

/**
 * @brief 释放浏览器录音占用的麦克风资源。
 */
function stopRecordingStream() {
  recordingStream.value?.getTracks().forEach((track) => track.stop())
  recordingStream.value = null
  mediaRecorder.value = null
}

/**
 * @brief 将浏览器麦克风异常转换为中文业务提示。
 *
 * @param error 浏览器录音 API 抛出的异常。
 * @return 面向管理人员的权限处理提示。
 */
function getMicrophoneErrorMessage(error: unknown) {
  const errorName = error instanceof DOMException ? error.name : ''
  if (errorName === 'NotAllowedError' || errorName === 'PermissionDeniedError') {
    return '麦克风权限被拒绝，请在浏览器地址栏允许麦克风后重试'
  }
  if (errorName === 'NotFoundError' || errorName === 'DevicesNotFoundError') {
    return '未检测到可用麦克风，请连接麦克风后重试'
  }
  if (errorName === 'NotReadableError' || errorName === 'TrackStartError') {
    return '麦克风正被其他程序占用，请关闭占用程序后重试'
  }
  return '无法访问麦克风，请检查浏览器权限或使用上传识别'
}

/**
 * @brief 将考籍查询结果摘要转换为语音播报。
 */
async function playRecordQueryNotice() {
  const content = lastNoticeText.value || buildRecordQueryNotice()
  if (!content) {
    ElMessage.warning('暂无可播报的查询结果')
    return
  }

  ttsLoading.value = true
  try {
    const response = await synthesizeSpeech({
      content,
      scene: 'ARCHIVE'
    })

    if (response.code !== 200) {
      ElMessage.error(response.message || '语音播报调用失败')
      return
    }

    const audio = new Audio(response.data.audio_url)
    await audio.play().catch(() => undefined)
    ElMessage.success('考籍查询结果播报已生成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '语音播报服务暂不可用')
  } finally {
    ttsLoading.value = false
  }
}

/**
 * @brief 打开选中档案的材料管理抽屉。
 *
 * @param record 当前选中的考籍档案。
 */
function openMaterialDrawer(record: StudentRecord) {
  selectedRecord.value = record
  materialDrawerVisible.value = true
}

/**
 * @brief 材料增删后保留抽屉状态，便于后续扩展材料数量统计刷新。
 */
function handleMaterialChanged() {
  selectedRecord.value = selectedRecord.value ? { ...selectedRecord.value } : null
}

function recordStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    NORMAL: '正常',
    SUSPENDED: '暂停',
    CANCELLED: '注销',
    TRANSFERRED_OUT: '已转出',
    GRADUATED: '已毕业'
  }
  return statusMap[status || ''] || status || '-'
}

function recordStatusTag(status?: string) {
  if (status === 'NORMAL') return 'success'
  if (status === 'SUSPENDED') return 'warning'
  if (status === 'CANCELLED') return 'danger'
  return 'info'
}

function archiveStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    UNARCHIVED: '未归档',
    ARCHIVED: '已归档'
  }
  return statusMap[status || ''] || status || '-'
}

/**
 * @brief 从语音识别文本中提取更适合考籍检索的关键字。
 *
 * @param text ASR 识别出的完整语音文本。
 * @return 用于档案编号、姓名或证件号模糊查询的关键字。
 */
function extractRecordKeyword(text: string) {
  const normalizedText = text.trim()
  const idMatch = normalizedText.match(/[0-9Xx]{6,}/)
  if (idMatch) return idMatch[0]

  return normalizedText
    .replace(/请|帮我|查询|查一下|考籍|档案|考生|信息|状态|业务|办理/g, '')
    .replace(/[，。,.？?\s]/g, '')
    .trim() || normalizedText
}

/**
 * @brief 构造考籍查询结果播报文本。
 *
 * @return 可提交给 TTS 的查询结果摘要。
 */
function buildRecordQueryNotice() {
  const keywordText = query.keyword ? `关键字 ${query.keyword}` : '全部考籍档案'
  if (total.value === 0) {
    return `${keywordText} 未查询到匹配的考籍数据，请调整条件后重试。`
  }

  const firstRecord = records.value[0]
  const firstRecordText = firstRecord
    ? `首条记录为 ${firstRecord.candidateName || '未知考生'}，档案编号 ${firstRecord.recordNo}，当前状态 ${recordStatusText(firstRecord.recordStatus)}。`
    : ''
  return `${keywordText} 共查询到 ${total.value} 条考籍数据。${firstRecordText}`
}

onMounted(loadRecords)
</script>

<style scoped>
.record-page {
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-date-editor) {
  width: 100%;
}

.voice-query {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.voice-query span {
  max-width: 320px;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.record-summary {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
