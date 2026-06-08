import hashlib

from app.models.schemas import (
    AlgorithmResponse,
    AbnormalMaterialReminder,
    ApplicationMaterialAuditRequest,
    ApplicationMaterialAuditResult,
    ChatAnswerResult,
    ChatReference,
    ChatRequest,
    ClassifiedApplicationMaterial,
    ImageClassifyResult,
    ImageClarityResult,
    ImageQualityResult,
    ImageSegmentResult,
    ImageTaskRequest,
    MaterialFormatValidationResult,
    MaterialPreprocessRequest,
    MaterialPreprocessResult,
    MissingMaterialReminder,
    MaterialSegment,
    MaterialCategoryCandidate,
    DetectedObject,
    ObjectBoundingBox,
    ObjectDetectResult,
    SegmentationPoint,
    SpeechRequest,
    SpeechRecognitionResult,
    SpeechRecognitionSegment,
    SpeechSynthesisResult,
)
from app.services.deepseek_client import answer_with_deepseek
from app.services.image_analysis_service import (
    ImageAnalysis,
    analyze_image,
    estimate_visual_confidence,
    estimate_material_document_score,
    has_material_document_evidence,
    looks_like_certificate_photo,
    looks_like_id_card_document,
    relative_bbox,
    scale_template_bbox,
)
from app.services.yolo_classify_service import build_yolo_material_candidates
from app.services.yolo_segment_service import (
    build_yolo_material_segments,
    write_polygon_mask_image,
    write_segmentation_preview_image,
)

SUPPORTED_IMAGE_SUFFIXES = (".jpg", ".jpeg", ".png", ".bmp", ".webp")
SUPPORTED_DOCUMENT_SUFFIXES = (".pdf",)
SUPPORTED_MATERIAL_SUFFIXES = SUPPORTED_IMAGE_SUFFIXES + SUPPORTED_DOCUMENT_SUFFIXES
SUPPORTED_AUDIO_SUFFIXES = (".wav", ".mp3", ".m4a", ".aac", ".flac", ".ogg", ".webm")
MAX_MATERIAL_SIZE_KB = 10 * 1024
MIN_AUTO_ACCEPT_CONFIDENCE = 0.85
MIN_REVIEW_CONFIDENCE = 0.6
UNKNOWN_MATERIAL_CODE = "UNKNOWN"
UNKNOWN_MATERIAL_NAME = "无法确认材料类别"
MATERIAL_CATEGORY_RULES = {
    "ID_CARD": {
        "name": "身份证材料",
        "keywords": ["idcard", "id-card", "identity", "shenfenzheng", "身份证", "证件", "ID_CARD"],
    },
    "ADMISSION_TICKET": {
        "name": "准考证材料",
        "keywords": ["admission", "ticket", "zkz", "准考证"],
    },
    "DIPLOMA": {
        "name": "学历证书材料",
        "keywords": ["diploma", "degree", "graduation", "certificate", "毕业证", "学历", "学位"],
    },
    "TRANSCRIPT": {
        "name": "成绩单材料",
        "keywords": ["transcript", "score", "grade", "成绩单", "成绩"],
    },
    "EXEMPTION_CERTIFICATE": {
        "name": "免考证明材料",
        "keywords": ["exemption", "免考", "证明"],
    },
    "PHOTO": {
        "name": "考生照片",
        "keywords": ["photo", "avatar", "portrait", "照片", "头像"],
    },
}

APPLICATION_REQUIRED_MATERIALS = {
    "EXEMPTION": [
        ("ID_CARD", "身份证材料"),
        ("TRANSCRIPT", "成绩单材料"),
        ("EXEMPTION_CERTIFICATE", "免考证明材料"),
    ],
    "COURSE_REPLACE": [
        ("ID_CARD", "身份证材料"),
        ("TRANSCRIPT", "成绩单材料"),
        ("DIPLOMA", "学历证书材料"),
    ],
    "TRANSFER": [
        ("ID_CARD", "身份证材料"),
        ("ADMISSION_TICKET", "准考证材料"),
        ("TRANSCRIPT", "成绩单材料"),
    ],
    "GRADUATION": [
        ("ID_CARD", "身份证材料"),
        ("DIPLOMA", "学历证书材料"),
        ("TRANSCRIPT", "成绩单材料"),
        ("PHOTO", "考生照片"),
    ],
}

OBJECT_DETECT_RULES = {
    "ID_CARD": [
        ("PHOTO_AREA", "照片区域", 0.95, (48, 58, 108, 138), "LOW", "用于核验考生照片"),
        ("NAME_AREA", "姓名区域", 0.93, (182, 70, 178, 34), "LOW", "用于核验考生姓名"),
        ("ID_NUMBER_AREA", "身份证号区域", 0.94, (180, 246, 330, 36), "LOW", "用于核验证件号码"),
        ("OCCLUSION_RISK", "疑似遮挡区域", 0.66, (406, 126, 62, 46), "MEDIUM", "建议人工确认是否遮挡关键信息"),
    ],
    "ADMISSION_TICKET": [
        ("ADMISSION_NO_AREA", "准考证号区域", 0.92, (120, 74, 260, 34), "LOW", "用于核验准考证号"),
        ("NAME_AREA", "姓名区域", 0.9, (120, 122, 160, 32), "LOW", "用于核验考生姓名"),
        ("EXAM_INFO_AREA", "考试信息区域", 0.88, (88, 178, 440, 156), "LOW", "用于核验考试课程和考点信息"),
    ],
    "DIPLOMA": [
        ("NAME_AREA", "姓名区域", 0.89, (130, 128, 168, 32), "LOW", "用于核验证书姓名"),
        ("CERTIFICATE_NO_AREA", "证书编号区域", 0.86, (118, 430, 286, 34), "LOW", "用于核验证书编号"),
        ("SEAL_AREA", "印章区域", 0.82, (410, 360, 118, 118), "MEDIUM", "印章区域需要人工确认"),
    ],
    "TRANSCRIPT": [
        ("NAME_AREA", "姓名区域", 0.88, (76, 82, 150, 30), "LOW", "用于核验成绩单姓名"),
        ("SCORE_TABLE_AREA", "成绩表格区域", 0.91, (58, 152, 500, 300), "LOW", "用于提取课程成绩"),
        ("SEAL_AREA", "印章区域", 0.78, (420, 480, 112, 112), "MEDIUM", "印章清晰度建议人工确认"),
    ],
    "EXEMPTION_CERTIFICATE": [
        ("NAME_AREA", "姓名区域", 0.88, (104, 96, 160, 32), "LOW", "用于核验申请人姓名"),
        ("CERTIFICATE_NO_AREA", "证明编号区域", 0.83, (110, 152, 260, 34), "LOW", "用于核验证明编号"),
        ("SEAL_AREA", "印章区域", 0.8, (390, 360, 126, 126), "MEDIUM", "印章区域需要人工确认"),
    ],
    "PHOTO": [
        ("FACE_AREA", "人像区域", 0.96, (156, 54, 220, 280), "LOW", "用于核验考生照片主体"),
        ("BACKGROUND_AREA", "背景区域", 0.82, (42, 36, 448, 328), "LOW", "用于检查照片背景"),
    ],
}

SEGMENT_TYPE_MAPPING = {
    "PHOTO_AREA": "PHOTO",
    "FACE_AREA": "PHOTO",
    "BACKGROUND_AREA": "BACKGROUND",
    "NAME_AREA": "TEXT",
    "ID_NUMBER_AREA": "TEXT",
    "ADMISSION_NO_AREA": "TEXT",
    "EXAM_INFO_AREA": "TEXT",
    "CERTIFICATE_NO_AREA": "TEXT",
    "SCORE_TABLE_AREA": "TABLE",
    "SEAL_AREA": "SEAL",
    "OCCLUSION_RISK": "RISK",
}

SEGMENT_NAME_MAPPING = {
    "PHOTO_AREA": "照片提取区域",
    "FACE_AREA": "人像主体区域",
    "BACKGROUND_AREA": "照片背景区域",
    "NAME_AREA": "姓名文字区域",
    "ID_NUMBER_AREA": "身份证号文字区域",
    "ADMISSION_NO_AREA": "准考证号文字区域",
    "EXAM_INFO_AREA": "考试信息文字区域",
    "CERTIFICATE_NO_AREA": "证书编号文字区域",
    "SCORE_TABLE_AREA": "成绩表格区域",
    "SEAL_AREA": "印章提取区域",
    "OCCLUSION_RISK": "疑似遮挡分割区域",
}

DEFAULT_IMAGE_WIDTH = 600
DEFAULT_IMAGE_HEIGHT = 800

FAQ_RULES = [
    {
        "intent_code": "ARCHIVE_QUERY",
        "intent_name": "考籍档案查询",
        "keywords": ["考籍", "档案", "查询", "信息"],
        "answer": "考籍档案可以在考生信息管理或考籍档案页面查询。建议先输入准考证号、身份证号或姓名进行检索，再进入详情查看基本信息、材料和状态记录。",
        "references": [
            ("考籍档案管理", "支持按考生基础信息查询档案详情、材料和状态变更记录。", "系统业务规则"),
        ],
        "suggestions": ["查询考生基础信息", "查看档案材料状态", "核对档案变更记录"],
    },
    {
        "intent_code": "MATERIAL_UPLOAD",
        "intent_name": "材料上传与审核",
        "keywords": ["材料", "上传", "附件", "审核", "图片"],
        "answer": "材料上传后系统会进行格式校验、图片质量检查和智能识别辅助。若提示不清晰、遮挡或格式不支持，需要重新上传或转人工复核。",
        "references": [
            ("材料预处理", "支持图片格式校验、清晰度检测、分类、检测和分割辅助。", "智能辅助规则"),
        ],
        "suggestions": ["重新上传清晰图片", "查看智能识别结果", "提交人工确认"],
    },
    {
        "intent_code": "EXEMPTION_APPLY",
        "intent_name": "免考申请办理",
        "keywords": ["免考", "申请", "证明", "课程免修"],
        "answer": "办理免考申请时，需要选择免考课程并上传对应证明材料。提交后进入审核流程，审核通过后会同步更新相关业务状态。",
        "references": [
            ("免考业务流程", "支持免考申请提交、审核、驳回、通过和流程记录查询。", "流程办理规则"),
        ],
        "suggestions": ["准备免考证明材料", "查看申请审核进度", "补充缺失材料"],
    },
    {
        "intent_code": "COURSE_REPLACE",
        "intent_name": "课程顶替办理",
        "keywords": ["顶替", "课程", "替代", "课程替换"],
        "answer": "课程顶替需要根据已维护的顶替规则提交申请。系统会关联课程信息和证明材料，审核通过后形成流程记录。",
        "references": [
            ("课程顶替流程", "支持课程顶替规则维护、申请提交和审核处理。", "流程办理规则"),
        ],
        "suggestions": ["确认课程顶替规则", "上传成绩或证明材料", "查看审核记录"],
    },
    {
        "intent_code": "TRANSFER_PROCESS",
        "intent_name": "考籍转入转出",
        "keywords": ["转入", "转出", "转考", "转入办理", "转出办理", "转考进度", "外省", "省内"],
        "answer": "考籍转入转出需要提交转考申请并补充身份、成绩或转出证明等材料。审核通过后，档案状态会按流程联动更新。",
        "references": [
            ("考籍转入转出", "支持转入转出申请、审核和流程记录。", "流程办理规则"),
        ],
        "suggestions": ["核对转考材料", "提交转入转出申请", "关注档案状态变化"],
    },
    {
        "intent_code": "GRADUATION_APPLY",
        "intent_name": "毕业申请办理",
        "keywords": ["毕业", "毕业申请", "资格", "审核"],
        "answer": "毕业申请需要完成资格校验，确认课程、成绩、材料和档案状态满足要求后再提交审核。审核结果可在毕业管理页面查看。",
        "references": [
            ("毕业申请流程", "支持毕业申请、资格校验、审核和结果查询。", "流程办理规则"),
        ],
        "suggestions": ["发起毕业资格校验", "补齐缺失材料", "查看毕业审核结果"],
    },
]

ASR_TEXT_RULES = [
    (["miankao", "exemption", "免考"], "我要办理免考申请，请帮我查询需要上传哪些证明材料。"),
    (["biye", "graduation", "毕业"], "查询毕业申请资格校验结果。"),
    (["zhuanru", "zhuanchu", "transfer", "转入", "转出"], "我要查询考籍转入转出的审核进度。"),
    (["cailiao", "material", "上传", "材料"], "帮我查看材料上传后是否通过智能审核。"),
    (["kaoji", "archive", "档案", "考籍"], "查询考生考籍档案。"),
]

SCENE_INTENT_MAPPING = {
    "ARCHIVE": "ARCHIVE_QUERY",
    "MATERIAL_AUDIT": "MATERIAL_UPLOAD",
    "EXEMPTION": "EXEMPTION_APPLY",
    "COURSE_REPLACE": "COURSE_REPLACE",
    "TRANSFER": "TRANSFER_PROCESS",
    "GRADUATION": "GRADUATION_APPLY",
}


def _success(data: dict) -> dict:
    """@brief 构造算法服务成功响应。

    @param data 响应业务数据。
    @return 符合算法服务统一响应格式的字典。
    """
    return AlgorithmResponse(code=200, message="操作成功", data=data).model_dump()


def _fail(code: int, message: str) -> dict:
    """@brief 构造算法服务失败响应。

    @param code 业务错误码。
    @param message 错误提示。
    @return 符合算法服务统一响应格式的字典。
    """
    return AlgorithmResponse(code=code, message=message, data=None).model_dump()


def _normalize_text(*values: str | None) -> str:
    """@brief 合并分类所需文本并统一大小写。

    @param values 分类相关文本片段。
    @return 标准化后的分类文本。
    """
    return " ".join(value for value in values if value).lower()


def _contains_keyword(text: str, keywords: list[str]) -> bool:
    """@brief 判断文本是否包含任一关键词。

    @param text 已标准化的待匹配文本。
    @param keywords 关键词列表。
    @return 如果命中任一关键词则返回 True。
    """
    return any(keyword.lower() in text for keyword in keywords)


def _check_image_quality(file_url: str, analysis: ImageAnalysis | None = None) -> ImageQualityResult:
    """@brief 根据真实图片分析结果生成质量预检。

    @param file_url 图片文件地址。
    @param analysis 可复用的图片视觉分析结果。
    @return 图片质量检查结果。
    """
    lower_url = file_url.lower()
    issues = list(analysis.issues if analysis else ())
    if not lower_url.endswith(SUPPORTED_IMAGE_SUFFIXES):
        issues.append("UNSUPPORTED_IMAGE_FORMAT")
    if any(keyword in lower_url for keyword in ["blur", "low", "unclear"]):
        issues.append("LOW_DEFINITION")
    if any(keyword in lower_url for keyword in ["blocked", "cover", "occlusion"]):
        issues.append("OCCLUSION")
    if analysis and analysis.loaded and not has_material_document_evidence(analysis):
        issues.append("MATERIAL_EVIDENCE_INSUFFICIENT")
    return ImageQualityResult(readable=not issues, issues=issues)


def _check_document_quality(file_url: str, file_name: str | None = None) -> ImageQualityResult:
    """@brief 根据文件地址生成 PDF 等非图片材料的可读性预检。

    @param file_url 材料文件地址。
    @param file_name 原始文件名。
    @return 非图片材料质量检查结果。
    """
    file_suffix = _extract_file_suffix(file_url, file_name)
    lower_text = _normalize_text(file_url, file_name)
    issues = []
    if file_suffix not in SUPPORTED_DOCUMENT_SUFFIXES:
        issues.append("UNSUPPORTED_MATERIAL_FORMAT")
    if any(keyword in lower_text for keyword in ["blocked", "cover", "occlusion"]):
        issues.append("OCCLUSION")
    return ImageQualityResult(readable=not issues, issues=issues)


def _extract_file_suffix(file_url: str, file_name: str | None = None) -> str:
    """@brief 提取材料文件后缀。

    @param file_url 材料文件地址。
    @param file_name 原始文件名。
    @return 小写文件后缀，无法识别时返回空字符串。
    """
    source = file_name or file_url.split("?")[0].split("#")[0]
    last_segment = source.rsplit("/", 1)[-1].rsplit("\\", 1)[-1]
    if "." not in last_segment:
        return ""
    return "." + last_segment.rsplit(".", 1)[-1].lower()


def _validate_material_format(request: MaterialPreprocessRequest) -> MaterialFormatValidationResult:
    """@brief 校验材料文件格式和大小。

    @param request 材料预处理请求。
    @return 材料格式校验结果。
    """
    file_suffix = _extract_file_suffix(request.file_url, request.file_name)
    issues = []
    if file_suffix not in SUPPORTED_MATERIAL_SUFFIXES:
        issues.append("UNSUPPORTED_MATERIAL_FORMAT")
    if request.file_size_kb is not None and request.file_size_kb > MAX_MATERIAL_SIZE_KB:
        issues.append("FILE_SIZE_EXCEEDED")
    if request.content_type and file_suffix in SUPPORTED_IMAGE_SUFFIXES and not request.content_type.startswith("image/"):
        issues.append("CONTENT_TYPE_MISMATCH")
    if request.content_type and file_suffix == ".pdf" and request.content_type != "application/pdf":
        issues.append("CONTENT_TYPE_MISMATCH")
    return MaterialFormatValidationResult(
        valid=not issues,
        file_suffix=file_suffix,
        content_type=request.content_type,
        max_size_kb=MAX_MATERIAL_SIZE_KB,
        issues=issues,
    )


def _detect_image_clarity(file_url: str, file_suffix: str) -> ImageClarityResult:
    """@brief 根据材料地址模拟图片清晰度检测。

    @param file_url 材料文件地址。
    @param file_suffix 材料文件后缀。
    @return 图片清晰度检测结果。
    """
    if file_suffix not in SUPPORTED_IMAGE_SUFFIXES:
        return ImageClarityResult(
            image=False,
            score=0,
            level="NOT_IMAGE",
            readable=True,
            issues=[],
            suggestion="非图片材料已跳过清晰度检测，请按格式校验结果继续处理。",
        )

    lower_url = file_url.lower()
    analysis = analyze_image(file_url)
    issues = list(analysis.issues)
    score = estimate_visual_confidence(0.92, analysis)
    if any(keyword in lower_url for keyword in ["blur", "low", "unclear"]):
        issues.append("LOW_DEFINITION")
        score = min(score, 0.42)
    if any(keyword in lower_url for keyword in ["dark", "shadow"]):
        issues.append("LOW_BRIGHTNESS")
        score = min(score, 0.58)
    if any(keyword in lower_url for keyword in ["small", "thumbnail"]):
        issues.append("LOW_RESOLUTION")
        score = min(score, 0.55)
    if any(keyword in lower_url for keyword in ["blocked", "cover", "occlusion"]):
        issues.append("OCCLUSION")
        score = min(score, 0.5)

    issues = list(dict.fromkeys(issues))
    if score >= 0.8:
        level = "CLEAR"
        suggestion = "图片清晰度满足自动预处理要求。"
    elif score >= 0.6:
        level = "REVIEW"
        suggestion = "图片基本可读，建议进入人工复核。"
    else:
        level = "BLURRY"
        suggestion = "图片清晰度不足，建议退回并重新上传。"
    return ImageClarityResult(
        image=True,
        score=score,
        level=level,
        readable=score >= 0.6 and "OCCLUSION" not in issues,
        issues=issues,
        suggestion=suggestion,
    )


def _score_category(
        category_code: str,
        rule: dict,
        strong_text: str,
        weak_text: str,
        analysis: ImageAnalysis | None = None) -> float:
    """@brief 根据材料规则、文本提示和视觉证据计算分类置信度。

    @param rule 材料分类规则。
    @param strong_text 原始文件名等相对可靠文本。
    @param weak_text 前端选择、业务场景和文件地址等弱提示文本。
    @param analysis 图片视觉分析结果。
    @return 分类置信度。
    """
    strong_match_count = sum(1 for keyword in rule["keywords"] if keyword.lower() in strong_text)
    weak_match_count = sum(1 for keyword in rule["keywords"] if keyword.lower() in weak_text)
    document_score = estimate_material_document_score(analysis) if analysis else 0.0
    if document_score < 0.48 and not (analysis and looks_like_certificate_photo(analysis)):
        return 0.08
    if strong_match_count == 0 and weak_match_count == 0:
        if category_code == "ID_CARD" and analysis and looks_like_id_card_document(analysis):
            return estimate_visual_confidence(0.72, analysis)
        return round(min(0.32, 0.12 + document_score * 0.18), 2)

    confidence = 0.34 + document_score * 0.22 + strong_match_count * 0.18 + min(weak_match_count, 1) * 0.08
    if category_code == "ID_CARD" and analysis and looks_like_id_card_document(analysis):
        confidence += 0.1
    return round(min(0.88, confidence), 2)


def _build_material_candidates(
        request: ImageTaskRequest,
        analysis: ImageAnalysis | None = None) -> list[MaterialCategoryCandidate]:
    """@brief 生成材料类别候选结果。

    @param request 图片算法任务请求。
    @param analysis 图片视觉分析结果。
    @return 按置信度倒序排列的材料类别候选项。
    """
    yolo_candidates = build_yolo_material_candidates(request, analysis)
    if yolo_candidates:
        return yolo_candidates

    strong_text = _normalize_text(request.file_name)
    weak_text = _normalize_text(request.material_type_hint)
    candidates = [
        MaterialCategoryCandidate(
            category_code=category_code,
            category_name=rule["name"],
            confidence=_score_category(category_code, rule, strong_text, weak_text, analysis),
        )
        for category_code, rule in MATERIAL_CATEGORY_RULES.items()
    ]
    if analysis and looks_like_certificate_photo(analysis):
        candidates.append(
            MaterialCategoryCandidate(
                category_code="PHOTO",
                category_name=MATERIAL_CATEGORY_RULES["PHOTO"]["name"],
                confidence=estimate_visual_confidence(0.78, analysis),
            )
        )
    candidates = sorted(candidates, key=lambda item: item.confidence, reverse=True)
    if candidates[0].confidence < MIN_REVIEW_CONFIDENCE:
        candidates.insert(
            0,
            MaterialCategoryCandidate(
                category_code=UNKNOWN_MATERIAL_CODE,
                category_name=UNKNOWN_MATERIAL_NAME,
                confidence=candidates[0].confidence,
            )
        )
    return candidates[:3]


def _build_preprocess_candidates(request: MaterialPreprocessRequest) -> list[MaterialCategoryCandidate]:
    """@brief 生成材料预处理分类候选结果。

    @param request 材料预处理请求。
    @return 按置信度倒序排列的材料类别候选项。
    """
    image_request = ImageTaskRequest(
        file_url=request.file_url,
        business_id=request.business_id,
        scene=request.scene,
        file_name=request.file_name,
        material_type_hint=request.material_type_hint,
    )
    file_suffix = _extract_file_suffix(request.file_url, request.file_name)
    analysis = None if file_suffix in SUPPORTED_DOCUMENT_SUFFIXES else analyze_image(request.file_url)
    return _build_material_candidates(image_request, analysis)


def _match_material_category(request: ImageTaskRequest, analysis: ImageAnalysis | None = None) -> str:
    """@brief 根据请求信息匹配材料类别。

    @param request 图片算法任务请求。
    @param analysis 图片视觉分析结果。
    @return 最匹配的材料类别编码。
    """
    return _build_material_candidates(request, analysis)[0].category_code


def _is_known_material_category(category_code: str) -> bool:
    """@brief 判断类别编码是否属于可执行检测和分割的材料类别。

    @param category_code 材料类别编码。
    @return 已知材料类别返回 True。
    """
    return category_code in MATERIAL_CATEGORY_RULES


def _build_detected_objects(
        category_code: str,
        file_url: str,
        analysis: ImageAnalysis | None = None) -> list[DetectedObject]:
    """@brief 根据真实图片尺寸和材料类别生成关键区域检测结果。

    @param category_code 材料类别编码。
    @param file_url 图片文件地址。
    @param analysis 图片视觉分析结果。
    @return 关键区域检测对象列表。
    """
    visual_analysis = analysis or analyze_image(file_url)
    if not _is_known_material_category(category_code) or not has_material_document_evidence(visual_analysis):
        return []
    detected_objects = []
    for object_code, object_name, confidence, template_bbox, risk_level, remark in OBJECT_DETECT_RULES.get(
            category_code, OBJECT_DETECT_RULES["ID_CARD"]):
        bbox = scale_template_bbox(template_bbox, visual_analysis)
        detected_objects.append(
            DetectedObject(
                object_code=object_code,
                object_name=object_name,
                confidence=estimate_visual_confidence(confidence, visual_analysis),
                bbox=ObjectBoundingBox(x=bbox.x, y=bbox.y, width=bbox.width, height=bbox.height),
                risk_level=risk_level,
                remark=f"{remark}；已按真实图片尺寸和主体边界定位",
            )
        )
    if any(keyword in file_url.lower() for keyword in ["blocked", "cover", "occlusion"]):
        bbox = relative_bbox(0.58, 0.2, 0.18, 0.12, visual_analysis)
        detected_objects.append(
            DetectedObject(
                object_code="OCCLUSION_RISK",
                object_name="疑似遮挡区域",
                confidence=estimate_visual_confidence(0.74, visual_analysis),
                bbox=ObjectBoundingBox(x=bbox.x, y=bbox.y, width=bbox.width, height=bbox.height),
                risk_level="HIGH",
                remark="检测到疑似遮挡，建议退回重传或人工复核",
            )
        )
    if visual_analysis.loaded and (
            "BLUR_RISK" in visual_analysis.issues
            or "LOW_CONTRAST" in visual_analysis.issues
            or "LOW_LIGHT" in visual_analysis.issues):
        bbox = relative_bbox(0.08, 0.08, 0.84, 0.84, visual_analysis)
        detected_objects.append(
            DetectedObject(
                object_code="QUALITY_RISK",
                object_name="图像质量风险区域",
                confidence=0.7,
                bbox=ObjectBoundingBox(x=bbox.x, y=bbox.y, width=bbox.width, height=bbox.height),
                risk_level="MEDIUM",
                remark=f"图片存在质量风险：{', '.join(visual_analysis.issues)}",
            )
        )
    return detected_objects


def _build_document_segment(category_code: str, analysis: ImageAnalysis) -> MaterialSegment:
    """@brief 根据真实图片主体边界生成整张材料主体分割区域。

    @param category_code 材料类别编码。
    @param analysis 图片视觉分析结果。
    @return 材料主体分割区域。
    """
    visual_bbox = analysis.document_bbox or relative_bbox(0.04, 0.04, 0.92, 0.92, analysis)
    image_width = analysis.width or DEFAULT_IMAGE_WIDTH
    image_height = analysis.height or DEFAULT_IMAGE_HEIGHT
    bbox = ObjectBoundingBox(
        x=visual_bbox.x,
        y=visual_bbox.y,
        width=visual_bbox.width,
        height=visual_bbox.height,
    )
    return MaterialSegment(
        segment_code=f"{category_code}_DOCUMENT",
        segment_name="材料主体区域",
        segment_type="DOCUMENT",
        confidence=estimate_visual_confidence(0.9, analysis),
        bbox=bbox,
        polygon=_rectangle_to_polygon(bbox),
        mask_url=write_polygon_mask_image(
            "fallback-document",
            category_code,
            1,
            _rectangle_to_polygon(bbox),
            image_width,
            image_height,
        ),
        area_ratio=round((bbox.width * bbox.height) / (image_width * image_height), 4),
        extraction_priority=1,
        need_manual_review=not analysis.loaded or bool(analysis.issues),
        remark="根据真实图片主体边界生成，用于裁剪材料主体并去除边缘背景",
    )


def _rectangle_to_polygon(bbox: ObjectBoundingBox) -> list[SegmentationPoint]:
    """@brief 将外接矩形转换为四点轮廓。

    @param bbox 目标区域外接矩形。
    @return 按左上、右上、右下、左下顺序排列的轮廓点。
    """
    right = bbox.x + bbox.width
    bottom = bbox.y + bbox.height
    return [
        SegmentationPoint(x=bbox.x, y=bbox.y),
        SegmentationPoint(x=right, y=bbox.y),
        SegmentationPoint(x=right, y=bottom),
        SegmentationPoint(x=bbox.x, y=bottom),
    ]


def _build_segments(category_code: str, file_url: str, analysis: ImageAnalysis | None = None) -> list[MaterialSegment]:
    """@brief 根据目标检测结果生成图像分割区域。

    @param category_code 材料类别编码。
    @param file_url 图片文件地址。
    @param analysis 图片视觉分析结果。
    @return 可用于材料区域提取的分割结果列表。
    """
    visual_analysis = analysis or analyze_image(file_url)
    if not _is_known_material_category(category_code) or not has_material_document_evidence(visual_analysis):
        return []
    image_width = visual_analysis.width or DEFAULT_IMAGE_WIDTH
    image_height = visual_analysis.height or DEFAULT_IMAGE_HEIGHT
    segments = [_build_document_segment(category_code, visual_analysis)]
    for index, detected_object in enumerate(_build_detected_objects(category_code, file_url, visual_analysis), start=2):
        segment_type = SEGMENT_TYPE_MAPPING.get(detected_object.object_code, "TEXT")
        segment_name = SEGMENT_NAME_MAPPING.get(detected_object.object_code, detected_object.object_name)
        bbox = detected_object.bbox
        area_ratio = round((bbox.width * bbox.height) / (image_width * image_height), 4)
        is_risk_segment = detected_object.risk_level in {"MEDIUM", "HIGH"} or segment_type == "RISK"
        segments.append(
            MaterialSegment(
                segment_code=detected_object.object_code,
                segment_name=segment_name,
                segment_type=segment_type,
                confidence=max(0.0, round(detected_object.confidence - 0.03, 2)),
                bbox=bbox,
                polygon=_rectangle_to_polygon(bbox),
                mask_url=write_polygon_mask_image(
                    f"fallback-{detected_object.object_code}",
                    category_code,
                    index,
                    _rectangle_to_polygon(bbox),
                    image_width,
                    image_height,
                ),
                area_ratio=area_ratio,
                extraction_priority=index,
                need_manual_review=is_risk_segment,
                remark=detected_object.remark,
            )
        )
    return segments


def _decide_classify_action(confidence: float, quality: ImageQualityResult) -> tuple[str, bool]:
    """@brief 根据分类置信度和图片质量给出建议动作。

    @param confidence 分类置信度。
    @param quality 图片质量检查结果。
    @return 建议动作和人工复核标记。
    """
    if not quality.readable or confidence < MIN_REVIEW_CONFIDENCE:
        return "REJECT", True
    if confidence >= MIN_AUTO_ACCEPT_CONFIDENCE:
        return "ACCEPT", False
    return "REVIEW", True


def _build_classified_application_materials(
        request: ApplicationMaterialAuditRequest) -> list[ClassifiedApplicationMaterial]:
    """@brief 生成申请材料分类结果列表。

    @param request 申请材料智能核验请求。
    @return 每份材料的分类、质量和处理建议。
    """
    classified_materials = []
    for item in request.materials:
        image_request = ImageTaskRequest(
            file_url=item.file_url,
            business_id=request.business_id,
            scene=request.application_type,
            file_name=item.file_name,
            material_type_hint=item.material_type_hint or item.uploaded_category_code,
        )
        file_suffix = _extract_file_suffix(item.file_url, item.file_name)
        analysis = None if file_suffix in SUPPORTED_DOCUMENT_SUFFIXES else analyze_image(item.file_url)
        quality = _check_document_quality(item.file_url, item.file_name) if file_suffix in SUPPORTED_DOCUMENT_SUFFIXES else _check_image_quality(item.file_url, analysis)
        candidates = _build_material_candidates(image_request, analysis)
        best_candidate = candidates[0]
        suggested_action, need_manual_review = _decide_classify_action(best_candidate.confidence, quality)
        if item.uploaded_category_code and item.uploaded_category_code != best_candidate.category_code:
            suggested_action = "REVIEW"
            need_manual_review = True
        classified_materials.append(
            ClassifiedApplicationMaterial(
                material_id=item.material_id,
                file_url=item.file_url,
                file_name=item.file_name,
                uploaded_category_code=item.uploaded_category_code,
                category_code=best_candidate.category_code,
                category_name=best_candidate.category_name,
                confidence=best_candidate.confidence,
                candidates=candidates,
                quality=quality,
                suggested_action=suggested_action,
                need_manual_review=need_manual_review,
            )
        )
    return classified_materials


def _get_required_categories(application_type: str) -> list[MaterialCategoryCandidate]:
    """@brief 获取申请类型要求的材料类别。

    @param application_type 申请类型。
    @return 必交材料类别列表。
    """
    normalized_type = application_type.upper()
    rules = APPLICATION_REQUIRED_MATERIALS.get(normalized_type, APPLICATION_REQUIRED_MATERIALS["EXEMPTION"])
    return [
        MaterialCategoryCandidate(category_code=category_code, category_name=category_name, confidence=1.0)
        for category_code, category_name in rules
    ]


def _build_missing_materials(
        required_categories: list[MaterialCategoryCandidate],
        classified_materials: list[ClassifiedApplicationMaterial]) -> list[MissingMaterialReminder]:
    """@brief 生成缺失材料提示。

    @param required_categories 必交材料类别。
    @param classified_materials 已分类材料列表。
    @return 缺失材料提醒列表。
    """
    uploaded_codes = {item.category_code for item in classified_materials if item.suggested_action != "REJECT"}
    return [
        MissingMaterialReminder(
            category_code=item.category_code,
            category_name=item.category_name,
            severity="HIGH",
            message=f"当前申请缺少必交材料：{item.category_name}，请补充上传后再提交审核。",
        )
        for item in required_categories
        if item.category_code not in uploaded_codes
    ]


def _build_abnormal_materials(
        classified_materials: list[ClassifiedApplicationMaterial]) -> list[AbnormalMaterialReminder]:
    """@brief 生成异常材料提醒。

    @param classified_materials 已分类材料列表。
    @return 异常材料提醒列表。
    """
    abnormal_materials = []
    seen_codes: set[str] = set()
    duplicated_codes: set[str] = set()
    for item in classified_materials:
        if item.category_code in seen_codes:
            duplicated_codes.add(item.category_code)
        seen_codes.add(item.category_code)

    for item in classified_materials:
        if not item.quality.readable:
            abnormal_materials.append(
                AbnormalMaterialReminder(
                    material_id=item.material_id,
                    file_url=item.file_url,
                    category_code=item.category_code,
                    category_name=item.category_name,
                    abnormal_type="QUALITY_RISK",
                    risk_level="HIGH",
                    message="材料图片存在格式、清晰度或遮挡问题，可能无法用于审核。",
                    suggestion="请退回申请人重新上传清晰、无遮挡的材料图片。",
                )
            )
        if item.confidence < MIN_REVIEW_CONFIDENCE:
            abnormal_materials.append(
                AbnormalMaterialReminder(
                    material_id=item.material_id,
                    file_url=item.file_url,
                    category_code=item.category_code,
                    category_name=item.category_name,
                    abnormal_type="LOW_CONFIDENCE",
                    risk_level="MEDIUM",
                    message="材料分类置信度偏低，算法无法稳定确认材料类别。",
                    suggestion="请人工核对材料类别，必要时要求补传。",
                )
            )
        if item.uploaded_category_code and item.uploaded_category_code != item.category_code:
            abnormal_materials.append(
                AbnormalMaterialReminder(
                    material_id=item.material_id,
                    file_url=item.file_url,
                    category_code=item.category_code,
                    category_name=item.category_name,
                    abnormal_type="CATEGORY_MISMATCH",
                    risk_level="MEDIUM",
                    message="上传登记类别与智能识别类别不一致。",
                    suggestion="请人工确认材料归类，并修正业务系统中的材料类别。",
                )
            )
        if item.category_code in duplicated_codes:
            abnormal_materials.append(
                AbnormalMaterialReminder(
                    material_id=item.material_id,
                    file_url=item.file_url,
                    category_code=item.category_code,
                    category_name=item.category_name,
                    abnormal_type="DUPLICATED_CATEGORY",
                    risk_level="LOW",
                    message=f"检测到多份{item.category_name}，可能存在重复上传。",
                    suggestion="请保留最清晰、最完整的一份材料，其他材料可转人工判断。",
                )
            )
    return abnormal_materials


def _decide_application_action(
        missing_materials: list[MissingMaterialReminder],
        abnormal_materials: list[AbnormalMaterialReminder]) -> tuple[str, bool]:
    """@brief 根据缺失和异常情况给出申请材料整体建议。

    @param missing_materials 缺失材料列表。
    @param abnormal_materials 异常材料列表。
    @return 整体建议动作和人工复核标记。
    """
    if missing_materials or any(item.risk_level == "HIGH" for item in abnormal_materials):
        return "REJECT", True
    if abnormal_materials:
        return "REVIEW", True
    return "ACCEPT", False


def _match_faq_rule(content: str, scene: str | None) -> tuple[dict, float]:
    """@brief 根据问题内容匹配考籍办理常见问题。

    @param content 用户问题。
    @param scene 业务场景。
    @return 命中的问答规则和匹配置信度。
    """
    normalized_content = _normalize_text(content)
    normalized_scene = (scene or "").upper()
    best_rule = FAQ_RULES[0]
    best_score = 0.0
    for rule in FAQ_RULES:
        matched_count = sum(1 for keyword in rule["keywords"] if keyword.lower() in normalized_content)
        scene_boost = 0.5 if matched_count > 0 and SCENE_INTENT_MAPPING.get(normalized_scene) == rule["intent_code"] else 0.0
        current_score = matched_count + scene_boost
        if current_score > best_score:
            best_rule = rule
            best_score = current_score
    if best_score == 0 and normalized_scene in SCENE_INTENT_MAPPING:
        scene_intent_code = SCENE_INTENT_MAPPING[normalized_scene]
        for rule in FAQ_RULES:
            if rule["intent_code"] == scene_intent_code:
                return rule, 0.62
    if best_score == 0:
        return {
            "intent_code": "GENERAL_CONSULT",
            "intent_name": "通用考籍咨询",
            "keywords": [],
            "answer": "当前问题可以先按考籍档案、材料审核、免考、课程顶替、转入转出或毕业申请方向处理。建议补充办理事项、考生信息或材料类型，系统会给出更准确的办理提示。",
            "references": [
                ("智能辅助范围", "覆盖考籍档案、材料审核、业务申请和流程查询等常见问题。", "系统业务规则"),
            ],
            "suggestions": ["补充具体办理事项", "说明材料类型", "转人工确认特殊情况"],
        }, 0.52
    return best_rule, round(min(0.95, 0.68 + best_score * 0.09), 2)


def _build_chat_references(rule: dict) -> list[ChatReference]:
    """@brief 生成智能问答参考依据。

    @param rule 命中的问答规则。
    @return 问答参考依据列表。
    """
    return [
        ChatReference(title=title, content=content, source=source)
        for title, content, source in rule["references"]
    ]


def _normalize_deepseek_intent_code(intent_code: str | None, fallback_intent_code: str) -> str:
    """@brief 约束 DeepSeek 返回的意图编码范围。

    @param intent_code 模型返回的意图编码。
    @param fallback_intent_code 本地规则命中的兜底意图编码。
    @return 合法的业务意图编码。
    """
    allowed_intent_codes = {
        "ARCHIVE_QUERY",
        "MATERIAL_UPLOAD",
        "EXEMPTION_APPLY",
        "COURSE_REPLACE",
        "TRANSFER_PROCESS",
        "GRADUATION_APPLY",
        "GENERAL_CONSULT",
    }
    if intent_code in allowed_intent_codes:
        return intent_code
    return fallback_intent_code


def _mock_recognized_text(audio_url: str) -> str:
    """@brief 根据音频地址模拟识别文本。

    @param audio_url 音频文件地址。
    @return 模拟 ASR 识别文本。
    """
    normalized_audio_url = audio_url.lower()
    for keywords, text in ASR_TEXT_RULES:
        if _contains_keyword(normalized_audio_url, keywords):
            return text
    return "查询考生考籍档案。"


def _split_asr_segments(text: str) -> list[SpeechRecognitionSegment]:
    """@brief 将模拟识别文本拆分为语音片段。

    @param text 完整识别文本。
    @return 语音识别片段列表。
    """
    parts = [part for part in text.replace("，", "，|").replace("。", "。|").split("|") if part]
    segments = []
    current_start = 0.0
    for index, part in enumerate(parts):
        duration = max(1.2, round(len(part) * 0.16, 1))
        end_time = round(current_start + duration, 1)
        segments.append(
            SpeechRecognitionSegment(
                start_time=current_start,
                end_time=end_time,
                text=part,
                confidence=max(0.82, round(0.94 - index * 0.03, 2)),
            )
        )
        current_start = end_time
    return segments


def classify_image(request: ImageTaskRequest) -> dict:
    """@brief 识别考籍材料类别。

    @param request 图片算法任务请求。
    @return 图像分类统一响应。
    """
    if not request.file_url.strip():
        return _fail(400, "图片文件地址不能为空")
    if not request.file_url.lower().endswith(SUPPORTED_IMAGE_SUFFIXES):
        return _fail(415, "文件格式不支持")
    analysis = analyze_image(request.file_url)
    quality = _check_image_quality(request.file_url, analysis)
    candidates = _build_material_candidates(request, analysis)
    best_candidate = candidates[0]
    suggested_action, need_manual_review = _decide_classify_action(best_candidate.confidence, quality)
    result = ImageClassifyResult(
        business_id=request.business_id,
        file_url=request.file_url,
        category_code=best_candidate.category_code,
        category_name=best_candidate.category_name,
        confidence=best_candidate.confidence,
        candidates=candidates,
        quality=quality,
        suggested_action=suggested_action,
        need_manual_review=need_manual_review,
    )
    return _success(result.model_dump())


def preprocess_material(request: MaterialPreprocessRequest) -> dict:
    """@brief 执行材料格式校验、图片清晰度检测和基础分类。

    @param request 材料预处理请求。
    @return 材料预处理统一响应。
    """
    if not request.file_url.strip():
        return _fail(400, "材料文件地址不能为空")
    format_validation = _validate_material_format(request)
    clarity = _detect_image_clarity(request.file_url, format_validation.file_suffix)
    candidates = _build_preprocess_candidates(request)
    best_candidate = candidates[0]

    if not format_validation.valid:
        suggested_action = "REJECT"
    elif not clarity.readable:
        suggested_action = "REJECT"
    elif best_candidate.confidence >= MIN_AUTO_ACCEPT_CONFIDENCE:
        suggested_action = "ACCEPT"
    elif best_candidate.confidence >= MIN_REVIEW_CONFIDENCE:
        suggested_action = "REVIEW"
    else:
        suggested_action = "REJECT"

    result = MaterialPreprocessResult(
        business_id=request.business_id,
        file_url=request.file_url,
        file_name=request.file_name,
        scene=request.scene,
        format_validation=format_validation,
        clarity=clarity,
        category_code=best_candidate.category_code,
        category_name=best_candidate.category_name,
        confidence=best_candidate.confidence,
        candidates=candidates,
        suggested_action=suggested_action,
        need_manual_review=suggested_action != "ACCEPT",
    )
    return _success(result.model_dump())


def audit_application_materials(request: ApplicationMaterialAuditRequest) -> dict:
    """@brief 核验业务申请材料分类、缺失项和异常风险。

    @param request 申请材料智能核验请求。
    @return 申请材料智能核验统一响应。
    """
    if not request.application_type.strip():
        return _fail(400, "申请类型不能为空")
    if not request.materials:
        required_categories = _get_required_categories(request.application_type)
        missing_materials = _build_missing_materials(required_categories, [])
        result = ApplicationMaterialAuditResult(
            business_id=request.business_id,
            application_type=request.application_type,
            applicant_name=request.applicant_name,
            required_categories=required_categories,
            classified_materials=[],
            missing_materials=missing_materials,
            abnormal_materials=[],
            summary={
                "material_count": 0,
                "missing_count": len(missing_materials),
                "abnormal_count": 0,
                "manual_review_count": 0,
            },
            suggested_action="REJECT",
            need_manual_review=True,
        )
        return _success(result.model_dump())

    for item in request.materials:
        if not item.file_url.strip():
            return _fail(400, "材料文件地址不能为空")
        if _extract_file_suffix(item.file_url, item.file_name) not in SUPPORTED_MATERIAL_SUFFIXES:
            return _fail(415, "文件格式不支持")

    required_categories = _get_required_categories(request.application_type)
    classified_materials = _build_classified_application_materials(request)
    missing_materials = _build_missing_materials(required_categories, classified_materials)
    abnormal_materials = _build_abnormal_materials(classified_materials)
    suggested_action, need_manual_review = _decide_application_action(missing_materials, abnormal_materials)
    result = ApplicationMaterialAuditResult(
        business_id=request.business_id,
        application_type=request.application_type,
        applicant_name=request.applicant_name,
        required_categories=required_categories,
        classified_materials=classified_materials,
        missing_materials=missing_materials,
        abnormal_materials=abnormal_materials,
        summary={
            "material_count": len(classified_materials),
            "missing_count": len(missing_materials),
            "abnormal_count": len(abnormal_materials),
            "manual_review_count": sum(1 for item in classified_materials if item.need_manual_review),
        },
        suggested_action=suggested_action,
        need_manual_review=need_manual_review,
    )
    return _success(result.model_dump())


def detect_objects(request: ImageTaskRequest) -> dict:
    """@brief 定位材料图片中的关键信息区域。

    @param request 图片算法任务请求。
    @return 目标检测统一响应。
    """
    if not request.file_url.strip():
        return _fail(400, "图片文件地址不能为空")
    if not request.file_url.lower().endswith(SUPPORTED_IMAGE_SUFFIXES):
        return _fail(415, "文件格式不支持")
    analysis = analyze_image(request.file_url)
    quality = _check_image_quality(request.file_url, analysis)
    category_code = _match_material_category(request, analysis)
    objects = _build_detected_objects(category_code, request.file_url, analysis)
    has_high_risk = any(item.risk_level == "HIGH" for item in objects)
    suggested_action = "REJECT" if not quality.readable or not objects else "REVIEW" if has_high_risk else "ACCEPT"
    result = ObjectDetectResult(
        business_id=request.business_id,
        file_url=request.file_url,
        scene=request.scene,
        material_type_hint=request.material_type_hint,
        objects=objects,
        quality=quality,
        suggested_action=suggested_action,
        need_manual_review=suggested_action != "ACCEPT",
    )
    return _success(result.model_dump())


def segment_image(request: ImageTaskRequest) -> dict:
    """@brief 分割材料图片中的可提取区域。

    @param request 图片算法任务请求。
    @return 图像分割统一响应。
    """
    if not request.file_url.strip():
        return _fail(400, "图片文件地址不能为空")
    if not request.file_url.lower().endswith(SUPPORTED_IMAGE_SUFFIXES):
        return _fail(415, "文件格式不支持")
    analysis = analyze_image(request.file_url)
    quality = _check_image_quality(request.file_url, analysis)
    yolo_segment_result = build_yolo_material_segments(request.file_url, analysis)
    if yolo_segment_result:
        category_code, category_name, segments, segmentation_image_url = yolo_segment_result
    else:
        category_code = UNKNOWN_MATERIAL_CODE
        category_name = UNKNOWN_MATERIAL_NAME
        segments = []
        segmentation_image_url = None
    has_review_segment = any(item.need_manual_review for item in segments)
    suggested_action = "REJECT" if not quality.readable or not segments else "REVIEW" if has_review_segment else "ACCEPT"
    result = ImageSegmentResult(
        business_id=request.business_id,
        file_url=request.file_url,
        segmentation_image_url=segmentation_image_url,
        scene=request.scene,
        material_type_hint=request.material_type_hint,
        category_code=category_code,
        category_name=category_name,
        image_width=analysis.width or DEFAULT_IMAGE_WIDTH,
        image_height=analysis.height or DEFAULT_IMAGE_HEIGHT,
        segments=segments,
        quality=quality,
        suggested_action=suggested_action,
        need_manual_review=suggested_action != "ACCEPT",
    )
    return _success(result.model_dump())


def answer_question(request: ChatRequest) -> dict:
    """@brief 回答考籍办理常见问题。

    @param request 智能问答请求。
    @return 智能问答统一响应。
    """
    if not request.content.strip():
        return _fail(400, "问题内容不能为空")
    rule, confidence = _match_faq_rule(request.content, request.scene)
    local_references = [item.model_dump() for item in _build_chat_references(rule)]
    matched_intent = {"intent_code": rule["intent_code"], "intent_name": rule["intent_name"]}
    deepseek_answer = answer_with_deepseek(request.content, request.scene, local_references, matched_intent)
    if _is_deepseek_answer_usable(deepseek_answer):
        deepseek_confidence = _read_deepseek_confidence(deepseek_answer, confidence)
        result = ChatAnswerResult(
            business_id=request.business_id,
            question=request.content,
            scene=request.scene,
            intent_code=_normalize_deepseek_intent_code(deepseek_answer.get("intent_code"), rule["intent_code"]),
            intent_name=deepseek_answer.get("intent_name") or rule["intent_name"],
            answer=deepseek_answer["answer"],
            confidence=deepseek_confidence,
            references=_build_chat_references(rule),
            suggestions=deepseek_answer.get("suggestions") or rule["suggestions"],
            need_manual_review=deepseek_answer.get(
                "need_manual_review",
                deepseek_confidence < MIN_REVIEW_CONFIDENCE,
            ),
        )
        return _success(result.model_dump())
    result = ChatAnswerResult(
        business_id=request.business_id,
        question=request.content,
        scene=request.scene,
        intent_code=rule["intent_code"],
        intent_name=rule["intent_name"],
        answer=rule["answer"],
        confidence=confidence,
        references=_build_chat_references(rule),
        suggestions=rule["suggestions"],
        need_manual_review=confidence < MIN_REVIEW_CONFIDENCE,
    )
    return _success(result.model_dump())


def _is_deepseek_answer_usable(payload: dict | None) -> bool:
    """@brief 判断 DeepSeek 问答结果是否可直接展示。

    @param payload DeepSeek 返回的结构化问答结果。
    @return 结果完整且置信度达标时返回 True。
    """
    if not payload:
        return False
    if not payload.get("answer"):
        return False
    return True


def _read_deepseek_confidence(payload: dict, fallback_confidence: float) -> float:
    """@brief 读取 DeepSeek 返回的问答置信度。

    @param payload DeepSeek 返回的结构化问答结果。
    @param fallback_confidence 本地规则匹配置信度。
    @return 归一化到 0 到 1 区间的置信度。
    """
    try:
        confidence = float(payload.get("confidence", fallback_confidence))
    except (TypeError, ValueError):
        confidence = fallback_confidence
    return max(0.0, min(1.0, confidence))


def recognize_speech(request: SpeechRequest) -> dict:
    """@brief 将语音输入转换为文本。

    @param request 语音识别请求。
    @return 语音识别统一响应。
    """
    if not request.audio_url.strip():
        return _fail(400, "音频文件地址不能为空")
    if not request.audio_url.lower().endswith(SUPPORTED_AUDIO_SUFFIXES):
        return _fail(415, "音频格式不支持")
    text = _mock_recognized_text(request.audio_url)
    segments = _split_asr_segments(text)
    confidence = min(item.confidence for item in segments) if segments else 0
    result = SpeechRecognitionResult(
        business_id=request.business_id,
        audio_url=request.audio_url,
        scene=request.scene,
        language=request.language_hint or "zh-CN",
        text=text,
        duration_seconds=segments[-1].end_time if segments else 1.2,
        confidence=confidence,
        segments=segments,
        suggested_action="ACCEPT" if confidence >= MIN_AUTO_ACCEPT_CONFIDENCE else "REVIEW",
        need_manual_review=confidence < MIN_AUTO_ACCEPT_CONFIDENCE,
    )
    return _success(result.model_dump())


def synthesize_speech(request: ChatRequest) -> dict:
    """@brief 将办理结果和提示信息合成为语音。

    @param request 语音播报请求。
    @return 语音合成统一响应。
    """
    if not request.content.strip():
        return _fail(400, "播报文本不能为空")
    normalized_scene = (request.scene or "general").lower().replace("_", "-")
    text_hash = hashlib.md5(request.content.encode("utf-8")).hexdigest()[:10]
    duration_seconds = round(max(1.2, len(request.content) * 0.18), 1)
    result = SpeechSynthesisResult(
        business_id=request.business_id,
        text=request.content,
        scene=request.scene,
        voice_name="standard-female-cn",
        language="zh-CN",
        audio_url=f"/mock/audio/tts-{normalized_scene}-{text_hash}.mp3",
        audio_format="mp3",
        duration_seconds=duration_seconds,
        sample_rate=24000,
        suggested_action="ACCEPT",
    )
    return _success(result.model_dump())

