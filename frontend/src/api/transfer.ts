import { http } from './http'

export interface TransferApplication {
  id: number
  applicationNo: string
  businessType: string
  recordId: number
  recordNo?: string
  recordStatus?: string
  candidateId?: number
  candidateName?: string
  idCard?: string
  admissionNo?: string
  applicationTitle?: string
  applicationStatus: string
  sourceProvince?: string
  sourceSchool?: string
  sourceRecordNo?: string
  targetProvince?: string
  targetSchool?: string
  transferReason?: string
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

export interface TransferApplicationPage {
  records: TransferApplication[]
  total: number
  size: number
  current: number
}

export interface TransferApplicationPayload {
  recordId?: number
  transferType?: string
  sourceProvince?: string
  sourceSchool?: string
  sourceRecordNo?: string
  targetProvince?: string
  targetSchool?: string
  transferReason: string
  materialIds?: number[]
  remark?: string
}

export interface TransferAuditPayload {
  auditOpinion: string
}

export interface TransferWithdrawPayload {
  withdrawReason: string
}

export interface TransferFlowRecord {
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
 * @brief 分页查询考籍转入转出申请。
 *
 * @param params 查询条件。
 * @return 转入转出申请分页数据。
 */
export function pageTransferApplications(params: {
  pageNo: number
  pageSize: number
  keyword?: string
  transferType?: string
  applicationStatus?: string
  recordId?: number
  candidateId?: number
}) {
  return http.get('/transfers/page', { params }) as unknown as Promise<TransferApplicationPage>
}

/**
 * @brief 查询考籍转入转出申请详情。
 *
 * @param id 转考申请ID。
 * @return 转考申请详情。
 */
export function getTransferApplicationDetail(id: number) {
  return http.get(`/transfers/${id}`) as unknown as Promise<TransferApplication>
}

/**
 * @brief 提交考籍转入转出申请。
 *
 * @param data 转考申请提交数据。
 * @return 已提交的转考申请。
 */
export function submitTransferApplication(data: TransferApplicationPayload) {
  return http.post('/transfers', data) as unknown as Promise<TransferApplication>
}

/**
 * @brief 修改考籍转入转出申请。
 *
 * @param id 转考申请ID。
 * @param data 转考申请修改数据。
 * @return 修改后的转考申请。
 */
export function updateTransferApplication(id: number, data: TransferApplicationPayload) {
  return http.put(`/transfers/${id}`, data) as unknown as Promise<TransferApplication>
}

/**
 * @brief 撤回考籍转入转出申请。
 *
 * @param id 转考申请ID。
 * @param data 撤回原因。
 * @return 撤回后的转考申请。
 */
export function withdrawTransferApplication(id: number, data: TransferWithdrawPayload) {
  return http.put(`/transfers/${id}/withdraw`, data) as unknown as Promise<TransferApplication>
}

/**
 * @brief 审核通过考籍转入转出申请。
 *
 * @param id 转考申请ID。
 * @param data 审核意见。
 * @return 审核通过后的转考申请。
 */
export function approveTransferApplication(id: number, data: TransferAuditPayload) {
  return http.put(`/transfers/${id}/approve`, data) as unknown as Promise<TransferApplication>
}

/**
 * @brief 驳回考籍转入转出申请。
 *
 * @param id 转考申请ID。
 * @param data 审核意见。
 * @return 驳回后的转考申请。
 */
export function rejectTransferApplication(id: number, data: TransferAuditPayload) {
  return http.put(`/transfers/${id}/reject`, data) as unknown as Promise<TransferApplication>
}

/**
 * @brief 查询考籍转入转出申请流程记录。
 *
 * @param id 转考申请ID。
 * @return 流程记录列表。
 */
export function listTransferFlowRecords(id: number) {
  return http.get(`/transfers/${id}/flow-records`) as unknown as Promise<TransferFlowRecord[]>
}
