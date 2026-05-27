<template>
  <div class="page menu-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">菜单管理</h2>
        <p class="page-subtitle">维护系统目录、菜单和按钮权限，按排序值展示导航层级。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog()">新增菜单</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :inline="true" :model="query" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model.trim="query.keyword"
            placeholder="菜单名称、路由或权限标识"
            clearable
            class="query-input"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.menuType" placeholder="全部类型" clearable class="type-select">
            <el-option v-for="item in menuTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
      <el-table
        v-loading="loading"
        :data="menus"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="190" />
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="menuTypeTagMap[row.menuType as MenuType]">{{ formatMenuType(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="routePath" label="路由路径" min-width="150" show-overflow-tooltip />
        <el-table-column prop="componentPath" label="组件路径" min-width="190" show-overflow-tooltip />
        <el-table-column prop="permissionCode" label="权限标识" min-width="180" show-overflow-tooltip />
        <el-table-column prop="menuSort" label="排序" width="86" align="center" />
        <el-table-column prop="visible" label="显示" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Plus" @click="openCreateDialog(row)">新增下级</el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" :icon="Sort" @click="openSortDialog(row)">排序</el-button>
            <el-popconfirm
              title="确认删除该菜单？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="menuDialogVisible" :title="isEditMode ? '编辑菜单' : '新增菜单'" width="680px" destroy-on-close>
      <el-form ref="menuFormRef" :model="menuForm" :rules="menuRules" label-width="96px">
        <el-form-item label="父级菜单" prop="parentId">
          <el-tree-select
            v-model="menuForm.parentId"
            :data="parentMenuOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="请选择父级菜单"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model.trim="menuForm.menuName" maxlength="64" show-word-limit placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="menuForm.menuType">
            <el-radio-button v-for="item in menuTypeOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径" prop="routePath">
          <el-input v-model.trim="menuForm.routePath" maxlength="255" placeholder="/system/menus" />
        </el-form-item>
        <el-form-item label="组件路径" prop="componentPath">
          <el-input v-model.trim="menuForm.componentPath" maxlength="255" placeholder="views/system/MenuManageView.vue" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permissionCode">
          <el-input v-model.trim="menuForm.permissionCode" maxlength="128" placeholder="system:menu:list" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model.trim="menuForm.icon" maxlength="64" placeholder="Menu" />
        </el-form-item>
        <el-form-item label="排序" prop="menuSort">
          <el-input-number v-model="menuForm.menuSort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="显示状态" prop="visible">
          <el-switch v-model="menuForm.visible" :active-value="1" :inactive-value="0" active-text="显示" inactive-text="隐藏" />
        </el-form-item>
        <el-form-item label="菜单状态" prop="status">
          <el-radio-group v-model="menuForm.status">
            <el-radio-button label="ENABLED">启用</el-radio-button>
            <el-radio-button label="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitMenuForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sortDialogVisible" title="调整菜单排序" width="480px" destroy-on-close>
      <el-form :model="sortForm" label-width="96px">
        <el-form-item label="菜单名称">
          <el-input :model-value="selectedMenu?.menuName || ''" disabled />
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-tree-select
            v-model="sortForm.parentId"
            :data="parentMenuOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="sortForm.menuSort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sortDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSortForm">保存排序</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, Sort } from '@element-plus/icons-vue'
import {
  createSystemMenu,
  deleteSystemMenu,
  sortSystemMenu,
  treeSystemMenus,
  updateSystemMenu,
  type MenuItem,
  type MenuPayload,
  type MenuStatus,
  type MenuType
} from '../../api/menu'

const ROOT_MENU_ID = 0

interface MenuForm extends MenuPayload {
  id?: number
}

const menus = ref<MenuItem[]>([])
const loading = ref(false)
const saving = ref(false)
const menuDialogVisible = ref(false)
const sortDialogVisible = ref(false)
const selectedMenu = ref<MenuItem | null>(null)
const menuFormRef = ref<FormInstance>()

const query = reactive({
  keyword: '',
  status: '' as MenuStatus | '',
  menuType: '' as MenuType | ''
})

const menuForm = reactive<MenuForm>({
  parentId: ROOT_MENU_ID,
  menuName: '',
  menuType: 'MENU',
  routePath: '',
  componentPath: '',
  permissionCode: '',
  icon: '',
  menuSort: 0,
  visible: 1,
  status: 'ENABLED'
})

const sortForm = reactive({
  parentId: ROOT_MENU_ID,
  menuSort: 0
})

const menuTypeOptions: Array<{ label: string; value: MenuType }> = [
  { label: '目录', value: 'CATALOG' },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' }
]

const menuTypeTagMap: Record<MenuType, 'primary' | 'success' | 'warning'> = {
  CATALOG: 'primary',
  MENU: 'success',
  BUTTON: 'warning'
}

const isEditMode = computed(() => Boolean(menuForm.id))

const parentMenuOptions = computed(() => [
  {
    id: ROOT_MENU_ID,
    menuName: '根目录',
    children: filterCurrentMenu(menus.value)
  }
])

const menuRules: FormRules<MenuForm> = {
  menuName: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' },
    { max: 64, message: '菜单名称长度不能超过 64 位', trigger: 'blur' }
  ],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  routePath: [{ max: 255, message: '路由路径长度不能超过 255 位', trigger: 'blur' }],
  componentPath: [{ max: 255, message: '组件路径长度不能超过 255 位', trigger: 'blur' }],
  permissionCode: [{ max: 128, message: '权限标识长度不能超过 128 位', trigger: 'blur' }],
  icon: [{ max: 64, message: '图标长度不能超过 64 位', trigger: 'blur' }],
  status: [{ required: true, message: '请选择菜单状态', trigger: 'change' }]
}

/**
 * @brief 加载菜单树，并保持后端返回的层级排序。
 */
async function loadMenus() {
  loading.value = true
  try {
    menus.value = await treeSystemMenus({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      menuType: query.menuType || undefined
    })
  } finally {
    loading.value = false
  }
}

/**
 * @brief 按当前筛选条件查询菜单树。
 */
function handleSearch() {
  loadMenus()
}

/**
 * @brief 清空筛选条件并重新加载菜单树。
 */
function resetSearch() {
  query.keyword = ''
  query.status = ''
  query.menuType = ''
  loadMenus()
}

/**
 * @brief 打开新增菜单表单，可从当前行创建下级菜单。
 *
 * @param parent 当前选中的父级菜单。
 */
function openCreateDialog(parent?: MenuItem) {
  selectedMenu.value = null
  resetMenuForm()
  menuForm.parentId = parent?.id ?? ROOT_MENU_ID
  menuDialogVisible.value = true
}

/**
 * @brief 打开编辑菜单表单并回填行数据。
 *
 * @param menu 当前选中的菜单。
 */
function openEditDialog(menu: MenuItem) {
  selectedMenu.value = menu
  resetMenuForm(menu)
  menuDialogVisible.value = true
}

/**
 * @brief 打开排序调整表单。
 *
 * @param menu 当前选中的菜单。
 */
function openSortDialog(menu: MenuItem) {
  selectedMenu.value = menu
  sortForm.parentId = menu.parentId ?? ROOT_MENU_ID
  sortForm.menuSort = menu.menuSort ?? 0
  sortDialogVisible.value = true
}

/**
 * @brief 保存新增或编辑后的菜单资料。
 */
async function submitMenuForm() {
  await menuFormRef.value?.validate()
  saving.value = true
  try {
    const payload = normalizeMenuPayload(menuForm)
    if (isEditMode.value && menuForm.id) {
      await updateSystemMenu(menuForm.id, payload)
      ElMessage.success('菜单信息已更新')
    } else {
      await createSystemMenu(payload)
      ElMessage.success('菜单已新增')
    }
    menuDialogVisible.value = false
    await loadMenus()
  } finally {
    saving.value = false
  }
}

/**
 * @brief 保存菜单排序和父级调整。
 */
async function submitSortForm() {
  if (!selectedMenu.value) {
    return
  }
  saving.value = true
  try {
    await sortSystemMenu(selectedMenu.value.id, {
      parentId: sortForm.parentId ?? ROOT_MENU_ID,
      menuSort: sortForm.menuSort ?? 0
    })
    ElMessage.success('菜单排序已更新')
    sortDialogVisible.value = false
    await loadMenus()
  } finally {
    saving.value = false
  }
}

/**
 * @brief 删除单个菜单，后端会阻止删除存在子菜单的节点。
 *
 * @param menu 当前选中的菜单。
 */
async function handleDelete(menu: MenuItem) {
  await deleteSystemMenu(menu.id)
  ElMessage.success('菜单已删除')
  await loadMenus()
}

/**
 * @brief 初始化菜单表单。
 *
 * @param menu 当前编辑的菜单。
 */
function resetMenuForm(menu?: MenuItem) {
  menuForm.id = menu?.id
  menuForm.parentId = menu?.parentId ?? ROOT_MENU_ID
  menuForm.menuName = menu?.menuName ?? ''
  menuForm.menuType = menu?.menuType ?? 'MENU'
  menuForm.routePath = menu?.routePath ?? ''
  menuForm.componentPath = menu?.componentPath ?? ''
  menuForm.permissionCode = menu?.permissionCode ?? ''
  menuForm.icon = menu?.icon ?? ''
  menuForm.menuSort = menu?.menuSort ?? 0
  menuForm.visible = menu?.visible ?? 1
  menuForm.status = menu?.status ?? 'ENABLED'
  menuFormRef.value?.clearValidate()
}

/**
 * @brief 过滤当前菜单及其子孙节点，避免父级选择产生循环结构。
 *
 * @param items 菜单树节点。
 * @return 可作为父级候选的菜单树。
 */
function filterCurrentMenu(items: MenuItem[]): MenuItem[] {
  return items
    .filter((item) => item.id !== selectedMenu.value?.id)
    .map((item) => ({
      ...item,
      children: filterCurrentMenu(item.children ?? [])
    }))
}

/**
 * @brief 清理菜单表单空字符串，生成后端 DTO 可接收的请求体。
 *
 * @param form 菜单表单。
 * @return 菜单保存请求体。
 */
function normalizeMenuPayload(form: MenuForm): MenuPayload {
  return {
    parentId: form.parentId ?? ROOT_MENU_ID,
    menuName: form.menuName,
    menuType: form.menuType,
    routePath: form.routePath || undefined,
    componentPath: form.componentPath || undefined,
    permissionCode: form.permissionCode || undefined,
    icon: form.icon || undefined,
    menuSort: form.menuSort ?? 0,
    visible: form.visible ?? 1,
    status: form.status
  }
}

/**
 * @brief 将菜单类型编码转换为中文展示文本。
 *
 * @param type 菜单类型编码。
 * @return 菜单类型中文名称。
 */
function formatMenuType(type: MenuType) {
  return menuTypeOptions.find((item) => item.value === type)?.label ?? type
}

onMounted(loadMenus)
</script>

<style scoped>
.menu-page {
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

.type-select,
.status-select {
  width: 150px;
}
</style>
