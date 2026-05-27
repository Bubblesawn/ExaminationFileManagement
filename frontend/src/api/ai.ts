import { http } from './http'

export type AiTaskType = 'classify' | 'detect' | 'segment'

export type SuggestedAction = 'ACCEPT' | 'REVIEW' | 'REJECT'

export interface AiImageTaskRequest {
  fileUrl: string
  businessId?: number
  scene?: string
  fileName?: string
  materialTypeHint?: string
}

export interface ImageQualityResult {
  readable: boolean
  issues: string[]
}

export interface MaterialCategoryCandidate {
  category_code: string
  category_name: string
  confidence: number
}

export interface ApplicationMaterialItemRequest {
  materialId?: number
  fileUrl: string
  fileName?: string
  materialTypeHint?: string
  uploadedCategoryCode?: string
}

export interface ApplicationMaterialAuditRequest {
  businessId?: number
  applicationType: string
  applicantName?: string
  materials: ApplicationMaterialItemRequest[]
}

export interface ObjectBoundingBox {
  x: number
  y: number
  width: number
  height: number
}

export interface DetectedObject {
  object_code: string
  object_name: string
  confidence: number
  bbox: ObjectBoundingBox
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH'
  remark?: string
}

export interface MaterialSegment {
  segment_code: string
  segment_name: string
  segment_type: 'TEXT' | 'PHOTO' | 'SEAL' | 'TABLE' | 'BACKGROUND' | 'DOCUMENT' | 'RISK'
  confidence: number
  bbox: ObjectBoundingBox
  polygon: Array<{ x: number; y: number }>
  mask_url: string
  area_ratio: number
  extraction_priority: number
  need_manual_review: boolean
  remark?: string
}

export interface AiRecognitionData {
  business_id?: number
  file_url?: string
  scene?: string
  material_type_hint?: string
  category_code?: string
  category_name?: string
  confidence?: number
  candidates?: MaterialCategoryCandidate[]
  objects?: DetectedObject[]
  segments?: MaterialSegment[]
  image_width?: number
  image_height?: number
  quality?: ImageQualityResult
  suggested_action?: SuggestedAction
  need_manual_review?: boolean
}

export interface ClassifiedApplicationMaterial {
  material_id?: number
  file_url: string
  file_name?: string
  uploaded_category_code?: string
  category_code: string
  category_name: string
  confidence: number
  candidates: MaterialCategoryCandidate[]
  quality: ImageQualityResult
  suggested_action: SuggestedAction
  need_manual_review: boolean
}

export interface MissingMaterialReminder {
  category_code: string
  category_name: string
  required: boolean
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
  message: string
}

export interface AbnormalMaterialReminder {
  material_id?: number
  file_url?: string
  category_code?: string
  category_name?: string
  abnormal_type: string
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH'
  message: string
  suggestion: string
}

export interface ApplicationMaterialAuditData {
  business_id?: number
  application_type: string
  applicant_name?: string
  required_categories: MaterialCategoryCandidate[]
  classified_materials: ClassifiedApplicationMaterial[]
  missing_materials: MissingMaterialReminder[]
  abnormal_materials: AbnormalMaterialReminder[]
  summary: {
    material_count?: number
    missing_count?: number
    abnormal_count?: number
    manual_review_count?: number
  }
  suggested_action: SuggestedAction
  need_manual_review: boolean
}

export interface AlgorithmResponse<T = AiRecognitionData> {
  code: number
  message: string
  data: T
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface AiChatRequest {
  content: string
  businessId?: number
  scene?: string
}

export interface ChatReference {
  title: string
  content: string
  source: string
}

export interface ChatAnswerData {
  business_id?: number
  question: string
  scene?: string
  intent_code: string
  intent_name: string
  answer: string
  confidence: number
  references: ChatReference[]
  suggestions: string[]
  need_manual_review: boolean
}

const taskUrlMap: Record<AiTaskType, string> = {
  classify: '/ai/image-classify',
  detect: '/ai/object-detect',
  segment: '/ai/image-segment'
}

/**
 * @brief 调用后端智能图片识别接口。
 *
 * @param taskType 识别任务类型。
 * @param payload 图片识别请求参数。
 * @return 后端封装后的算法响应。
 */
export function recognizeImage(taskType: AiTaskType, payload: AiImageTaskRequest) {
  return http.post<unknown, ApiResult<AlgorithmResponse>>(taskUrlMap[taskType], payload)
}

/**
 * @brief 调用申请材料智能核验接口。
 *
 * @param payload 申请材料核验请求参数。
 * @return 后端封装后的申请材料核验算法响应。
 */
export function auditApplicationMaterials(payload: ApplicationMaterialAuditRequest) {
  return http.post<unknown, ApiResult<AlgorithmResponse<ApplicationMaterialAuditData>>>(
    '/ai/application-material-audit',
    payload
  )
}

/**
 * @brief 调用后端智能问答接口。
 *
 * @param payload 智能问答请求参数。
 * @return 后端封装后的问答算法响应。
 */
export function askAiQuestion(payload: AiChatRequest) {
  return http.post<unknown, ApiResult<AlgorithmResponse<ChatAnswerData>>>('/ai/chat', payload)
}
