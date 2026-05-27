import { http } from './http'

export interface RolePageParams {
  pageNo: number
  pageSize: number
  keyword?: string
  status?: string
}

export interface RolePageResult {
  records?: RoleRecord[]
  total?: number
}

export interface RolePayload {
  roleCode?: string
  roleName: string
  roleSort?: number
  dataScope: string
  status: string
  remark?: string
}

export interface RoleRecord {
  id: number
  roleCode: string
  roleName: string
  roleSort: number
  dataScope: string
  status: string
  remark?: string
  menuIds?: number[]
  createTime?: string
  updateTime?: string
}

export interface MenuRecord {
  id: number
  parentId: number
  menuName: string
  menuType: string
  routePath?: string
  componentPath?: string
  permissionCode?: string
  icon?: string
  menuSort: number
  visible: number
  status: string
  children?: MenuRecord[]
}

/**
 * @brief 分页查询系统角色。
 *
 * @param params 角色分页查询条件。
 * @return 系统角色分页结果。
 */
export function pageRoles(params: RolePageParams) {
  return http.get('/system/roles/page', { params }) as unknown as Promise<RolePageResult>
}

/**
 * @brief 查询系统角色详情。
 *
 * @param id 角色ID。
 * @return 系统角色详情。
 */
export function getRoleDetail(id: number) {
  return http.get(`/system/roles/${id}`) as unknown as Promise<RoleRecord>
}

/**
 * @brief 新增系统角色。
 *
 * @param data 角色表单数据。
 * @return 新增后的系统角色。
 */
export function createRole(data: RolePayload) {
  return http.post('/system/roles', data) as unknown as Promise<RoleRecord>
}

/**
 * @brief 修改系统角色。
 *
 * @param id 角色ID。
 * @param data 角色表单数据。
 * @return 修改后的系统角色。
 */
export function updateRole(id: number, data: RolePayload) {
  return http.put(`/system/roles/${id}`, data) as unknown as Promise<RoleRecord>
}

/**
 * @brief 删除系统角色。
 *
 * @param id 角色ID。
 */
export function deleteRole(id: number) {
  return http.delete(`/system/roles/${id}`) as unknown as Promise<void>
}

/**
 * @brief 为角色分配菜单权限。
 *
 * @param id 角色ID。
 * @param menuIds 菜单ID列表。
 * @return 分配后的系统角色详情。
 */
export function assignRoleMenus(id: number, menuIds: number[]) {
  return http.put(`/system/roles/${id}/menus`, { menuIds }) as unknown as Promise<RoleRecord>
}

/**
 * @brief 查询系统菜单树。
 *
 * @param params 菜单查询条件。
 * @return 系统菜单树。
 */
export function treeMenus(params?: { status?: string }) {
  return http.get('/system/menus/tree', { params }) as unknown as Promise<MenuRecord[]>
}
