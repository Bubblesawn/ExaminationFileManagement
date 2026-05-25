<template>
  <div class="page">
    <h2 class="page-title">考生管理</h2>
    <el-card shadow="never">
      <el-form :inline="true">
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名、身份证号、准考证号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="dialogVisible = true">新增考生</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="rows" border>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" width="100" />
        <el-table-column prop="idCard" label="身份证号" />
        <el-table-column prop="admissionNo" label="准考证号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="status" label="状态" width="120" />
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增考生" width="520px">
      <el-form label-width="92px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别"><el-input v-model="form.gender" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="准考证号"><el-input v-model="form.admissionNo" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createCandidate, pageCandidates } from '../../api/candidate'

const keyword = ref('')
const rows = ref([])
const dialogVisible = ref(false)
const form = reactive({ name: '', gender: '', idCard: '', admissionNo: '', phone: '' })

async function loadData() {
  const result: any = await pageCandidates({ pageNo: 1, pageSize: 10, keyword: keyword.value })
  rows.value = result.data?.records ?? []
}

async function submit() {
  await createCandidate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

onMounted(loadData)
</script>

