import { http } from './http'

export interface StudentRecord {
  id: number
  candidateId: number
  candidateName?: string
  idCard?: string
  admissionNo?: string
  phone?: string
  recordNo: string
  enrollBatch?: string
  educationLevel?: string
  majorCode?: string
  majorName?: string
  recordStatus: string
  archiveStatus: string
  archiveTime?: string
  archiveOperatorId?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface StudentRecordPage {
  records: StudentRecord[]
  total: number
  size: number
  current: number
}

export interface StudentRecordCreatePayload {
  candidateId: number
  recordNo: string
  enrollBatch?: string
  educationLevel?: string
  majorCode?: string
  majorName?: string
  recordStatus?: string
  remark?: string
}

export interface StudentRecordUpdatePayload {
  candidateId: number
  enrollBatch?: string
  educationLevel?: string
  majorCode?: string
  majorName?: string
  remark?: string
}

export interface StudentRecordStatusPayload {
  recordStatus: string
  changeReason?: string
}

export interface StudentRecordArchivePayload {
  archiveReason?: string
}

export interface RecordChangeLog {
  id: number
  recordId: number
  changeType: string
  changeField?: string
  beforeValue?: string
  afterValue?: string
  changeReason?: string
  operatorId?: number
  operatorName?: string
  operationTime: string
}

export interface RecordChangeLogPage {
  records: RecordChangeLog[]
  total: number
  size: number
  current: number
}

/**
 * @brief 分页查询考籍档案，用于档案选择和档案列表展示。
 *
 * @param params 档案分页查询条件。
 * @return 考籍档案分页结果。
 */
export function pageStudentRecords(params: {
  pageNo: number
  pageSize: number
  keyword?: string
  recordStatus?: string
  archiveStatus?: string
  candidateId?: number
}) {
  return http.get('/records/page', { params }) as unknown as Promise<StudentRecordPage>
}

/**
 * @brief 查询考籍档案详情。
 *
 * @param id 档案ID。
 * @return 考籍档案详情。
 */
export function getStudentRecordDetail(id: number) {
  return http.get(`/records/${id}`) as unknown as Promise<StudentRecord>
}

/**
 * @brief 创建考籍档案。
 *
 * @param data 档案创建表单数据。
 * @return 新增后的考籍档案。
 */
export function createStudentRecord(data: StudentRecordCreatePayload) {
  return http.post('/records', data) as unknown as Promise<StudentRecord>
}

/**
 * @brief 编辑考籍档案基础信息。
 *
 * @param id 档案ID。
 * @param data 档案编辑表单数据。
 * @return 修改后的考籍档案。
 */
export function updateStudentRecord(id: number, data: StudentRecordUpdatePayload) {
  return http.put(`/records/${id}`, data) as unknown as Promise<StudentRecord>
}

/**
 * @brief 更新考籍档案状态。
 *
 * @param id 档案ID。
 * @param data 状态维护表单数据。
 * @return 状态更新后的考籍档案。
 */
export function updateStudentRecordStatus(id: number, data: StudentRecordStatusPayload) {
  return http.put(`/records/${id}/status`, data) as unknown as Promise<StudentRecord>
}

/**
 * @brief 将考籍档案归档。
 *
 * @param id 档案ID。
 * @param data 归档原因。
 * @return 归档后的考籍档案。
 */
export function archiveStudentRecord(id: number, data: StudentRecordArchivePayload) {
  return http.put(`/records/${id}/archive`, data) as unknown as Promise<StudentRecord>
}

/**
 * @brief 分页查询指定档案的变更记录。
 *
 * @param recordId 档案ID。
 * @param params 变更记录分页查询条件。
 * @return 档案变更记录分页结果。
 */
export function pageRecordChangeLogs(
  recordId: number,
  params: { pageNo: number; pageSize: number; changeType?: string }
) {
  return http.get(`/records/${recordId}/change-logs/page`, { params }) as unknown as Promise<RecordChangeLogPage>
}
