<template>
  <div class="page change-log-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">档案变更记录</h2>
        <p class="page-subtitle">按考籍档案查看操作人、操作时间、变更内容和变更原因。</p>
      </div>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="档案">
          <el-select
            v-model="selectedRecordId"
            filterable
            remote
            reserve-keyword
            clearable
            class="record-select"
            placeholder="请输入考籍号、姓名或身份证号"
            :remote-method="searchRecordOptions"
            :loading="recordOptionLoading"
            @change="handleRecordChange"
          >
            <el-option
              v-for="record in recordOptions"
              :key="record.id"
              :label="recordOptionLabel(record)"
              :value="record.id"
            >
              <div class="record-option">
                <span>{{ record.recordNo }}</span>
                <small>{{ record.candidateName || '-' }} / {{ record.majorName || '-' }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="query.changeType" clearable class="type-select" placeholder="全部类型">
            <el-option label="创建档案" value="CREATE" />
            <el-option label="编辑档案" value="UPDATE" />
            <el-option label="状态变更" value="STATUS_CHANGE" />
            <el-option label="档案归档" value="ARCHIVE" />
            <el-option label="材料变更" value="MATERIAL_CHANGE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :disabled="!selectedRecordId" @click="searchChangeLogs">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-alert
        v-if="!selectedRecordId"
        class="select-alert"
        type="info"
        show-icon
        :closable="false"
        title="请选择一个考籍档案后查看变更记录。"
      />

      <el-table v-loading="loading" :data="changeLogs" border>
        <el-table-column prop="changeType" label="变更类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="changeTypeTag(row.changeType)">{{ changeTypeText(row.changeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeField" label="变更字段" min-width="130">
          <template #default="{ row }">{{ fieldText(row.changeField) }}</template>
        </el-table-column>
        <el-table-column label="变更内容" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ changeContentText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="changeReason" label="变更原因" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.changeReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" min-width="120">
          <template #default="{ row }">{{ row.operatorName || '系统' }}</template>
        </el-table-column>
        <el-table-column prop="operationTime" label="操作时间" min-width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
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
          @size-change="loadChangeLogs"
          @current-change="loadChangeLogs"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="变更记录详情" size="560px" destroy-on-close>
      <el-descriptions v-if="changeLogDetail" :column="1" border>
        <el-descriptions-item label="记录 ID">{{ changeLogDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="档案 ID">{{ changeLogDetail.recordId }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">
          <el-tag :type="changeTypeTag(changeLogDetail.changeType)">
            {{ changeTypeText(changeLogDetail.changeType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="变更字段">{{ fieldText(changeLogDetail.changeField) }}</el-descriptions-item>
        <el-descriptions-item label="变更前">
          <div class="detail-text">{{ changeLogDetail.beforeValue || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="变更后">
          <div class="detail-text">{{ changeLogDetail.afterValue || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="变更原因">
          <div class="detail-text">{{ changeLogDetail.changeReason || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ changeLogDetail.operatorName || '系统' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ changeLogDetail.operationTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import {
  pageRecordChangeLogs,
  pageStudentRecords,
  type RecordChangeLog,
  type StudentRecord
} from '../../api/record'

const loading = ref(false)
const recordOptionLoading = ref(false)
const detailVisible = ref(false)
const selectedRecordId = ref<number>()
const recordOptions = ref<StudentRecord[]>([])
const changeLogs = ref<RecordChangeLog[]>([])
const changeLogDetail = ref<RecordChangeLog | null>(null)
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  changeType: ''
})

/**
 * @brief 加载可供选择的考籍档案。
 *
 * @param keyword 档案关键字，支持考籍号、姓名或身份证号。
 */
async function loadRecordOptions(keyword?: string) {
  recordOptionLoading.value = true
  try {
    const result = await pageStudentRecords({
      pageNo: 1,
      pageSize: 20,
      keyword: keyword || undefined
    })
    recordOptions.value = result.records ?? []
    if (!selectedRecordId.value && recordOptions.value.length > 0) {
      selectedRecordId.value = recordOptions.value[0].id
      await loadChangeLogs()
    }
  } finally {
    recordOptionLoading.value = false
  }
}

/**
 * @brief 远程搜索档案下拉选项。
 *
 * @param keyword 用户输入的档案查询关键字。
 */
function searchRecordOptions(keyword: string) {
  loadRecordOptions(keyword)
}

/**
 * @brief 加载当前档案的变更记录分页数据。
 */
async function loadChangeLogs() {
  if (!selectedRecordId.value) {
    changeLogs.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const result = await pageRecordChangeLogs(selectedRecordId.value, {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      changeType: query.changeType || undefined
    })
    changeLogs.value = result.records ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

/**
 * @brief 切换档案时重新查询第一页变更记录。
 */
function handleRecordChange() {
  query.pageNo = 1
  loadChangeLogs()
}

/**
 * @brief 按当前筛选条件查询变更记录。
 */
function searchChangeLogs() {
  query.pageNo = 1
  loadChangeLogs()
}

/**
 * @brief 重置筛选条件并重新加载默认档案。
 */
async function resetQuery() {
  query.pageNo = 1
  query.pageSize = 10
  query.changeType = ''
  selectedRecordId.value = undefined
  changeLogs.value = []
  total.value = 0
  await loadRecordOptions()
}

/**
 * @brief 打开变更记录详情抽屉。
 *
 * @param row 当前选择的变更记录。
 */
function openDetail(row: RecordChangeLog) {
  changeLogDetail.value = row
  detailVisible.value = true
}

/**
 * @brief 生成档案下拉选项展示文本。
 *
 * @param record 考籍档案摘要。
 * @return 档案选项文本。
 */
function recordOptionLabel(record: StudentRecord) {
  return `${record.recordNo} - ${record.candidateName || '未知考生'}`
}

/**
 * @brief 将变更类型编码转换为中文。
 *
 * @param type 变更类型编码。
 * @return 中文变更类型。
 */
function changeTypeText(type?: string) {
  const typeMap: Record<string, string> = {
    CREATE: '创建档案',
    UPDATE: '编辑档案',
    STATUS_CHANGE: '状态变更',
    ARCHIVE: '档案归档',
    MATERIAL_CHANGE: '材料变更'
  }
  return typeMap[type || ''] || type || '-'
}

/**
 * @brief 根据变更类型选择标签样式。
 *
 * @param type 变更类型编码。
 * @return Element Plus 标签类型。
 */
function changeTypeTag(type?: string) {
  const tagMap: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    CREATE: 'success',
    UPDATE: 'primary',
    STATUS_CHANGE: 'warning',
    ARCHIVE: 'info',
    MATERIAL_CHANGE: 'danger'
  }
  return tagMap[type || ''] || 'info'
}

/**
 * @brief 将档案字段名转换为中文展示。
 *
 * @param field 字段编码。
 * @return 中文字段名。
 */
function fieldText(field?: string) {
  const fieldMap: Record<string, string> = {
    recordNo: '考籍号',
    enrollBatch: '入籍批次',
    educationLevel: '学历层次',
    majorCode: '专业代码',
    majorName: '专业名称',
    recordStatus: '考籍状态',
    archiveStatus: '归档状态',
    remark: '备注',
    material: '档案材料'
  }
  return fieldMap[field || ''] || field || '-'
}

/**
 * @brief 组合变更前后内容，便于列表快速阅读。
 *
 * @param row 变更记录。
 * @return 变更内容摘要。
 */
function changeContentText(row: RecordChangeLog) {
  if (!row.beforeValue && !row.afterValue) {
    return '-'
  }
  if (!row.beforeValue) {
    return `新增为：${row.afterValue}`
  }
  if (!row.afterValue) {
    return `由“${row.beforeValue}”清空`
  }
  return `由“${row.beforeValue}”变更为“${row.afterValue}”`
}

onMounted(() => loadRecordOptions())
</script>

<style scoped>
.change-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-heading {
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
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
}

.record-select {
  width: 340px;
}

.type-select {
  width: 150px;
}

.record-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.record-option small {
  color: #64748b;
}

.select-alert {
  margin-bottom: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.detail-text {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 768px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .record-select,
  .type-select {
    width: 100%;
  }
}
</style>
