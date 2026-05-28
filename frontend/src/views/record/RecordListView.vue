<template>
  <div class="page record-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">考籍档案</h2>
        <p class="page-subtitle">维护考籍档案列表，并为选中档案上传、预览、下载和删除材料。</p>
      </div>
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
            <el-option label="在籍" value="ACTIVE" />
            <el-option label="暂停" value="SUSPENDED" />
            <el-option label="注销" value="CANCELLED" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { FolderOpened, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { recognizeSpeech, synthesizeSpeech, uploadSpeechAudio } from '../../api/ai'
import MaterialUploadPreview from '../../components/material/MaterialUploadPreview.vue'
import { pageStudentRecords, type StudentRecord } from '../../api/record'

const loading = ref(false)
const voiceQueryLoading = ref(false)
const ttsLoading = ref(false)
const records = ref<StudentRecord[]>([])
const selectedRecord = ref<StudentRecord | null>(null)
const materialDrawerVisible = ref(false)
const total = ref(0)
const voiceQueryText = ref('')
const lastNoticeText = ref('')
const voiceHintText = ref('点击开始语音后自动查询考籍')
const recording = ref(false)
const mediaRecorder = ref<MediaRecorder | null>(null)
const recordedChunks = ref<BlobPart[]>([])
const recordingStream = ref<MediaStream | null>(null)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  recordStatus: ''
})

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
    ACTIVE: '在籍',
    SUSPENDED: '暂停',
    CANCELLED: '注销',
    ARCHIVED: '已归档'
  }
  return statusMap[status || ''] || status || '-'
}

function recordStatusTag(status?: string) {
  if (status === 'ACTIVE') return 'success'
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
}
</style>
