import { http } from './http'

export interface GraduationEligibility {
  eligible: boolean
  passedItems: string[]
  failedItems: string[]
  warningItems: string[]
}

export interface GraduationApplication {
  id: number
  applicationNo: string
  businessType: string
  recordId: number
  recordNo?: string
  recordStatus?: string
  archiveStatus?: string
  educationLevel?: string
  majorCode?: string
  majorName?: string
  candidateId: number
  candidateName?: string
  idCard?: string
  admissionNo?: string
  phone?: string
  applicationTitle?: string
  applicationStatus: string
  graduationBatch?: string
  degreeApplyType?: string
  applyReason?: string
  eligibilityPassed?: boolean
  eligibilitySummary?: string
  materialIds?: number[]
  applyUserName?: string
  submitTime?: string
  withdrawTime?: string
  withdrawReason?: string
  auditUserName?: string
  auditTime?: string
  auditOpinion?: string
  remark?: string
}

export interface GraduationApplicationPage {
  records: GraduationApplication[]
  total: number
  size: number
  current: number
}

export interface GraduationApplicationPayload {
  recordId?: number
  graduationBatch: string
  degreeApplyType?: string
  applyReason: string
  materialIds?: number[]
  remark?: string
}

export interface GraduationAuditPayload {
  auditOpinion: string
}

export interface GraduationWithdrawPayload {
  withdrawReason: string
}

export interface GraduationFlowRecord {
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
 * @brief 分页查询毕业申请。
 *
 * @param params 查询条件。
 * @return 毕业申请分页数据。
 */
export function pageGraduationApplications(params: {
  pageNo: number
  pageSize: number
  keyword?: string
  applicationStatus?: string
  recordId?: number
  candidateId?: number
}) {
  return http.get('/graduations/page', { params }) as unknown as Promise<GraduationApplicationPage>
}

/**
 * @brief 查询毕业申请详情。
 *
 * @param id 毕业申请ID。
 * @return 毕业申请详情。
 */
export function getGraduationApplicationDetail(id: number) {
  return http.get(`/graduations/${id}`) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 校验考籍档案毕业资格。
 *
 * @param recordId 考籍档案ID。
 * @return 毕业资格校验结果。
 */
export function checkGraduationEligibility(recordId: number) {
  return http.get(`/graduations/eligibility/${recordId}`) as unknown as Promise<GraduationEligibility>
}

/**
 * @brief 提交毕业申请。
 *
 * @param data 毕业申请提交数据。
 * @return 已提交的毕业申请。
 */
export function submitGraduationApplication(data: GraduationApplicationPayload) {
  return http.post('/graduations', data) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 修改毕业申请。
 *
 * @param id 毕业申请ID。
 * @param data 毕业申请修改数据。
 * @return 修改后的毕业申请。
 */
export function updateGraduationApplication(id: number, data: GraduationApplicationPayload) {
  return http.put(`/graduations/${id}`, data) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 撤回毕业申请。
 *
 * @param id 毕业申请ID。
 * @param data 撤回原因。
 * @return 撤回后的毕业申请。
 */
export function withdrawGraduationApplication(id: number, data: GraduationWithdrawPayload) {
  return http.put(`/graduations/${id}/withdraw`, data) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 审核通过毕业申请。
 *
 * @param id 毕业申请ID。
 * @param data 审核意见。
 * @return 审核通过后的毕业申请。
 */
export function approveGraduationApplication(id: number, data: GraduationAuditPayload) {
  return http.put(`/graduations/${id}/approve`, data) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 驳回毕业申请。
 *
 * @param id 毕业申请ID。
 * @param data 审核意见。
 * @return 驳回后的毕业申请。
 */
export function rejectGraduationApplication(id: number, data: GraduationAuditPayload) {
  return http.put(`/graduations/${id}/reject`, data) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 查询毕业申请结果。
 *
 * @param id 毕业申请ID。
 * @return 毕业申请结果。
 */
export function getGraduationApplicationResult(id: number) {
  return http.get(`/graduations/${id}/result`) as unknown as Promise<GraduationApplication>
}

/**
 * @brief 查询毕业申请流程记录。
 *
 * @param id 毕业申请ID。
 * @return 流程记录列表。
 */
export function listGraduationFlowRecords(id: number) {
  return http.get(`/graduations/${id}/flow-records`) as unknown as Promise<GraduationFlowRecord[]>
}
