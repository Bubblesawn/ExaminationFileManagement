import { http } from './http'

export interface CourseReplacementRule {
  id: number
  sourceCourseCode: string
  sourceCourseName: string
  targetCourseCode: string
  targetCourseName: string
  majorCode?: string
  educationLevel?: string
  credit?: number
  ruleStatus: string
  effectiveDate?: string
  expireDate?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface CourseReplacementRulePage {
  records: CourseReplacementRule[]
  total: number
  size: number
  current: number
}

export interface CourseReplacementRulePayload {
  sourceCourseCode: string
  sourceCourseName: string
  targetCourseCode: string
  targetCourseName: string
  majorCode?: string
  educationLevel?: string
  credit?: number
  effectiveDate?: string
  expireDate?: string
  remark?: string
}

export interface CourseReplacementApplication {
  id: number
  applicationNo: string
  businessType: string
  recordId: number
  recordNo?: string
  candidateId?: number
  candidateName?: string
  idCard?: string
  admissionNo?: string
  applicationStatus: string
  ruleId?: number
  sourceCourseCode?: string
  sourceCourseName?: string
  targetCourseCode?: string
  targetCourseName?: string
  majorCode?: string
  educationLevel?: string
  applyReason?: string
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

export interface CourseReplacementApplicationPage {
  records: CourseReplacementApplication[]
  total: number
  size: number
  current: number
}

export interface CourseReplacementApplicationPayload {
  recordId?: number
  ruleId?: number
  applyReason?: string
  materialIds?: number[]
  remark?: string
}

export interface CourseReplacementAuditPayload {
  auditOpinion: string
}

export interface CourseReplacementWithdrawPayload {
  withdrawReason: string
}

export interface CourseReplacementFlowRecord {
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
 * @brief 分页查询课程顶替规则。
 *
 * @param params 规则分页查询条件。
 * @return 课程顶替规则分页数据。
 */
export function pageCourseReplacementRules(params: {
  pageNo: number
  pageSize: number
  keyword?: string
  ruleStatus?: string
}) {
  return http.get('/course-replacements/rules/page', { params }) as unknown as Promise<CourseReplacementRulePage>
}

/**
 * @brief 查询课程顶替规则详情。
 *
 * @param id 规则ID。
 * @return 课程顶替规则详情。
 */
export function getCourseReplacementRuleDetail(id: number) {
  return http.get(`/course-replacements/rules/${id}`) as unknown as Promise<CourseReplacementRule>
}

/**
 * @brief 新增课程顶替规则。
 *
 * @param data 规则新增数据。
 * @return 新增后的课程顶替规则。
 */
export function createCourseReplacementRule(data: CourseReplacementRulePayload) {
  return http.post('/course-replacements/rules', data) as unknown as Promise<CourseReplacementRule>
}

/**
 * @brief 修改课程顶替规则。
 *
 * @param id 规则ID。
 * @param data 规则修改数据。
 * @return 修改后的课程顶替规则。
 */
export function updateCourseReplacementRule(id: number, data: CourseReplacementRulePayload) {
  return http.put(`/course-replacements/rules/${id}`, data) as unknown as Promise<CourseReplacementRule>
}

/**
 * @brief 修改课程顶替规则状态。
 *
 * @param id 规则ID。
 * @param ruleStatus 目标规则状态。
 * @return 修改后的课程顶替规则。
 */
export function updateCourseReplacementRuleStatus(id: number, ruleStatus: string) {
  return http.put(`/course-replacements/rules/${id}/status`, { ruleStatus }) as unknown as Promise<CourseReplacementRule>
}

/**
 * @brief 分页查询课程顶替申请。
 *
 * @param params 申请分页查询条件。
 * @return 课程顶替申请分页数据。
 */
export function pageCourseReplacementApplications(params: {
  pageNo: number
  pageSize: number
  keyword?: string
  applicationStatus?: string
  recordId?: number
  candidateId?: number
}) {
  return http.get('/course-replacements/applications/page', { params }) as unknown as Promise<CourseReplacementApplicationPage>
}

/**
 * @brief 查询课程顶替申请详情。
 *
 * @param id 申请ID。
 * @return 课程顶替申请详情。
 */
export function getCourseReplacementApplicationDetail(id: number) {
  return http.get(`/course-replacements/applications/${id}`) as unknown as Promise<CourseReplacementApplication>
}

/**
 * @brief 提交课程顶替申请。
 *
 * @param data 申请提交数据。
 * @return 已提交的课程顶替申请。
 */
export function submitCourseReplacementApplication(data: CourseReplacementApplicationPayload) {
  return http.post('/course-replacements/applications', data) as unknown as Promise<CourseReplacementApplication>
}

/**
 * @brief 修改课程顶替申请。
 *
 * @param id 申请ID。
 * @param data 申请修改数据。
 * @return 修改后的课程顶替申请。
 */
export function updateCourseReplacementApplication(id: number, data: CourseReplacementApplicationPayload) {
  return http.put(`/course-replacements/applications/${id}`, data) as unknown as Promise<CourseReplacementApplication>
}

/**
 * @brief 撤回课程顶替申请。
 *
 * @param id 申请ID。
 * @param data 撤回原因。
 * @return 撤回后的课程顶替申请。
 */
export function withdrawCourseReplacementApplication(id: number, data: CourseReplacementWithdrawPayload) {
  return http.put(`/course-replacements/applications/${id}/withdraw`, data) as unknown as Promise<CourseReplacementApplication>
}

/**
 * @brief 审核通过课程顶替申请。
 *
 * @param id 申请ID。
 * @param data 审核意见。
 * @return 审核通过后的课程顶替申请。
 */
export function approveCourseReplacementApplication(id: number, data: CourseReplacementAuditPayload) {
  return http.put(`/course-replacements/applications/${id}/approve`, data) as unknown as Promise<CourseReplacementApplication>
}

/**
 * @brief 驳回课程顶替申请。
 *
 * @param id 申请ID。
 * @param data 审核意见。
 * @return 驳回后的课程顶替申请。
 */
export function rejectCourseReplacementApplication(id: number, data: CourseReplacementAuditPayload) {
  return http.put(`/course-replacements/applications/${id}/reject`, data) as unknown as Promise<CourseReplacementApplication>
}

/**
 * @brief 查询课程顶替申请流程记录。
 *
 * @param id 申请ID。
 * @return 流程记录列表。
 */
export function listCourseReplacementFlowRecords(id: number) {
  return http.get(`/course-replacements/applications/${id}/flow-records`) as unknown as Promise<CourseReplacementFlowRecord[]>
}
