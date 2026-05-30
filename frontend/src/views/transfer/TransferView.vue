<template>
  <div class="page transfer-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">考籍转入转出</h2>
        <p class="page-subtitle">办理考籍转入、考籍转出申请、审核和流程记录查看。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新增申请</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="申请编号、姓名、考籍号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="转考类型">
          <el-select v-model="query.transferType" clearable placeholder="全部">
            <el-option label="考籍转入" value="TRANSFER_IN" />
            <el-option label="考籍转出" value="TRANSFER_OUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请状态">
          <el-select v-model="query.applicationStatus" clearable placeholder="全部">
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="审核通过" value="APPROVED" />
            <el-option label="审核驳回" value="REJECTED" />
            <el-option label="已撤回" value="WITHDRAWN" />
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
            <el-button :loading="ttsLoading" plain @click="playTransferQueryNotice">播报结果</el-button>
            <span>{{ voiceQueryText || voiceHintText }}</span>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="applications" border>
        <el-table-column prop="applicationNo" label="申请编号" min-width="170" fixed="left" show-overflow-tooltip />
        <el-table-column label="转考类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="transferTypeTag(row.businessType)">{{ transferTypeText(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="candidateName" label="考生姓名" min-width="110">
          <template #default="{ row }">{{ row.candidateName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="recordNo" label="考籍号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.recordNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sourceProvince" label="原考籍省份" min-width="130">
          <template #default="{ row }">{{ row.sourceProvince || '-' }}</template>
        </el-table-column>
        <el-table-column prop="targetProvince" label="目标省份" min-width="130">
          <template #default="{ row }">{{ row.targetProvince || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="applicationStatusTag(row.applicationStatus)">
              {{ applicationStatusText(row.applicationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetailDrawer(row)">详情</el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="primary"
              :icon="Edit"
              @click="openEditDrawer(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.applicationStatus === 'SUBMITTED'"
              link
              type="warning"
              :icon="RefreshLeft"
              @click="openWithdrawDialog(row)"
            >
              撤回
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
          @size-change="loadApplications"
          @current-change="loadApplications"
        />
      </div>
    </el-card>

    <el-drawer v-model="formDrawerVisible" :title="formMode === 'create' ? '新增转考申请' : '编辑转考申请'" size="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item v-if="formMode === 'create'" label="考籍档案" prop="recordId">
          <el-select
            v-model="form.recordId"
            class="full-select"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchStudentRecords"
            :loading="recordLoading"
            placeholder="输入考籍号、姓名或身份证号搜索"
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
        <el-form-item label="转考类型" prop="transferType">
          <el-radio-group v-model="form.transferType" :disabled="formMode === 'edit'" @change="handleTransferTypeChange">
            <el-radio-button label="TRANSFER_IN">考籍转入</el-radio-button>
            <el-radio-button label="TRANSFER_OUT">考籍转出</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.transferType === 'TRANSFER_IN'">
          <el-form-item label="原考籍省份" prop="sourceProvince">
            <el-input v-model="form.sourceProvince" placeholder="例如：湖南省" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="原考籍单位">
            <el-input v-model="form.sourceSchool" placeholder="例如：湖南省自考办" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item label="原考籍号">
            <el-input v-model="form.sourceRecordNo" maxlength="64" show-word-limit />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="目标省份" prop="targetProvince">
            <el-input v-model="form.targetProvince" placeholder="例如：浙江省" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="目标接收单位">
            <el-input v-model="form.targetSchool" placeholder="例如：浙江省自考办" maxlength="128" show-word-limit />
          </el-form-item>
        </template>
        <el-form-item label="转考原因" prop="transferReason">
          <el-input v-model="form.transferReason" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="申请材料">
          <el-select
            v-model="selectedMaterialIds"
            class="full-select"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :disabled="!form.recordId"
            :loading="materialLoading"
            :placeholder="form.recordId ? '请选择当前档案材料' : '请先选择考籍档案'"
          >
            <el-option
              v-for="material in materialOptions"
              :key="material.id"
              :label="formatMaterialOption(material)"
              :value="material.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="转考申请详情" size="780px">
      <template v-if="selectedApplication">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ selectedApplication.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ applicationStatusText(selectedApplication.applicationStatus) }}</el-descriptions-item>
          <el-descriptions-item label="转考类型">{{ transferTypeText(selectedApplication.businessType) }}</el-descriptions-item>
          <el-descriptions-item label="考籍档案">{{ selectedApplication.recordNo || selectedApplication.recordId }}</el-descriptions-item>
          <el-descriptions-item label="考生姓名">{{ selectedApplication.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考籍号">{{ selectedApplication.recordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原考籍省份">{{ selectedApplication.sourceProvince || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原考籍单位">{{ selectedApplication.sourceSchool || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原考籍号">{{ selectedApplication.sourceRecordNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标省份">{{ selectedApplication.targetProvince || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标接收单位">{{ selectedApplication.targetSchool || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请材料">{{ selectedApplication.materialIds?.join(', ') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转考原因" :span="2">{{ selectedApplication.transferReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ selectedApplication.auditOpinion || '-' }}</el-descriptions-item>
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
            <div class="flow-item">
              <span>{{ flowActionText(record.auditAction) }}</span>
              <span class="flow-status">{{ applicationStatusText(record.beforeStatus) }} → {{ applicationStatusText(record.afterStatus) }}</span>
            </div>
            <p class="flow-opinion">{{ record.auditOpinion || '-' }}</p>
          </el-timeline-item>
        </el-timeline>

        <ApplicationMaterialAuditPanel :application-id="selectedApplication.id" />
      </template>
    </el-drawer>

    <el-dialog v-model="auditDialogVisible" :title="auditMode === 'approve' ? '审核通过' : '审核驳回'" width="880px">
      <ApplicationMaterialAuditPanel
        v-if="selectedApplication"
        :application-id="selectedApplication.id"
        title="审核前智能核验"
        description="审核转考申请前，查看材料完整性和异常材料提醒。"
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
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose, Edit, Plus, Refresh, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import { recognizeSpeech, synthesizeSpeech, uploadSpeechAudio } from '../../api/ai'
import ApplicationMaterialAuditPanel from '../../components/ai/ApplicationMaterialAuditPanel.vue'
import { listRecordMaterials, type RecordMaterial } from '../../api/material'
import { pageStudentRecords, type StudentRecord } from '../../api/record'
import {
  approveTransferApplication,
  getTransferApplicationDetail,
  listTransferFlowRecords,
  pageTransferApplications,
  rejectTransferApplication,
  submitTransferApplication,
  updateTransferApplication,
  withdrawTransferApplication,
  type TransferApplication,
  type TransferFlowRecord
} from '../../api/transfer'

const loading = ref(false)
const saving = ref(false)
const voiceQueryLoading = ref(false)
const ttsLoading = ref(false)
const recordLoading = ref(false)
const materialLoading = ref(false)
const applications = ref<TransferApplication[]>([])
const recordOptions = ref<StudentRecord[]>([])
const materialOptions = ref<RecordMaterial[]>([])
const total = ref(0)
const formDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const auditDialogVisible = ref(false)
const withdrawDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const auditMode = ref<'approve' | 'reject'>('approve')
const selectedApplication = ref<TransferApplication | null>(null)
const flowRecords = ref<TransferFlowRecord[]>([])
const formRef = ref<FormInstance>()
const selectedMaterialIds = ref<number[]>([])
const voiceQueryText = ref('')
const lastNoticeText = ref('')
const voiceHintText = ref('点击开始语音后自动查询转考申请')
const recording = ref(false)
const mediaRecorder = ref<MediaRecorder | null>(null)
const recordedChunks = ref<BlobPart[]>([])
const recordingStream = ref<MediaStream | null>(null)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  transferType: '',
  applicationStatus: ''
})

const form = reactive({
  id: 0,
  recordId: undefined as number | undefined,
  transferType: 'TRANSFER_IN',
  sourceProvince: '',
  sourceSchool: '',
  sourceRecordNo: '',
  targetProvince: '',
  targetSchool: '',
  transferReason: '',
  remark: ''
})

const auditForm = reactive({
  auditOpinion: ''
})

const withdrawForm = reactive({
  withdrawReason: ''
})

const formRules: FormRules = {
  recordId: [{ required: true, message: '请选择考籍档案', trigger: 'change' }],
  transferType: [{ required: true, message: '请选择转考类型', trigger: 'change' }],
  sourceProvince: [
    {
      validator: (_rule, value, callback) => {
        if (form.transferType === 'TRANSFER_IN' && !String(value || '').trim()) {
          callback(new Error('请输入原考籍省份'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  targetProvince: [
    {
      validator: (_rule, value, callback) => {
        if (form.transferType === 'TRANSFER_OUT' && !String(value || '').trim()) {
          callback(new Error('请输入目标省份'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  transferReason: [{ required: true, message: '请输入转考原因', trigger: 'blur' }]
}

/**
 * @brief 加载考籍转入转出申请分页列表。
 */
async function loadApplications() {
  loading.value = true
  try {
    const result = await pageTransferApplications({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      transferType: query.transferType || undefined,
      applicationStatus: query.applicationStatus || undefined
    })
    applications.value = result.records ?? []
    total.value = result.total ?? 0
    lastNoticeText.value = buildTransferQueryNotice()
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNo = 1
  loadApplications()
}

function resetQuery() {
  query.pageNo = 1
  query.keyword = ''
  query.transferType = ''
  query.applicationStatus = ''
  voiceQueryText.value = ''
  loadApplications()
}

/**
 * @brief 上传语音并调用 ASR，将识别文本作为转考申请查询关键字。
 *
 * @param uploadFile Element Plus 上传文件对象。
 */
async function handleVoiceQueryUploadChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) return

  await recognizeTransferVoiceFile(file)
}

/**
 * @brief 启停浏览器录音，将麦克风语音直接转为转考查询条件。
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
      const voiceFile = new File([voiceBlob], `transfer-voice-${Date.now()}.webm`, { type: voiceBlob.type })
      recognizeTransferVoiceFile(voiceFile)
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
 * @brief 调用 ASR 识别语音文件并刷新转考申请列表。
 *
 * @param file 浏览器录音或用户上传的语音文件。
 */
async function recognizeTransferVoiceFile(file: File) {

  voiceQueryLoading.value = true
  try {
    const uploadResult = await uploadSpeechAudio(file)
    const response = await recognizeSpeech({
      audioUrl: uploadResult.fileUrl,
      scene: 'TRANSFER',
      languageHint: 'zh-CN'
    })

    if (response.code !== 200) {
      ElMessage.error(response.message || '语音查询识别失败')
      return
    }

    voiceQueryText.value = response.data.text
    applyTransferVoiceCommand(response.data.text)
    query.pageNo = 1
    await loadApplications()
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
 * @brief 将转考查询结果摘要转换为语音播报。
 */
async function playTransferQueryNotice() {
  const content = lastNoticeText.value || buildTransferQueryNotice()
  if (!content) {
    ElMessage.warning('暂无可播报的查询结果')
    return
  }

  ttsLoading.value = true
  try {
    const response = await synthesizeSpeech({
      content,
      scene: 'TRANSFER'
    })

    if (response.code !== 200) {
      ElMessage.error(response.message || '语音播报调用失败')
      return
    }

    const audio = new Audio(response.data.audio_url)
    await audio.play().catch(() => undefined)
    ElMessage.success('转考查询结果播报已生成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '语音播报服务暂不可用')
  } finally {
    ttsLoading.value = false
  }
}

function openCreateDrawer() {
  formMode.value = 'create'
  selectedApplication.value = null
  resetForm()
  formDrawerVisible.value = true
  searchStudentRecords('')
}

async function openEditDrawer(row: TransferApplication) {
  formMode.value = 'edit'
  selectedApplication.value = await getTransferApplicationDetail(row.id)
  Object.assign(form, {
    id: selectedApplication.value.id,
    recordId: selectedApplication.value.recordId,
    transferType: selectedApplication.value.businessType || 'TRANSFER_IN',
    sourceProvince: selectedApplication.value.sourceProvince || '',
    sourceSchool: selectedApplication.value.sourceSchool || '',
    sourceRecordNo: selectedApplication.value.sourceRecordNo || '',
    targetProvince: selectedApplication.value.targetProvince || '',
    targetSchool: selectedApplication.value.targetSchool || '',
    transferReason: selectedApplication.value.transferReason || '',
    remark: selectedApplication.value.remark || ''
  })
  selectedMaterialIds.value = [...(selectedApplication.value.materialIds || [])]
  await loadRecordMaterials(selectedApplication.value.recordId)
  formDrawerVisible.value = true
}

async function openDetailDrawer(row: TransferApplication) {
  selectedApplication.value = await getTransferApplicationDetail(row.id)
  flowRecords.value = await listTransferFlowRecords(row.id)
  detailDrawerVisible.value = true
}

function openAuditDialog(row: TransferApplication, mode: 'approve' | 'reject') {
  selectedApplication.value = row
  auditMode.value = mode
  auditForm.auditOpinion = mode === 'approve' ? '材料齐全，符合转考办理要求。' : ''
  auditDialogVisible.value = true
}

function openWithdrawDialog(row: TransferApplication) {
  selectedApplication.value = row
  withdrawForm.withdrawReason = ''
  withdrawDialogVisible.value = true
}

function handleTransferTypeChange() {
  form.sourceProvince = ''
  form.sourceSchool = ''
  form.sourceRecordNo = ''
  form.targetProvince = ''
  form.targetSchool = ''
  formRef.value?.clearValidate(['sourceProvince', 'targetProvince'])
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      recordId: form.recordId,
      transferType: form.transferType,
      sourceProvince: form.sourceProvince || undefined,
      sourceSchool: form.sourceSchool || undefined,
      sourceRecordNo: form.sourceRecordNo || undefined,
      targetProvince: form.targetProvince || undefined,
      targetSchool: form.targetSchool || undefined,
      transferReason: form.transferReason,
      materialIds: selectedMaterialIds.value,
      remark: form.remark || undefined
    }
    if (formMode.value === 'create') {
      await submitTransferApplication(payload)
    } else {
      await updateTransferApplication(form.id, payload)
    }
    ElMessage.success('保存成功')
    formDrawerVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

async function submitAudit() {
  if (!selectedApplication.value) return
  if (!auditForm.auditOpinion.trim()) {
    ElMessage.warning('请输入审核意见')
    return
  }
  saving.value = true
  try {
    if (auditMode.value === 'approve') {
      await approveTransferApplication(selectedApplication.value.id, auditForm)
    } else {
      await rejectTransferApplication(selectedApplication.value.id, auditForm)
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
    await withdrawTransferApplication(selectedApplication.value.id, withdrawForm)
    ElMessage.success('撤回成功')
    withdrawDialogVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: 0,
    recordId: undefined,
    transferType: 'TRANSFER_IN',
    sourceProvince: '',
    sourceSchool: '',
    sourceRecordNo: '',
    targetProvince: '',
    targetSchool: '',
    transferReason: '',
    remark: ''
  })
  recordOptions.value = []
  selectedMaterialIds.value = []
  materialOptions.value = []
  formRef.value?.clearValidate()
}

/**
 * @brief 远程搜索可办理转考业务的考籍档案。
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
 * @brief 切换考籍档案后加载该档案材料并清空旧选择。
 *
 * @param recordId 当前选中的考籍档案主键。
 */
async function handleRecordChange(recordId?: number) {
  selectedMaterialIds.value = []
  await loadRecordMaterials(recordId)
}

/**
 * @brief 加载指定考籍档案的材料列表。
 *
 * @param recordId 考籍档案主键。
 */
async function loadRecordMaterials(recordId?: number) {
  if (!recordId) {
    materialOptions.value = []
    return
  }
  materialLoading.value = true
  try {
    materialOptions.value = await listRecordMaterials({ recordId })
  } finally {
    materialLoading.value = false
  }
}

/**
 * @brief 格式化考籍档案选项。
 *
 * @param record 考籍档案摘要。
 * @return 下拉框展示文本。
 */
function formatRecordOption(record: StudentRecord) {
  const idCard = record.idCard ? ` / ${record.idCard}` : ''
  const majorName = record.majorName ? ` / ${record.majorName}` : ''
  return `${record.recordNo} / ${record.candidateName || '未知考生'}${idCard}${majorName}`
}

/**
 * @brief 格式化材料选项。
 *
 * @param material 档案材料摘要。
 * @return 下拉框展示文本。
 */
function formatMaterialOption(material: RecordMaterial) {
  const fileName = material.originalFileName || material.fileName || `材料${material.id}`
  return `${material.materialType} / ${fileName} / ${auditStatusText(material.auditStatus)}`
}

function transferTypeText(type?: string) {
  const typeMap: Record<string, string> = {
    TRANSFER_IN: '考籍转入',
    TRANSFER_OUT: '考籍转出'
  }
  return typeMap[type || ''] || type || '-'
}

function transferTypeTag(type?: string) {
  return type === 'TRANSFER_OUT' ? 'warning' : 'primary'
}

function applicationStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    SUBMITTED: '已提交',
    AUDITING: '审核中',
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

function auditStatusText(status?: string) {
  const statusMap: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return statusMap[status || ''] || status || '-'
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

/**
 * @brief 根据语音指令填充转考查询条件。
 *
 * @param text ASR 识别出的完整语音文本。
 */
function applyTransferVoiceCommand(text: string) {
  const normalizedText = text.trim()
  query.keyword = extractTransferKeyword(normalizedText)

  if (normalizedText.includes('转入')) {
    query.transferType = 'TRANSFER_IN'
  } else if (normalizedText.includes('转出')) {
    query.transferType = 'TRANSFER_OUT'
  }

  if (normalizedText.includes('通过')) {
    query.applicationStatus = 'APPROVED'
  } else if (normalizedText.includes('驳回') || normalizedText.includes('拒绝')) {
    query.applicationStatus = 'REJECTED'
  } else if (normalizedText.includes('撤回')) {
    query.applicationStatus = 'WITHDRAWN'
  } else if (normalizedText.includes('提交') || normalizedText.includes('待审')) {
    query.applicationStatus = 'SUBMITTED'
  }
}

/**
 * @brief 从语音识别文本中提取转考申请检索关键字。
 *
 * @param text ASR 识别出的完整语音文本。
 * @return 用于申请编号、姓名或考籍号模糊查询的关键字。
 */
function extractTransferKeyword(text: string) {
  const codeMatch = text.match(/[A-Za-z]{1,4}[0-9A-Za-z]{6,}/)
  if (codeMatch) return codeMatch[0]

  const numberMatch = text.match(/[0-9Xx]{6,}/)
  if (numberMatch) return numberMatch[0]

  return text
    .replace(/请|帮我|查询|查一下|转入|转出|转考|申请|考籍|档案|考生|信息|状态|业务|办理|通过|驳回|拒绝|撤回|提交|待审/g, '')
    .replace(/[，。,.？?\s]/g, '')
    .trim()
}

/**
 * @brief 构造转考申请查询结果播报文本。
 *
 * @return 可提交给 TTS 的查询结果摘要。
 */
function buildTransferQueryNotice() {
  const keywordText = query.keyword ? `关键字 ${query.keyword}` : '当前条件'
  if (total.value === 0) {
    return `${keywordText} 未查询到匹配的转考申请，请调整条件后重试。`
  }

  const firstApplication = applications.value[0]
  const firstApplicationText = firstApplication
    ? `首条申请为 ${firstApplication.candidateName || '未知考生'}，申请编号 ${firstApplication.applicationNo}，${transferTypeText(firstApplication.businessType)}，状态 ${applicationStatusText(firstApplication.applicationStatus)}。`
    : ''
  return `${keywordText} 共查询到 ${total.value} 条转考申请。${firstApplicationText}`
}

onMounted(loadApplications)
</script>

<style scoped>
.transfer-page {
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

.full-select {
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

.section-title {
  margin: 18px 0 12px;
  font-size: 16px;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}

.flow-status,
.flow-opinion {
  color: #64748b;
  font-weight: 400;
}

.flow-opinion {
  margin: 6px 0 0;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
