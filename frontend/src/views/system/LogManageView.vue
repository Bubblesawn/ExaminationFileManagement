<template>
  <div class="page log-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">日志管理</h2>
        <p class="page-subtitle">查询系统登录日志与操作日志，查看审计详情和异常原因。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="log-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="登录日志" name="login">
        <el-card shadow="never" class="query-card">
          <el-form :inline="true" :model="loginQuery" class="query-form" @submit.prevent>
            <el-form-item label="登录账号">
              <el-input
                v-model.trim="loginQuery.username"
                clearable
                class="query-input"
                placeholder="请输入登录账号"
                @keyup.enter="searchLoginLogs"
              />
            </el-form-item>
            <el-form-item label="登录状态">
              <el-select v-model="loginQuery.loginStatus" clearable placeholder="全部状态" class="status-select">
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAIL" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchLoginLogs">查询</el-button>
              <el-button :icon="Refresh" @click="resetLoginQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <el-table v-loading="loginLoading" :data="loginLogs" border>
            <el-table-column prop="username" label="登录账号" min-width="130" />
            <el-table-column prop="loginStatus" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.loginStatus)">{{ statusText(row.loginStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failureReason" label="失败原因" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.failureReason || '-' }}</template>
            </el-table-column>
            <el-table-column prop="loginIp" label="登录 IP" min-width="140" />
            <el-table-column prop="loginTime" label="登录时间" min-width="180" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="openLoginDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="loginQuery.pageNo"
              v-model:page-size="loginQuery.pageSize"
              :total="loginTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadLoginLogs"
              @current-change="loadLoginLogs"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="operation">
        <el-card shadow="never" class="query-card">
          <el-form :inline="true" :model="operationQuery" class="query-form" @submit.prevent>
            <el-form-item label="模块名称">
              <el-input
                v-model.trim="operationQuery.moduleName"
                clearable
                class="query-input"
                placeholder="请输入模块名称"
                @keyup.enter="searchOperationLogs"
              />
            </el-form-item>
            <el-form-item label="操作人">
              <el-input
                v-model.trim="operationQuery.operatorName"
                clearable
                class="query-input"
                placeholder="请输入操作人"
                @keyup.enter="searchOperationLogs"
              />
            </el-form-item>
            <el-form-item label="操作状态">
              <el-select v-model="operationQuery.operationStatus" clearable placeholder="全部状态" class="status-select">
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAIL" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchOperationLogs">查询</el-button>
              <el-button :icon="Refresh" @click="resetOperationQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <el-table v-loading="operationLoading" :data="operationLogs" border>
            <el-table-column prop="moduleName" label="模块" min-width="120" />
            <el-table-column prop="operationType" label="类型" width="110" />
            <el-table-column prop="requestMethod" label="方法" width="90" align="center">
              <template #default="{ row }">
                <el-tag type="info">{{ row.requestMethod || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="requestUri" label="请求地址" min-width="220" show-overflow-tooltip />
            <el-table-column prop="operatorName" label="操作人" min-width="120" />
            <el-table-column prop="operationStatus" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.operationStatus)">{{ statusText(row.operationStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="costTime" label="耗时" width="100" align="right">
              <template #default="{ row }">{{ formatCostTime(row.costTime) }}</template>
            </el-table-column>
            <el-table-column prop="operationTime" label="操作时间" min-width="180" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="openOperationDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="operationQuery.pageNo"
              v-model:page-size="operationQuery.pageSize"
              :total="operationTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadOperationLogs"
              @current-change="loadOperationLogs"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="loginDetailVisible" title="登录日志详情" size="520px" destroy-on-close>
      <el-descriptions v-if="loginDetail" :column="1" border>
        <el-descriptions-item label="日志 ID">{{ loginDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ loginDetail.userId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录账号">{{ loginDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="登录状态">
          <el-tag :type="statusTag(loginDetail.loginStatus)">{{ statusText(loginDetail.loginStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败原因">{{ loginDetail.failureReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录 IP">{{ loginDetail.loginIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录时间">{{ loginDetail.loginTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户代理">
          <div class="detail-text">{{ loginDetail.userAgent || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-drawer v-model="operationDetailVisible" title="操作日志详情" size="640px" destroy-on-close>
      <el-descriptions v-if="operationDetail" :column="1" border>
        <el-descriptions-item label="日志 ID">{{ operationDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="模块名称">{{ operationDetail.moduleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ operationDetail.operationType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ operationDetail.operationDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ operationDetail.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ operationDetail.requestUri || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="statusTag(operationDetail.operationStatus)">{{ statusText(operationDetail.operationStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">
          <div class="detail-text">{{ operationDetail.errorMessage || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ operationDetail.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作 IP">{{ operationDetail.operationIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ operationDetail.operationTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatCostTime(operationDetail.costTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="detail-code">{{ formatJsonLikeText(operationDetail.requestParam) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应摘要">
          <pre class="detail-code">{{ formatJsonLikeText(operationDetail.responseResult) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import {
  getLoginLogDetail,
  getOperationLogDetail,
  pageLoginLogs,
  pageOperationLogs,
  type LogStatus,
  type LoginLogRecord,
  type OperationLogRecord
} from '../../api/systemLog'

type LogTabName = 'login' | 'operation'

const activeTab = ref<LogTabName>('login')
const loginLoading = ref(false)
const operationLoading = ref(false)
const loginDetailVisible = ref(false)
const operationDetailVisible = ref(false)
const loginLogs = ref<LoginLogRecord[]>([])
const operationLogs = ref<OperationLogRecord[]>([])
const loginDetail = ref<LoginLogRecord | null>(null)
const operationDetail = ref<OperationLogRecord | null>(null)
const loginTotal = ref(0)
const operationTotal = ref(0)

const loginQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  username: '',
  loginStatus: '' as LogStatus | ''
})

const operationQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  moduleName: '',
  operatorName: '',
  operationStatus: '' as LogStatus | ''
})

/**
 * @brief 加载登录日志分页数据。
 */
async function loadLoginLogs() {
  loginLoading.value = true
  try {
    const result = await pageLoginLogs({
      pageNo: loginQuery.pageNo,
      pageSize: loginQuery.pageSize,
      username: loginQuery.username || undefined,
      loginStatus: loginQuery.loginStatus || undefined
    })
    loginLogs.value = result.records ?? []
    loginTotal.value = result.total ?? 0
  } finally {
    loginLoading.value = false
  }
}

/**
 * @brief 按当前条件查询登录日志，并回到第一页。
 */
function searchLoginLogs() {
  loginQuery.pageNo = 1
  loadLoginLogs()
}

/**
 * @brief 重置登录日志查询条件。
 */
function resetLoginQuery() {
  loginQuery.pageNo = 1
  loginQuery.username = ''
  loginQuery.loginStatus = ''
  loadLoginLogs()
}

/**
 * @brief 加载操作日志分页数据。
 */
async function loadOperationLogs() {
  operationLoading.value = true
  try {
    const result = await pageOperationLogs({
      pageNo: operationQuery.pageNo,
      pageSize: operationQuery.pageSize,
      moduleName: operationQuery.moduleName || undefined,
      operatorName: operationQuery.operatorName || undefined,
      operationStatus: operationQuery.operationStatus || undefined
    })
    operationLogs.value = result.records ?? []
    operationTotal.value = result.total ?? 0
  } finally {
    operationLoading.value = false
  }
}

/**
 * @brief 按当前条件查询操作日志，并回到第一页。
 */
function searchOperationLogs() {
  operationQuery.pageNo = 1
  loadOperationLogs()
}

/**
 * @brief 重置操作日志查询条件。
 */
function resetOperationQuery() {
  operationQuery.pageNo = 1
  operationQuery.moduleName = ''
  operationQuery.operatorName = ''
  operationQuery.operationStatus = ''
  loadOperationLogs()
}

/**
 * @brief 切换日志页签时按需加载对应列表。
 *
 * @param name 当前选中的页签名称。
 */
function handleTabChange(name: string | number) {
  activeTab.value = name as LogTabName
  if (activeTab.value === 'operation' && operationLogs.value.length === 0) {
    loadOperationLogs()
  }
}

/**
 * @brief 打开登录日志详情抽屉。
 *
 * @param row 当前选中的登录日志行。
 */
async function openLoginDetail(row: LoginLogRecord) {
  loginDetail.value = await getLoginLogDetail(row.id)
  loginDetailVisible.value = true
}

/**
 * @brief 打开操作日志详情抽屉。
 *
 * @param row 当前选中的操作日志行。
 */
async function openOperationDetail(row: OperationLogRecord) {
  operationDetail.value = await getOperationLogDetail(row.id)
  operationDetailVisible.value = true
}

/**
 * @brief 将日志状态编码转换为中文展示文本。
 *
 * @param status 日志状态编码。
 * @return 中文状态名称。
 */
function statusText(status?: LogStatus) {
  return status === 'SUCCESS' ? '成功' : '失败'
}

/**
 * @brief 根据日志状态选择标签样式。
 *
 * @param status 日志状态编码。
 * @return Element Plus 标签类型。
 */
function statusTag(status?: LogStatus) {
  return status === 'SUCCESS' ? 'success' : 'danger'
}

/**
 * @brief 格式化接口耗时展示。
 *
 * @param costTime 接口耗时，单位毫秒。
 * @return 带单位的耗时文本。
 */
function formatCostTime(costTime?: number) {
  return typeof costTime === 'number' ? `${costTime} ms` : '-'
}

/**
 * @brief 尝试格式化 JSON 字符串，失败时保留原文本。
 *
 * @param value 日志中记录的请求参数或响应摘要。
 * @return 适合详情区展示的文本。
 */
function formatJsonLikeText(value?: string) {
  if (!value) {
    return '-'
  }

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(loadLoginLogs)
</script>

<style scoped>
.log-page {
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

.log-tabs {
  --el-tabs-header-height: 44px;
}

.query-card,
.table-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.query-card {
  margin-bottom: 14px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
}

.query-input {
  width: 220px;
}

.status-select {
  width: 150px;
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

.detail-code {
  max-height: 260px;
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-family: Consolas, "Courier New", monospace;
  font-size: 13px;
}
</style>
