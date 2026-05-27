<template>
  <div class="page ai-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">智能辅助</h2>
      </div>
      <el-tag type="info" effect="plain">第五阶段 5.9 / 5.10</el-tag>
    </div>

    <el-tabs v-model="activeTab" class="ai-tabs">
      <el-tab-pane label="智能识别确认" name="recognition">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="8">
            <section class="task-panel">
              <h3>识别任务</h3>
              <el-form label-position="top">
                <el-form-item label="任务类型">
                  <el-segmented v-model="form.taskType" :options="taskOptions" />
                </el-form-item>
                <el-form-item label="图片地址">
                  <el-input v-model="form.fileUrl" placeholder="请输入图片访问地址" clearable />
                </el-form-item>
                <el-form-item label="文件名称">
                  <el-input v-model="form.fileName" placeholder="例如 idcard-sample.jpg" clearable />
                </el-form-item>
                <el-row :gutter="12">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="业务编号">
                      <el-input-number v-model="form.businessId" :min="1" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="业务场景">
                      <el-select v-model="form.scene" placeholder="请选择">
                        <el-option label="材料审核" value="MATERIAL_AUDIT" />
                        <el-option label="免考申请" value="EXEMPTION" />
                        <el-option label="毕业申请" value="GRADUATION" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="材料类型提示">
                  <el-select v-model="form.materialTypeHint" clearable placeholder="可选">
                    <el-option label="身份证材料" value="身份证" />
                    <el-option label="准考证材料" value="准考证" />
                    <el-option label="学历证书材料" value="毕业证" />
                    <el-option label="成绩单材料" value="成绩单" />
                    <el-option label="免考证明材料" value="免考证明" />
                    <el-option label="考生照片" value="照片" />
                  </el-select>
                </el-form-item>
                <el-button type="primary" :loading="loading" class="submit-button" @click="submitRecognition">
                  发起识别
                </el-button>
              </el-form>
            </section>

            <section class="confirm-panel">
              <h3>确认记录</h3>
              <el-empty v-if="confirmedRecords.length === 0" description="暂无确认记录" />
              <el-timeline v-else>
                <el-timeline-item v-for="item in confirmedRecords" :key="item.id" :timestamp="item.time">
                  <div class="record-title">{{ item.categoryName || '未命名材料' }}</div>
                  <div class="record-meta">{{ item.actionText }} · {{ item.remark || '未填写修正说明' }}</div>
                </el-timeline-item>
              </el-timeline>
            </section>
          </el-col>

          <el-col :xs="24" :lg="16">
            <RecognitionResultPanel
              :result="recognitionResult"
              :task-name="currentTaskLabel"
              @confirm="handleConfirm"
            />
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="智能问答" name="chat">
        <AiChatPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AiChatPanel from '../../components/ai/AiChatPanel.vue'
import RecognitionResultPanel from '../../components/ai/RecognitionResultPanel.vue'
import { recognizeImage, type AiRecognitionData, type AiTaskType, type SuggestedAction } from '../../api/ai'

interface ConfirmRecord {
  id: number
  time: string
  categoryName: string
  actionText: string
  remark: string
}

const taskOptions = [
  { label: '图像分类', value: 'classify' },
  { label: '目标检测', value: 'detect' },
  { label: '图像分割', value: 'segment' }
]

const actionTextMap: Record<SuggestedAction, string> = {
  ACCEPT: '确认通过',
  REVIEW: '转人工复核',
  REJECT: '退回处理'
}

const form = reactive({
  taskType: 'classify' as AiTaskType,
  fileUrl: 'https://example.com/mock/idcard-sample.jpg',
  fileName: 'idcard-sample.jpg',
  businessId: 2026052701,
  scene: 'MATERIAL_AUDIT',
  materialTypeHint: '身份证'
})

const loading = ref(false)
const activeTab = ref('recognition')
const recognitionResult = ref<AiRecognitionData | null>(null)
const confirmedRecords = ref<ConfirmRecord[]>([])

const currentTaskLabel = computed(() => taskOptions.find((item) => item.value === form.taskType)?.label ?? '智能识别')

/**
 * @brief 发起图片智能识别，并将算法结果送入人工确认组件。
 */
async function submitRecognition() {
  if (!form.fileUrl.trim()) {
    ElMessage.warning('请先填写图片地址')
    return
  }

  loading.value = true
  try {
    const response = await recognizeImage(form.taskType, {
      fileUrl: form.fileUrl,
      businessId: form.businessId,
      scene: form.scene,
      fileName: form.fileName,
      materialTypeHint: form.materialTypeHint
    })

    if (response.code !== 200 || response.data.code !== 200) {
      ElMessage.error(response.data?.message || response.message || '智能识别调用失败')
      return
    }

    recognitionResult.value = response.data.data
    ElMessage.success('识别结果已生成，请人工确认')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能识别服务暂不可用')
  } finally {
    loading.value = false
  }
}

/**
 * @brief 接收人工确认后的修正结果，供后续业务保存接口对接。
 *
 * @param payload 人工确认结果和修正说明。
 */
function handleConfirm(payload: { result: AiRecognitionData; remark: string }) {
  recognitionResult.value = payload.result
  confirmedRecords.value.unshift({
    id: Date.now(),
    time: new Date().toLocaleString('zh-CN', { hour12: false }),
    categoryName: payload.result.category_name || payload.result.material_type_hint || '',
    actionText: actionTextMap[payload.result.suggested_action ?? 'REVIEW'],
    remark: payload.remark
  })
  ElMessage.success('人工确认结果已记录')
}
</script>

<style scoped>
.ai-page {
  display: grid;
  gap: 16px;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.task-panel,
.confirm-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.ai-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.confirm-panel {
  margin-top: 16px;
}

.task-panel h3,
.confirm-panel h3 {
  margin: 0 0 14px;
  font-size: 16px;
  color: #111827;
}

.task-panel :deep(.el-input-number),
.task-panel :deep(.el-select),
.task-panel :deep(.el-segmented),
.submit-button {
  width: 100%;
}

.record-title {
  color: #1f2937;
  font-weight: 700;
}

.record-meta {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 768px) {
  .page-heading {
    flex-direction: column;
  }
}
</style>
