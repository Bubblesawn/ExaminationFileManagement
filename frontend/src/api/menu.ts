import { http } from './http'

export type MenuType = 'CATALOG' | 'MENU' | 'BUTTON'
export type MenuStatus = 'ENABLED' | 'DISABLED'

export interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuType: MenuType
  routePath?: string
  componentPath?: string
  permissionCode?: string
  icon?: string
  menuSort: number
  visible: number
  status: MenuStatus
  createTime?: string
  updateTime?: string
  children?: MenuItem[]
}

export interface MenuQueryParams {
  keyword?: string
  status?: MenuStatus | ''
  menuType?: MenuType | ''
}

export interface MenuPayload {
  parentId: number
  menuName: string
  menuType: MenuType
  routePath?: string
  componentPath?: string
  permissionCode?: string
  icon?: string
  menuSort: number
  visible: number
  status: MenuStatus
}

export interface MenuSortPayload {
  parentId: number
  menuSort: number
}

/**
 * @brief 查询系统菜单树，用于菜单管理页面展示层级结构。
 *
 * @param params 菜单查询条件。
 * @return 系统菜单树。
 */
export function treeSystemMenus(params?: MenuQueryParams) {
  return http.get<MenuItem[], MenuItem[]>('/system/menus/tree', { params })
}

/**
 * @brief 创建系统菜单。
 *
 * @param data 新增菜单表单数据。
 * @return 创建后的菜单资料。
 */
export function createSystemMenu(data: MenuPayload) {
  return http.post<MenuItem, MenuItem>('/system/menus', data)
}

/**
 * @brief 修改系统菜单。
 *
 * @param id 菜单主键。
 * @param data 菜单编辑表单数据。
 * @return 修改后的菜单资料。
 */
export function updateSystemMenu(id: number, data: MenuPayload) {
  return http.put<MenuItem, MenuItem>(`/system/menus/${id}`, data)
}

/**
 * @brief 删除单个叶子菜单。
 *
 * @param id 菜单主键。
 */
export function deleteSystemMenu(id: number) {
  return http.delete<void, void>(`/system/menus/${id}`)
}

/**
 * @brief 调整系统菜单父级和排序值。
 *
 * @param id 菜单主键。
 * @param data 排序调整数据。
 * @return 调整后的菜单资料。
 */
export function sortSystemMenu(id: number, data: MenuSortPayload) {
  return http.put<MenuItem, MenuItem>(`/system/menus/${id}/sort`, data)
}
