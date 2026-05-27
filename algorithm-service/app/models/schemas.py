from typing import Literal

from pydantic import BaseModel, Field


class ImageTaskRequest(BaseModel):
    """@brief 图片算法任务请求。"""

    file_url: str = Field(..., description="图片文件地址")
    business_id: int | None = Field(default=None, description="业务ID")
    scene: str | None = Field(default=None, description="业务场景，例如 MATERIAL_AUDIT、EXEMPTION")
    file_name: str | None = Field(default=None, description="原始文件名")
    material_type_hint: str | None = Field(default=None, description="前端或业务系统提供的材料类型提示")


class ApplicationMaterialItemRequest(BaseModel):
    """@brief 申请材料核验单项材料请求。"""

    material_id: int | None = Field(default=None, description="材料ID")
    file_url: str = Field(..., description="材料文件地址")
    file_name: str | None = Field(default=None, description="原始文件名")
    material_type_hint: str | None = Field(default=None, description="前端或业务系统提供的材料类型提示")
    uploaded_category_code: str | None = Field(default=None, description="业务系统已登记的材料类别编码")


class ApplicationMaterialAuditRequest(BaseModel):
    """@brief 申请材料智能核验请求。"""

    business_id: int | None = Field(default=None, description="业务ID")
    application_type: str = Field(..., description="申请类型，例如 EXEMPTION、COURSE_REPLACE、TRANSFER、GRADUATION")
    applicant_name: str | None = Field(default=None, description="申请人姓名")
    materials: list[ApplicationMaterialItemRequest] = Field(default_factory=list, description="已上传申请材料列表")


class AlgorithmResponse(BaseModel):
    """@brief 算法服务统一响应。"""

    code: int = Field(..., description="业务状态码，200 表示成功")
    message: str = Field(..., description="响应消息")
    data: dict | None = Field(default=None, description="响应数据")


class ImageQualityResult(BaseModel):
    """@brief 图片质量检查结果。"""

    readable: bool = Field(..., description="图片是否可用于人工或算法审核")
    issues: list[str] = Field(default_factory=list, description="质量问题列表")


class MaterialCategoryCandidate(BaseModel):
    """@brief 材料类别候选项。"""

    category_code: str = Field(..., description="材料类别编码")
    category_name: str = Field(..., description="材料类别名称")
    confidence: float = Field(..., ge=0, le=1, description="置信度")


class ImageClassifyResult(BaseModel):
    """@brief 图像分类结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    file_url: str = Field(..., description="图片文件地址")
    category_code: str = Field(..., description="最可能的材料类别编码")
    category_name: str = Field(..., description="最可能的材料类别名称")
    confidence: float = Field(..., ge=0, le=1, description="最高置信度")
    candidates: list[MaterialCategoryCandidate] = Field(default_factory=list, description="候选材料类别")
    quality: ImageQualityResult = Field(..., description="图片质量检查结果")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="建议动作")
    need_manual_review: bool = Field(..., description="是否需要人工复核")


class ClassifiedApplicationMaterial(BaseModel):
    """@brief 申请材料分类结果。"""

    material_id: int | None = Field(default=None, description="材料ID")
    file_url: str = Field(..., description="材料文件地址")
    file_name: str | None = Field(default=None, description="原始文件名")
    uploaded_category_code: str | None = Field(default=None, description="业务系统已登记的材料类别编码")
    category_code: str = Field(..., description="算法识别出的材料类别编码")
    category_name: str = Field(..., description="算法识别出的材料类别名称")
    confidence: float = Field(..., ge=0, le=1, description="分类置信度")
    candidates: list[MaterialCategoryCandidate] = Field(default_factory=list, description="候选材料类别")
    quality: ImageQualityResult = Field(..., description="图片质量检查结果")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="建议动作")
    need_manual_review: bool = Field(..., description="是否需要人工复核")


class MissingMaterialReminder(BaseModel):
    """@brief 缺失材料提示。"""

    category_code: str = Field(..., description="缺失材料类别编码")
    category_name: str = Field(..., description="缺失材料类别名称")
    required: bool = Field(default=True, description="是否必交")
    severity: Literal["LOW", "MEDIUM", "HIGH"] = Field(..., description="提示等级")
    message: str = Field(..., description="缺失提示文案")


class AbnormalMaterialReminder(BaseModel):
    """@brief 异常材料提醒。"""

    material_id: int | None = Field(default=None, description="材料ID")
    file_url: str | None = Field(default=None, description="材料文件地址")
    category_code: str | None = Field(default=None, description="材料类别编码")
    category_name: str | None = Field(default=None, description="材料类别名称")
    abnormal_type: str = Field(..., description="异常类型编码")
    risk_level: Literal["LOW", "MEDIUM", "HIGH"] = Field(..., description="风险等级")
    message: str = Field(..., description="异常说明")
    suggestion: str = Field(..., description="处理建议")


class ApplicationMaterialAuditResult(BaseModel):
    """@brief 申请材料智能核验结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    application_type: str = Field(..., description="申请类型")
    applicant_name: str | None = Field(default=None, description="申请人姓名")
    required_categories: list[MaterialCategoryCandidate] = Field(default_factory=list, description="本申请类型要求的材料类别")
    classified_materials: list[ClassifiedApplicationMaterial] = Field(default_factory=list, description="材料分类结果列表")
    missing_materials: list[MissingMaterialReminder] = Field(default_factory=list, description="缺失材料提示列表")
    abnormal_materials: list[AbnormalMaterialReminder] = Field(default_factory=list, description="异常材料提醒列表")
    summary: dict = Field(default_factory=dict, description="核验汇总信息")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="整体建议动作")
    need_manual_review: bool = Field(..., description="是否需要人工复核")


class ObjectBoundingBox(BaseModel):
    """@brief 目标检测边界框。"""

    x: int = Field(..., ge=0, description="左上角横坐标")
    y: int = Field(..., ge=0, description="左上角纵坐标")
    width: int = Field(..., gt=0, description="目标区域宽度")
    height: int = Field(..., gt=0, description="目标区域高度")


class DetectedObject(BaseModel):
    """@brief 材料关键区域检测对象。"""

    object_code: str = Field(..., description="目标区域编码")
    object_name: str = Field(..., description="目标区域名称")
    confidence: float = Field(..., ge=0, le=1, description="目标检测置信度")
    bbox: ObjectBoundingBox = Field(..., description="目标区域边界框")
    risk_level: Literal["LOW", "MEDIUM", "HIGH"] = Field(..., description="风险等级")
    remark: str | None = Field(default=None, description="检测备注")


class ObjectDetectResult(BaseModel):
    """@brief 目标检测结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    file_url: str = Field(..., description="图片文件地址")
    scene: str | None = Field(default=None, description="业务场景")
    material_type_hint: str | None = Field(default=None, description="材料类型提示")
    objects: list[DetectedObject] = Field(default_factory=list, description="检测到的关键区域")
    quality: ImageQualityResult = Field(..., description="图片质量检查结果")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="建议动作")
    need_manual_review: bool = Field(..., description="是否需要人工复核")


class SegmentationPoint(BaseModel):
    """@brief 图像分割轮廓点。"""

    x: int = Field(..., ge=0, description="轮廓点横坐标")
    y: int = Field(..., ge=0, description="轮廓点纵坐标")


class MaterialSegment(BaseModel):
    """@brief 材料分割区域。"""

    segment_code: str = Field(..., description="分割区域编码")
    segment_name: str = Field(..., description="分割区域名称")
    segment_type: Literal["TEXT", "PHOTO", "SEAL", "TABLE", "BACKGROUND", "DOCUMENT", "RISK"] = Field(
        ..., description="分割区域类型"
    )
    confidence: float = Field(..., ge=0, le=1, description="分割置信度")
    bbox: ObjectBoundingBox = Field(..., description="分割区域外接矩形")
    polygon: list[SegmentationPoint] = Field(default_factory=list, description="分割区域轮廓点")
    mask_url: str = Field(..., description="分割掩码访问地址")
    area_ratio: float = Field(..., ge=0, le=1, description="分割区域面积占整图比例")
    extraction_priority: int = Field(..., ge=1, le=9, description="区域提取优先级，数值越小优先级越高")
    need_manual_review: bool = Field(..., description="是否需要人工复核")
    remark: str | None = Field(default=None, description="分割结果备注")


class ImageSegmentResult(BaseModel):
    """@brief 图像分割结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    file_url: str = Field(..., description="图片文件地址")
    scene: str | None = Field(default=None, description="业务场景")
    material_type_hint: str | None = Field(default=None, description="材料类型提示")
    category_code: str = Field(..., description="匹配到的材料类别编码")
    category_name: str = Field(..., description="匹配到的材料类别名称")
    image_width: int = Field(..., gt=0, description="模拟识别图片宽度")
    image_height: int = Field(..., gt=0, description="模拟识别图片高度")
    segments: list[MaterialSegment] = Field(default_factory=list, description="材料分割区域列表")
    quality: ImageQualityResult = Field(..., description="图片质量检查结果")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="建议动作")
    need_manual_review: bool = Field(..., description="是否需要人工复核")


class ChatRequest(BaseModel):
    """@brief 智能问答请求。"""

    content: str = Field(..., description="问题或待播报文本")
    business_id: int | None = Field(default=None, description="业务ID")
    scene: str | None = Field(default=None, description="业务场景，例如 ARCHIVE、EXEMPTION、GRADUATION")


class ChatReference(BaseModel):
    """@brief 智能问答参考依据。"""

    title: str = Field(..., description="参考标题")
    content: str = Field(..., description="参考内容摘要")
    source: str = Field(..., description="参考来源")


class ChatAnswerResult(BaseModel):
    """@brief 智能问答结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    question: str = Field(..., description="用户问题")
    scene: str | None = Field(default=None, description="业务场景")
    intent_code: str = Field(..., description="识别到的问题意图编码")
    intent_name: str = Field(..., description="识别到的问题意图名称")
    answer: str = Field(..., description="问答回复内容")
    confidence: float = Field(..., ge=0, le=1, description="问答匹配置信度")
    references: list[ChatReference] = Field(default_factory=list, description="参考依据")
    suggestions: list[str] = Field(default_factory=list, description="后续可追问或办理建议")
    need_manual_review: bool = Field(..., description="是否建议转人工确认")


class SpeechRequest(BaseModel):
    """@brief 语音识别请求。"""

    audio_url: str = Field(..., description="音频文件地址")
    business_id: int | None = Field(default=None, description="业务ID")
    scene: str | None = Field(default=None, description="业务场景")
    language_hint: str | None = Field(default="zh-CN", description="语种提示")


class SpeechRecognitionSegment(BaseModel):
    """@brief 语音识别片段。"""

    start_time: float = Field(..., ge=0, description="片段开始时间，单位秒")
    end_time: float = Field(..., ge=0, description="片段结束时间，单位秒")
    text: str = Field(..., description="片段识别文本")
    confidence: float = Field(..., ge=0, le=1, description="片段识别置信度")


class SpeechRecognitionResult(BaseModel):
    """@brief 语音识别结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    audio_url: str = Field(..., description="音频文件地址")
    scene: str | None = Field(default=None, description="业务场景")
    language: str = Field(..., description="识别语种")
    text: str = Field(..., description="完整识别文本")
    duration_seconds: float = Field(..., gt=0, description="模拟音频时长，单位秒")
    confidence: float = Field(..., ge=0, le=1, description="整体识别置信度")
    segments: list[SpeechRecognitionSegment] = Field(default_factory=list, description="语音识别片段")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="建议动作")
    need_manual_review: bool = Field(..., description="是否需要人工复核")


class SpeechSynthesisResult(BaseModel):
    """@brief 语音合成结果。"""

    business_id: int | None = Field(default=None, description="业务ID")
    text: str = Field(..., description="播报文本")
    scene: str | None = Field(default=None, description="业务场景")
    voice_name: str = Field(..., description="模拟音色名称")
    language: str = Field(..., description="播报语种")
    audio_url: str = Field(..., description="合成音频访问地址")
    audio_format: str = Field(..., description="音频格式")
    duration_seconds: float = Field(..., gt=0, description="模拟音频时长，单位秒")
    sample_rate: int = Field(..., gt=0, description="采样率")
    suggested_action: Literal["ACCEPT", "REVIEW", "REJECT"] = Field(..., description="建议动作")

