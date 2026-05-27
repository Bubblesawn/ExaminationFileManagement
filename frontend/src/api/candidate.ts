import { http } from './http'

export interface Candidate {
  id: number
  name: string
  gender?: string
  idCard: string
  admissionNo?: string
  birthDate?: string
  nation?: string
  politicalStatus?: string
  educationLevel?: string
  majorName?: string
  phone?: string
  email?: string
  address?: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface CandidateCreatePayload {
  name: string
  gender?: string
  idCard: string
  admissionNo?: string
  birthDate?: string
  nation?: string
  politicalStatus?: string
  educationLevel?: string
  majorName?: string
  phone?: string
  email?: string
  address?: string
  status?: string
}

export type CandidateUpdatePayload = CandidateCreatePayload

export interface CandidateImportPreview {
  fileName: string
  fileSize: number
  expectedHeaders: string[]
  totalRows: number
  validRows: number
  invalidRows: number
  message: string
}

export interface CandidatePage {
  records: Candidate[]
  total: number
  size: number
  current: number
}

/**
 * @brief 分页查询考生信息。
 *
 * @param params 考生分页查询条件。
 * @return 考生分页结果。
 */
export function pageCandidates(params: { pageNo: number; pageSize: number; keyword?: string }) {
  return http.get('/candidates/page', { params }) as unknown as Promise<CandidatePage>
}

/**
 * @brief 查询考生详情。
 *
 * @param id 考生ID。
 * @return 考生详情。
 */
export function getCandidateDetail(id: number) {
  return http.get(`/candidates/${id}`) as unknown as Promise<Candidate>
}

/**
 * @brief 新增考生信息。
 *
 * @param data 考生表单数据。
 * @return 新增后的考生信息。
 */
export function createCandidate(data: CandidateCreatePayload) {
  return http.post('/candidates', data) as unknown as Promise<Candidate>
}

/**
 * @brief 修改考生信息。
 *
 * @param id 考生ID。
 * @param data 考生表单数据。
 * @return 修改后的考生信息。
 */
export function updateCandidate(id: number, data: CandidateUpdatePayload) {
  return http.put(`/candidates/${id}`, data) as unknown as Promise<Candidate>
}

/**
 * @brief 删除考生信息。
 *
 * @param id 考生ID。
 */
export function deleteCandidate(id: number) {
  return http.delete(`/candidates/${id}`) as unknown as Promise<void>
}

/**
 * @brief 预览导入考生文件。
 *
 * @param file 待导入文件。
 * @return 导入预览结果。
 */
export function previewCandidateImport(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/candidates/import/preview', formData) as unknown as Promise<CandidateImportPreview>
}
