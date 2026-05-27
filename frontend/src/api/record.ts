import { http } from './http'

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

export function pageRecordChangeLogs(
  recordId: number,
  params: { pageNo: number; pageSize: number; changeType?: string }
) {
  return http.get<RecordChangeLogPage>(`/records/${recordId}/change-logs/page`, { params })
}
