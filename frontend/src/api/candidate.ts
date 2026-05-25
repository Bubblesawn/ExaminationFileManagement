import { http } from './http'

export interface CandidateCreatePayload {
  name: string
  gender?: string
  idCard: string
  admissionNo?: string
  phone?: string
}

export function pageCandidates(params: { pageNo: number; pageSize: number; keyword?: string }) {
  return http.get('/candidates/page', { params })
}

export function createCandidate(data: CandidateCreatePayload) {
  return http.post('/candidates', data)
}

