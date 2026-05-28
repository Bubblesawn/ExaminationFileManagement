import { http } from './http'

export interface ExemptionApplication {
  id: number
  applicationNo: string
  businessType: string
  recordId: number
  recordNo?: string
  candidateId?: number
  candidateName?: string
  idCard?: string
  admissionNo?: string
  applicationTitle?: string
  applicationStatus: string
  currentNodeCode?: string
  currentNodeName?: string
  courseCode?: string
  courseName?: string
  sourceCourseCode?: string
  sourceCourseName?: string
  exemptionReason?: string
  materialIds?: number[]
  applyUserName?: string
  submitTime?: string
  withdrawTime?: string
  withdrawReason?: string
  auditUserName?: string
  auditTime?: string
  auditOpinion?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ExemptionApplicationPage {
  records: ExemptionApplication[]
  total: number
  size: number
  current: number
}

export interface ExemptionApplicationPayload {
  recordId?: number
  courseCode: string
  courseName: string
  sourceCourseCode?: string
  sourceCourseName?: string
  exemptionReason: string
  materialIds?: number[]
  remark?: string
}

export interface ExemptionAuditPayload {
  auditOpinion: string
}

export interface ExemptionWithdrawPayload {
  withdrawReason: string
}

export interface ExemptionFlowRecord {
  id: number
  applicationId: number
  businessType: string
  businessId: number
  recordId?: number
  auditAction: string
  beforeStatus?: string
  afterStatus: string
  auditStatus: string
  auditOpinion?: string
  auditorName?: string
  operationTime?: string
}

/**
 * @brief 分页查询免考申请。
 *
 * @param params 查询条件。
 * @return 免考申请分页数据。
 */
export function pageExemptionApplications(params: {
  pageNo: number
  pageSize: number
  keyword?: string
  applicationStatus?: string
  recordId?: number
  candidateId?: number
}) {
  return http.get('/exemptions/page', { params }) as unknown as Promise<ExemptionApplicationPage>
}

/**
 * @brief 查询免考申请详情。
 *
 * @param id 免考申请ID。
 * @return 免考申请详情。
 */
export function getExemptionApplicationDetail(id: number) {
  return http.get(`/exemptions/${id}`) as unknown as Promise<ExemptionApplication>
}

/**
 * @brief 提交免考申请。
 *
 * @param data 免考申请提交数据。
 * @return 已提交的免考申请。
 */
export function submitExemptionApplication(data: ExemptionApplicationPayload) {
  return http.post('/exemptions', data) as unknown as Promise<ExemptionApplication>
}

/**
 * @brief 修改免考申请。
 *
 * @param id 免考申请ID。
 * @param data 免考申请修改数据。
 * @return 修改后的免考申请。
 */
export function updateExemptionApplication(id: number, data: ExemptionApplicationPayload) {
  return http.put(`/exemptions/${id}`, data) as unknown as Promise<ExemptionApplication>
}

/**
 * @brief 撤回免考申请。
 *
 * @param id 免考申请ID。
 * @param data 撤回原因。
 * @return 撤回后的免考申请。
 */
export function withdrawExemptionApplication(id: number, data: ExemptionWithdrawPayload) {
  return http.put(`/exemptions/${id}/withdraw`, data) as unknown as Promise<ExemptionApplication>
}

/**
 * @brief 审核通过免考申请。
 *
 * @param id 免考申请ID。
 * @param data 审核意见。
 * @return 审核通过后的免考申请。
 */
export function approveExemptionApplication(id: number, data: ExemptionAuditPayload) {
  return http.put(`/exemptions/${id}/approve`, data) as unknown as Promise<ExemptionApplication>
}

/**
 * @brief 驳回免考申请。
 *
 * @param id 免考申请ID。
 * @param data 审核意见。
 * @return 驳回后的免考申请。
 */
export function rejectExemptionApplication(id: number, data: ExemptionAuditPayload) {
  return http.put(`/exemptions/${id}/reject`, data) as unknown as Promise<ExemptionApplication>
}

/**
 * @brief 查询免考申请流程记录。
 *
 * @param id 免考申请ID。
 * @return 流程记录列表。
 */
export function listExemptionFlowRecords(id: number) {
  return http.get(`/exemptions/${id}/flow-records`) as unknown as Promise<ExemptionFlowRecord[]>
}
