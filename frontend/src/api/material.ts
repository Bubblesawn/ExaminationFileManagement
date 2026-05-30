import { http } from './http'

export interface MaterialType {
  id: number
  typeCode: string
  typeName: string
  description?: string
  sortOrder?: number
  status: string
  createTime?: string
  updateTime?: string
}

export interface RecordMaterial {
  id: number
  recordId: number
  materialType: string
  fileName: string
  originalFileName: string
  fileUrl: string
  fileSize: number
  fileSuffix: string
  mimeType?: string
  previewUrl: string
  uploadUserId?: number
  auditStatus: string
  auditOpinion?: string
  auditUserId?: number
  auditTime?: string
  createTime?: string
  updateTime?: string
}

export interface BusinessMaterialBundle {
  id: number
  applicationNo: string
  businessType: string
  recordId: number
  candidateId?: number
  applicationTitle?: string
  applicationStatus?: string
  currentNodeName?: string
  applyUserName?: string
  submitTime?: string
  materialIds: number[]
  materials: RecordMaterial[]
}

/**
 * @brief 查询启用材料类型列表。
 *
 * @return 启用状态的材料类型。
 */
export function listEnabledMaterialTypes() {
  return http.get('/material-types', { params: { status: 'ENABLED' } }) as unknown as Promise<MaterialType[]>
}

/**
 * @brief 查询档案材料列表。
 *
 * @param params 材料查询条件。
 * @return 档案材料列表。
 */
export function listRecordMaterials(params: { recordId?: number; materialType?: string }) {
  return http.get('/materials', { params }) as unknown as Promise<RecordMaterial[]>
}

/**
 * @brief 上传单个档案材料。
 *
 * @param recordId 考籍档案ID。
 * @param materialType 材料类型编码。
 * @param file 待上传材料文件。
 * @return 上传后的材料记录。
 */
export function uploadRecordMaterial(recordId: number, materialType: string, file: File) {
  const formData = new FormData()
  formData.append('recordId', String(recordId))
  formData.append('materialType', materialType)
  formData.append('file', file)
  return http.post('/materials/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as unknown as Promise<RecordMaterial>
}

/**
 * @brief 按业务编号查询业务申请和材料列表。
 *
 * @param businessNo 业务申请编号或业务申请ID。
 * @return 业务申请材料包。
 */
export function getBusinessMaterials(businessNo: string) {
  return http.get(`/materials/business/${encodeURIComponent(businessNo)}`) as unknown as Promise<BusinessMaterialBundle>
}

/**
 * @brief 按业务编号上传材料并自动绑定到业务申请。
 *
 * @param businessNo 业务申请编号或业务申请ID。
 * @param materialType 材料类型编码。
 * @param file 待上传材料文件。
 * @return 上传后同步完成的业务申请材料包。
 */
export function uploadBusinessMaterial(businessNo: string, materialType: string, file: File) {
  const formData = new FormData()
  formData.append('materialType', materialType)
  formData.append('file', file)
  return http.post(`/materials/business/${encodeURIComponent(businessNo)}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as unknown as Promise<BusinessMaterialBundle>
}

/**
 * @brief 删除单条档案材料。
 *
 * @param id 材料ID。
 */
export function deleteRecordMaterial(id: number) {
  return http.delete(`/materials/${id}`) as unknown as Promise<void>
}

/**
 * @brief 获取材料预览文件 Blob。
 *
 * @param id 材料ID。
 * @return 可用于本地对象地址的文件 Blob。
 */
export function previewRecordMaterial(id: number) {
  return http.get(`/materials/${id}/preview`, { responseType: 'blob' }) as unknown as Promise<Blob>
}

/**
 * @brief 获取材料下载文件 Blob。
 *
 * @param id 材料ID。
 * @return 下载文件 Blob。
 */
export function downloadRecordMaterial(id: number) {
  return http.get(`/materials/${id}/download`, { responseType: 'blob' }) as unknown as Promise<Blob>
}
