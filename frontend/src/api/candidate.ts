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

export function pageCandidates(params: { pageNo: number; pageSize: number; keyword?: string }) {
  return http.get('/candidates/page', { params })
}

export function getCandidateDetail(id: number) {
  return http.get<Candidate>(`/candidates/${id}`)
}

export function createCandidate(data: CandidateCreatePayload) {
  return http.post<Candidate>('/candidates', data)
}

export function updateCandidate(id: number, data: CandidateUpdatePayload) {
  return http.put<Candidate>(`/candidates/${id}`, data)
}

export function deleteCandidate(id: number) {
  return http.delete<void>(`/candidates/${id}`)
}

export function previewCandidateImport(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<CandidateImportPreview>('/candidates/import/preview', formData)
}
