<template>
  <div class="page system-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">维护系统登录账号、状态和基础联系方式。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="账号、姓名、手机号"
            clearable
            class="query-input"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="users" border>
        <el-table-column prop="username" label="账号" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'ENABLED' ? 'warning' : 'success'"
              :icon="SwitchButton"
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" :icon="Key" @click="openResetDialog(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="userDialogVisible" :title="isEditMode ? '编辑用户' : '新增用户'" width="560px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="92px">
        <el-form-item v-if="!isEditMode" label="账号" prop="username">
          <el-input v-model.trim="userForm.username" maxlength="64" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item v-if="!isEditMode" label="初始密码" prop="password">
          <el-input v-model="userForm.password" type="password" maxlength="64" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model.trim="userForm.realName" maxlength="64" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="userForm.phone" maxlength="32" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="userForm.email" maxlength="128" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="头像地址" prop="avatar">
          <el-input v-model.trim="userForm.avatar" maxlength="512" placeholder="可选，填写头像 URL" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio-button label="ENABLED">启用</el-radio-button>
            <el-radio-button label="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitUserForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialogVisible" title="重置密码" width="460px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="92px">
        <el-form-item label="用户">
          <el-input :model-value="selectedUser?.realName || selectedUser?.username || ''" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" maxlength="64" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Edit, Key, Plus, Refresh, Search, SwitchButton } from '@element-plus/icons-vue'
import {
  createSystemUser,
  disableSystemUser,
  enableSystemUser,
  pageSystemUsers,
  resetSystemUserPassword,
  updateSystemUser,
  type SystemUser,
  type UserStatus
} from '../../api/systemUser'

interface UserForm {
  id?: number
  username: string
  password: string
  realName: string
  phone: string
  email: string
  avatar: string
  status: UserStatus
}

const users = ref<SystemUser[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const userDialogVisible = ref(false)
const resetDialogVisible = ref(false)
const selectedUser = ref<SystemUser | null>(null)
const userFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: '' as UserStatus | ''
})

const userForm = reactive<UserForm>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  status: 'ENABLED'
})

const resetForm = reactive({
  password: ''
})

const isEditMode = computed(() => Boolean(userForm.id))

const userRules: FormRules<UserForm> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 64, message: '账号长度不能超过 64 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度必须在 6 到 64 位之间', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 64, message: '姓名长度不能超过 64 位', trigger: 'blur' }
  ],
  phone: [{ max: 32, message: '手机号长度不能超过 32 位', trigger: 'blur' }],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 128, message: '邮箱长度不能超过 128 位', trigger: 'blur' }
  ],
  avatar: [{ max: 512, message: '头像地址长度不能超过 512 位', trigger: 'blur' }],
  status: [{ required: true, message: '请选择用户状态', trigger: 'change' }]
}

const resetRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度必须在 6 到 64 位之间', trigger: 'blur' }
  ]
}

/**
 * @brief 加载用户分页列表，并同步分页总数。
 */
async function loadUsers() {
  loading.value = true
  try {
    const result = await pageSystemUsers({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
    users.value = result.records ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

/**
 * @brief 按当前筛选条件从第一页重新查询用户。
 */
function handleSearch() {
  query.pageNo = 1
  loadUsers()
}

/**
 * @brief 清空筛选条件并重新加载用户列表。
 */
function resetSearch() {
  query.keyword = ''
  query.status = ''
  query.pageNo = 1
  loadUsers()
}

/**
 * @brief 打开新增用户表单并初始化默认值。
 */
function openCreateDialog() {
  resetUserForm()
  userDialogVisible.value = true
}

/**
 * @brief 打开编辑用户表单并回填当前行数据。
 *
 * @param user 当前选中的系统用户。
 */
function openEditDialog(user: SystemUser) {
  userForm.id = user.id
  userForm.username = user.username
  userForm.password = ''
  userForm.realName = user.realName
  userForm.phone = user.phone || ''
  userForm.email = user.email || ''
  userForm.avatar = user.avatar || ''
  userForm.status = user.status
  userDialogVisible.value = true
}

/**
 * @brief 保存新增或编辑后的用户资料。
 */
async function submitUserForm() {
  await userFormRef.value?.validate()
  saving.value = true
  try {
    if (isEditMode.value && userForm.id) {
      await updateSystemUser(userForm.id, {
        realName: userForm.realName,
        phone: userForm.phone || undefined,
        email: userForm.email || undefined,
        avatar: userForm.avatar || undefined,
        status: userForm.status
      })
      ElMessage.success('用户信息已更新')
    } else {
      await createSystemUser({
        username: userForm.username,
        password: userForm.password,
        realName: userForm.realName,
        phone: userForm.phone || undefined,
        email: userForm.email || undefined,
        avatar: userForm.avatar || undefined,
        status: userForm.status
      })
      ElMessage.success('用户已新增')
    }
    userDialogVisible.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

/**
 * @brief 根据当前状态启用或禁用用户。
 *
 * @param user 当前选中的系统用户。
 */
async function toggleUserStatus(user: SystemUser) {
  const nextAction = user.status === 'ENABLED' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${nextAction}用户“${user.realName || user.username}”？`, '状态确认', {
    confirmButtonText: `确认${nextAction}`,
    cancelButtonText: '取消',
    type: 'warning'
  })
  if (user.status === 'ENABLED') {
    await disableSystemUser(user.id)
  } else {
    await enableSystemUser(user.id)
  }
  ElMessage.success(`用户已${nextAction}`)
  await loadUsers()
}

/**
 * @brief 打开重置密码表单。
 *
 * @param user 当前选中的系统用户。
 */
function openResetDialog(user: SystemUser) {
  selectedUser.value = user
  resetForm.password = ''
  resetDialogVisible.value = true
}

/**
 * @brief 提交重置密码请求。
 */
async function submitResetPassword() {
  if (!selectedUser.value) {
    return
  }
  await resetFormRef.value?.validate()
  resetting.value = true
  try {
    await resetSystemUserPassword(selectedUser.value.id, resetForm.password)
    ElMessage.success('密码已重置')
    resetDialogVisible.value = false
  } finally {
    resetting.value = false
  }
}

/**
 * @brief 将用户表单恢复为空白新增状态。
 */
function resetUserForm() {
  userForm.id = undefined
  userForm.username = ''
  userForm.password = ''
  userForm.realName = ''
  userForm.phone = ''
  userForm.email = ''
  userForm.avatar = ''
  userForm.status = 'ENABLED'
  userFormRef.value?.clearValidate()
}

/**
 * @brief 格式化后端返回的日期时间文本。
 *
 * @param value 后端日期时间字符串。
 * @return 页面展示文本。
 */
function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(loadUsers)
</script>

<style scoped>
.system-page {
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

.query-input {
  width: 260px;
}

.status-select {
  width: 160px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>