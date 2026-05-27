from fastapi import APIRouter, Depends

from app.models.schemas import AlgorithmResponse, ApplicationMaterialAuditRequest, ChatRequest, ImageTaskRequest, SpeechRequest
from app.api.security import verify_internal_api_key
from app.services.mock_algorithm_service import (
    audit_application_materials,
    classify_image,
    detect_objects,
    segment_image,
    answer_question,
    recognize_speech,
    synthesize_speech,
)

router = APIRouter(dependencies=[Depends(verify_internal_api_key)])


@router.post("/image-classify", response_model=AlgorithmResponse)
def image_classify(request: ImageTaskRequest) -> dict:
    """@brief 识别考籍材料类别。

    @param request 图片算法任务请求。
    @return 图像分类统一响应。
    """
    return classify_image(request)


@router.post("/application-material-audit", response_model=AlgorithmResponse)
def application_material_audit(request: ApplicationMaterialAuditRequest) -> dict:
    """@brief 核验申请材料分类、缺失项和异常提醒。

    @param request 申请材料智能核验请求。
    @return 申请材料核验统一响应。
    """
    return audit_application_materials(request)


@router.post("/object-detect", response_model=AlgorithmResponse)
def object_detect(request: ImageTaskRequest) -> dict:
    """@brief 定位材料中的关键信息区域。

    @param request 图片算法任务请求。
    @return 目标检测统一响应。
    """
    return detect_objects(request)


@router.post("/image-segment", response_model=AlgorithmResponse)
def image_segment(request: ImageTaskRequest) -> dict:
    """@brief 分割材料图片中的可提取区域。

    @param request 图片算法任务请求。
    @return 图像分割统一响应。
    """
    return segment_image(request)


@router.post("/chat", response_model=AlgorithmResponse)
def chat(request: ChatRequest) -> dict:
    """@brief 回答考籍办理常见问题。

    @param request 智能问答请求。
    @return 智能问答统一响应。
    """
    return answer_question(request)


@router.post("/asr", response_model=AlgorithmResponse)
def asr(request: SpeechRequest) -> dict:
    """@brief 将语音输入转换为文本。

    @param request 语音识别请求。
    @return 语音识别统一响应。
    """
    return recognize_speech(request)


@router.post("/tts", response_model=AlgorithmResponse)
def tts(request: ChatRequest) -> dict:
    """@brief 将办理结果和提示信息合成为语音。

    @param request 语音播报请求。
    @return 语音合成统一响应。
    """
    return synthesize_speech(request)

