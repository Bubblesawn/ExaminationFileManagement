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
import MaterialUploadPreview from '../../components/material/MaterialUploadPreview.vue'
import { pageStudentRecords, type StudentRecord } from '../../api/record'

const loading = ref(false)
const records = ref<StudentRecord[]>([])
const selectedRecord = ref<StudentRecord | null>(null)
const materialDrawerVisible = ref(false)
const total = ref(0)

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
  loadRecords()
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
