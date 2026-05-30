<template>
  <div class="page system-user-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">创建后台账号，并为账号分配系统角色。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="账号、姓名、手机号或邮箱" clearable @keyup.enter="loadUsers" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable>
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="users" border>
        <el-table-column prop="username" label="登录账号" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="roleName in row.roleNames" :key="roleName" class="role-tag" type="info">
              {{ roleName }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" :icon="Key" @click="openPasswordDialog(row)">重置密码</el-button>
            <el-button
              link
              :type="row.status === 'ENABLED' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="removeUser(row)">删除</el-button>
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="620px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="96px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="userForm.username" :disabled="Boolean(editingUserId)" maxlength="64" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item v-if="!editingUserId" label="初始密码" prop="password">
          <el-input v-model="userForm.password" type="password" maxlength="64" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" maxlength="64" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" maxlength="32" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" maxlength="128" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" class="full-field">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
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
        <el-button type="primary" :loading="submitting" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" maxlength="64" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { pageRoles, type RoleRecord } from '../../api/system'
import {
  createSystemUser,
  deleteSystemUser,
  disableSystemUser,
  enableSystemUser,
  pageSystemUsers,
  resetSystemUserPassword,
  updateSystemUser,
  type SystemUser,
  type UserStatus
} from '../../api/systemUser'

const loading = ref(false)
const submitting = ref(false)
const passwordSubmitting = ref(false)
const userDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const users = ref<SystemUser[]>([])
const roleOptions = ref<RoleRecord[]>([])
const total = ref(0)
const editingUserId = ref<number | null>(null)
const passwordUserId = ref<number | null>(null)
const userFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: '' as UserStatus | ''
})

const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  status: 'ENABLED' as UserStatus,
  roleIds: [] as number[]
})

const passwordForm = reactive({
  password: ''
})

const userRules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 64, message: '密码长度必须在6到64位之间', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const passwordRules: FormRules = {
  password: [{ required: true, min: 6, max: 64, message: '密码长度必须在6到64位之间', trigger: 'blur' }]
}

const userDialogTitle = computed(() => (editingUserId.value ? '编辑用户' : '新增用户'))

function statusText(status: string) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function resetUserForm() {
  editingUserId.value = null
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    status: 'ENABLED',
    roleIds: []
  })
  userFormRef.value?.clearValidate()
}

/**
 * @brief 加载启用角色选项，供管理员创建或编辑用户时分配角色。
 */
async function loadRoleOptions() {
  const result = await pageRoles({ pageNo: 1, pageSize: 100, status: 'ENABLED' })
  roleOptions.value = result.records ?? []
}

/**
 * @brief 分页加载系统用户，并展示用户当前绑定的角色名称。
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

function resetQuery() {
  query.pageNo = 1
  query.keyword = ''
  query.status = ''
  loadUsers()
}

async function openCreateDialog() {
  resetUserForm()
  await loadRoleOptions()
  userDialogVisible.value = true
}

async function openEditDialog(row: SystemUser) {
  resetUserForm()
  await loadRoleOptions()
  editingUserId.value = row.id
  Object.assign(userForm, {
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    status: row.status,
    roleIds: [...(row.roleIds ?? [])]
  })
  userDialogVisible.value = true
}

async function submitUser() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    if (editingUserId.value) {
      await updateSystemUser(editingUserId.value, {
        realName: userForm.realName,
        phone: userForm.phone,
        email: userForm.email,
        status: userForm.status,
        roleIds: userForm.roleIds
      })
    } else {
      await createSystemUser({
        username: userForm.username,
        password: userForm.password,
        realName: userForm.realName,
        phone: userForm.phone,
        email: userForm.email,
        status: userForm.status,
        roleIds: userForm.roleIds
      })
    }
    ElMessage.success('保存成功')
    userDialogVisible.value = false
    await loadUsers()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: SystemUser) {
  if (row.status === 'ENABLED') {
    await disableSystemUser(row.id)
    ElMessage.success('已禁用')
  } else {
    await enableSystemUser(row.id)
    ElMessage.success('已启用')
  }
  await loadUsers()
}

async function removeUser(row: SystemUser) {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.realName}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  await deleteSystemUser(row.id)
  ElMessage.success('删除成功')
  await loadUsers()
}

function openPasswordDialog(row: SystemUser) {
  passwordUserId.value = row.id
  passwordForm.password = ''
  passwordFormRef.value?.clearValidate()
  passwordDialogVisible.value = true
}

async function submitPassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid || !passwordUserId.value) {
    return
  }

  passwordSubmitting.value = true
  try {
    await resetSystemUserPassword(passwordUserId.value, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } finally {
    passwordSubmitting.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.system-user-page {
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

.role-tag {
  margin-right: 6px;
}

.muted {
  color: #94a3b8;
}

.full-field {
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
