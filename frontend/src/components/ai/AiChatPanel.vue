<template>
  <section class="chat-shell">
    <div class="chat-main">
      <div class="chat-header">
        <div>
          <h3>智能问答</h3>
        </div>
        <el-tag v-if="currentAnswer?.need_manual_review" type="warning" effect="plain">建议人工确认</el-tag>
      </div>

      <el-form label-position="top" class="question-form">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="16">
            <el-form-item label="问题内容">
              <el-input
                v-model="question"
                type="textarea"
                :rows="4"
                maxlength="300"
                show-word-limit
                placeholder="请输入要咨询的办理问题"
                @keydown.ctrl.enter.prevent="submitQuestion()"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-row :gutter="12">
              <el-col :xs="24">
                <el-form-item label="业务场景">
                  <el-select v-model="scene" placeholder="请选择">
                    <el-option label="考籍档案" value="ARCHIVE" />
                    <el-option label="材料审核" value="MATERIAL_AUDIT" />
                    <el-option label="免考申请" value="EXEMPTION" />
                    <el-option label="课程顶替" value="COURSE_REPLACE" />
                    <el-option label="毕业申请" value="GRADUATION" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24">
                <el-form-item label="业务编号">
                  <el-input-number v-model="businessId" :min="1" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
        </el-row>

        <div class="question-actions">
          <el-button @click="question = ''">清空</el-button>
          <el-button type="primary" :loading="loading" @click="submitQuestion">提交问题</el-button>
        </div>
      </el-form>

      <div class="answer-area">
        <el-empty v-if="!currentAnswer" description="暂无回答" />
        <template v-else>
          <div class="answer-card">
            <div class="answer-meta">
              <el-tag type="success" effect="light">{{ currentAnswer.intent_name }}</el-tag>
              <span>匹配度 {{ formatPercent(currentAnswer.confidence) }}</span>
            </div>
            <h4>{{ currentAnswer.question }}</h4>
            <p>{{ currentAnswer.answer }}</p>
          </div>

          <div v-if="currentAnswer.references.length" class="info-block">
            <div class="block-title">参考依据</div>
            <el-collapse accordion>
              <el-collapse-item
                v-for="reference in currentAnswer.references"
                :key="`${reference.title}-${reference.source}`"
                :title="reference.title"
              >
                <p>{{ reference.content }}</p>
                <span>来源：{{ reference.source }}</span>
              </el-collapse-item>
            </el-collapse>
          </div>

          <div v-if="currentAnswer.suggestions.length" class="info-block">
            <div class="block-title">可继续追问</div>
            <div class="suggestion-list">
              <el-button
                v-for="suggestion in currentAnswer.suggestions"
                :key="suggestion"
                plain
                @click="useSuggestion(suggestion)"
              >
                {{ suggestion }}
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <aside class="history-panel">
      <div class="history-header">
        <h3>问答历史</h3>
        <el-button text type="primary" :disabled="historyRecords.length === 0" @click="clearHistory">清空</el-button>
      </div>

      <el-empty v-if="historyRecords.length === 0" description="暂无历史记录" />
      <div v-else class="history-list">
        <button v-for="record in historyRecords" :key="record.id" type="button" @click="selectHistory(record)">
          <span>{{ record.answer.question }}</span>
          <small>{{ record.time }} · {{ record.answer.intent_name }}</small>
        </button>
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { askAiQuestion, type ChatAnswerData } from '../../api/ai'

interface ChatHistoryRecord {
  id: number
  time: string
  answer: ChatAnswerData
}

const HISTORY_STORAGE_KEY = 'exam-ai-chat-history'
const MAX_HISTORY_COUNT = 10

const question = ref('材料上传后智能审核不通过应该怎么处理？')
const scene = ref('MATERIAL_AUDIT')
const businessId = ref(2026052701)
const loading = ref(false)
const currentAnswer = ref<ChatAnswerData | null>(null)
const historyRecords = ref<ChatHistoryRecord[]>([])

onMounted(() => {
  historyRecords.value = loadHistory()
})

/**
 * @brief 提交智能问答请求，并将回答写入本地历史。
 */
async function submitQuestion() {
  const content = question.value.trim()
  if (!content) {
    ElMessage.warning('请先输入问题内容')
    return
  }

  loading.value = true
  try {
    const response = await askAiQuestion({
      content,
      businessId: businessId.value,
      scene: scene.value
    })

    if (response.code !== 200 || response.data.code !== 200) {
      ElMessage.error(response.data?.message || response.message || '智能问答调用失败')
      return
    }

    currentAnswer.value = normalizeAnswer(response.data.data, content)
    saveHistory(currentAnswer.value)
    ElMessage.success('智能问答已返回')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能问答服务暂不可用')
  } finally {
    loading.value = false
  }
}

function useSuggestion(suggestion: string) {
  question.value = suggestion
}

function selectHistory(record: ChatHistoryRecord) {
  currentAnswer.value = record.answer
  question.value = record.answer.question
  scene.value = record.answer.scene || scene.value
  businessId.value = record.answer.business_id || businessId.value
}

async function clearHistory() {
  try {
    await ElMessageBox.confirm('确认清空本地问答历史吗？', '清空历史', {
      type: 'warning',
      confirmButtonText: '确认清空',
      cancelButtonText: '取消'
    })
    historyRecords.value = []
    localStorage.removeItem(HISTORY_STORAGE_KEY)
    ElMessage.success('问答历史已清空')
  } catch {
    // 用户取消时无需提示。
  }
}

/**
 * @brief 规范化问答结果，兜底后端缺失的数组字段。
 *
 * @param data 后端算法问答结果。
 * @param fallbackQuestion 当前输入的问题。
 * @return 可直接渲染的问答结果。
 */
function normalizeAnswer(data: ChatAnswerData, fallbackQuestion: string): ChatAnswerData {
  return {
    ...data,
    question: data.question || fallbackQuestion,
    references: data.references ?? [],
    suggestions: data.suggestions ?? []
  }
}

function saveHistory(answer: ChatAnswerData) {
  const record: ChatHistoryRecord = {
    id: Date.now(),
    time: new Date().toLocaleString('zh-CN', { hour12: false }),
    answer
  }
  historyRecords.value = [record, ...historyRecords.value].slice(0, MAX_HISTORY_COUNT)
  localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(historyRecords.value))
}

function loadHistory(): ChatHistoryRecord[] {
  const rawHistory = localStorage.getItem(HISTORY_STORAGE_KEY)
  if (!rawHistory) return []

  try {
    const parsed = JSON.parse(rawHistory)
    return Array.isArray(parsed) ? parsed.slice(0, MAX_HISTORY_COUNT) : []
  } catch {
    localStorage.removeItem(HISTORY_STORAGE_KEY)
    return []
  }
}

function formatPercent(value?: number) {
  return `${Number(((value ?? 0) * 100).toFixed(1))}%`
}
</script>

<style scoped>
.chat-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.chat-main,
.history-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.chat-header,
.history-header,
.question-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-header {
  margin-bottom: 16px;
}

.chat-header h3,
.history-header h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.question-form :deep(.el-input-number),
.question-form :deep(.el-select) {
  width: 100%;
}

.question-actions {
  justify-content: flex-end;
}

.answer-area {
  margin-top: 18px;
}

.answer-card {
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

.answer-card h4 {
  margin: 12px 0 8px;
  color: #111827;
  font-size: 16px;
}

.answer-card p {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.answer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.info-block {
  margin-top: 16px;
}

.block-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.info-block :deep(.el-collapse-item__content) {
  color: #475569;
  line-height: 1.7;
}

.info-block :deep(.el-collapse-item__content p) {
  margin: 0 0 8px;
}

.info-block :deep(.el-collapse-item__content span) {
  color: #64748b;
  font-size: 12px;
}

.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-list {
  display: grid;
  gap: 10px;
}

.history-list button {
  display: grid;
  width: 100%;
  gap: 6px;
  padding: 10px 12px;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
}

.history-list button:hover {
  border-color: #409eff;
}

.history-list span {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.history-list small {
  color: #667085;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .chat-header,
  .history-header,
  .question-actions,
  .answer-meta {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
