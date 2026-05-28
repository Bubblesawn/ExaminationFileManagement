<template>
  <div class="page">
    <h2 class="page-title">工作台</h2>
    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="item in stats" :key="item.label" :span="6">
        <el-card shadow="never">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardStats, type DashboardStats } from '../../api/dashboard'

const loading = ref(false)
const dashboardStats = ref<DashboardStats>({
  recordCount: 0,
  pendingMaterialCount: 0,
  exemptionApplicationCount: 0,
  graduationApplicationCount: 0
})

const stats = computed(() => [
  { label: '考籍档案', value: dashboardStats.value.recordCount },
  { label: '待审材料', value: dashboardStats.value.pendingMaterialCount },
  { label: '免考申请', value: dashboardStats.value.exemptionApplicationCount },
  { label: '毕业申请', value: dashboardStats.value.graduationApplicationCount }
])

/**
 * @brief 加载工作台统计面板数据。
 *
 * @details
 * 通过后端聚合统计接口一次性获取首页面板数量，接口异常时保留默认值并提示用户。
 */
async function loadDashboardStats() {
  loading.value = true
  try {
    dashboardStats.value = await getDashboardStats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '工作台统计加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardStats()
})
</script>

<style scoped>
.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  margin-top: 8px;
  color: #667085;
}
</style>
