<template>
  <div class="page system-role-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">角色管理</h2>
        <p class="page-subtitle">维护系统角色信息，并为角色分配菜单访问权限。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增角色</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="角色名称、角色编码" clearable @keyup.enter="loadRoles" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable>
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRoles">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="roles" border>
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleCode" label="角色编码" min-width="150" />
        <el-table-column prop="roleSort" label="排序" width="90" align="center" />
        <el-table-column prop="dataScope" label="数据范围" width="120">
          <template #default="{ row }">
            <el-tag :type="row.dataScope === 'ALL' ? 'success' : 'info'">
              {{ dataScopeText(row.dataScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" :icon="Key" @click="openPermissionDialog(row)">分配权限</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeRole(row)">删除</el-button>
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
          @size-change="loadRoles"
          @current-change="loadRoles"
        />
      </div>
    </el-card>

    <el-dialog v-model="roleDialogVisible" :title="roleDialogTitle" width="560px" destroy-on-close>
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="96px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" :disabled="Boolean(editingRoleId)" maxlength="64" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" maxlength="64" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="显示排序" prop="roleSort">
          <el-input-number v-model="roleForm.roleSort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-radio-group v-model="roleForm.dataScope">
            <el-radio-button label="ALL">全部数据</el-radio-button>
            <el-radio-button label="SELF">本人数据</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
            <el-radio-button label="ENABLED">启用</el-radio-button>
            <el-radio-button label="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" maxlength="512" show-word-limit :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" title="分配菜单权限" width="520px" destroy-on-close>
      <div class="permission-title">
        当前角色：<strong>{{ permissionRole?.roleName }}</strong>
      </div>
      <el-tree
        ref="menuTreeRef"
        v-loading="menuLoading"
        :data="menuTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="menuTreeProps"
        class="permission-tree"
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSubmitting" @click="submitPermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getRoleDetail,
  pageRoles,
  treeMenus,
  updateRole,
  type MenuRecord,
  type RoleRecord
} from '../../api/system'

const loading = ref(false)
const submitting = ref(false)
const menuLoading = ref(false)
const permissionSubmitting = ref(false)
const roleDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const roles = ref<RoleRecord[]>([])
const total = ref(0)
const editingRoleId = ref<number | null>(null)
const permissionRole = ref<RoleRecord | null>(null)
const menuTree = ref<MenuRecord[]>([])
const roleFormRef = ref<FormInstance>()
const menuTreeRef = ref<TreeInstance>()

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

const roleForm = reactive({
  roleCode: '',
  roleName: '',
  roleSort: 0,
  dataScope: 'ALL',
  status: 'ENABLED',
  remark: ''
})

const roleRules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const menuTreeProps = {
  label: 'menuName',
  children: 'children'
}

const roleDialogTitle = computed(() => (editingRoleId.value ? '编辑角色' : '新增角色'))

function statusText(status: string) {
  return status === 'ENABLED' ? '启用' : '禁用'
}

function dataScopeText(dataScope: string) {
  return dataScope === 'ALL' ? '全部数据' : '本人数据'
}

function resetRoleForm() {
  editingRoleId.value = null
  Object.assign(roleForm, {
    roleCode: '',
    roleName: '',
    roleSort: 0,
    dataScope: 'ALL',
    status: 'ENABLED',
    remark: ''
  })
  roleFormRef.value?.clearValidate()
}

async function loadRoles() {
  loading.value = true
  try {
    const result = await pageRoles({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    })
    roles.value = result.records ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.pageNo = 1
  query.keyword = ''
  query.status = ''
  loadRoles()
}

function openCreateDialog() {
  resetRoleForm()
  roleDialogVisible.value = true
}

async function openEditDialog(row: RoleRecord) {
  resetRoleForm()
  editingRoleId.value = row.id
  Object.assign(roleForm, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    roleSort: row.roleSort ?? 0,
    dataScope: row.dataScope || 'ALL',
    status: row.status || 'ENABLED',
    remark: row.remark || ''
  })
  roleDialogVisible.value = true
}

async function submitRole() {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    if (editingRoleId.value) {
      await updateRole(editingRoleId.value, {
        roleName: roleForm.roleName,
        roleSort: roleForm.roleSort,
        dataScope: roleForm.dataScope,
        status: roleForm.status,
        remark: roleForm.remark
      })
    } else {
      await createRole({
        roleCode: roleForm.roleCode,
        roleName: roleForm.roleName,
        roleSort: roleForm.roleSort,
        dataScope: roleForm.dataScope,
        status: roleForm.status,
        remark: roleForm.remark
      })
    }
    ElMessage.success('保存成功')
    roleDialogVisible.value = false
    await loadRoles()
  } finally {
    submitting.value = false
  }
}

async function removeRole(row: RoleRecord) {
  try {
    await ElMessageBox.confirm(`确认删除角色“${row.roleName}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  await deleteRole(row.id)
  ElMessage.success('删除成功')
  await loadRoles()
}

async function loadMenuTree() {
  menuLoading.value = true
  try {
    menuTree.value = await treeMenus({ status: 'ENABLED' })
  } finally {
    menuLoading.value = false
  }
}

async function openPermissionDialog(row: RoleRecord) {
  permissionRole.value = await getRoleDetail(row.id)
  permissionDialogVisible.value = true
  await loadMenuTree()
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(permissionRole.value.menuIds ?? [])
}

async function submitPermissions() {
  if (!permissionRole.value) {
    return
  }

  permissionSubmitting.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) ?? []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() ?? []
    const menuIds = [...checkedKeys, ...halfCheckedKeys].map((key) => Number(key))
    await assignRoleMenus(permissionRole.value.id, menuIds)
    ElMessage.success('权限保存成功')
    permissionDialogVisible.value = false
    await loadRoles()
  } finally {
    permissionSubmitting.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.system-role-page {
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.permission-title {
  margin-bottom: 12px;
  color: #334155;
}

.permission-tree {
  max-height: 420px;
  overflow: auto;
  padding: 8px 0;
}
</style>
