<template>
  <div class="page">
    <h2 class="page-title">工作台</h2>
    <el-row :gutter="16">
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
const stats = [
  { label: '考籍档案', value: 0 },
  { label: '待审材料', value: 0 },
  { label: '免考申请', value: 0 },
  { label: '毕业申请', value: 0 }
]
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

