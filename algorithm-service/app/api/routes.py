from fastapi import APIRouter

from app.models.schemas import ChatRequest, ImageTaskRequest, SpeechRequest
from app.services.mock_algorithm_service import (
    classify_image,
    detect_objects,
    segment_image,
    answer_question,
    recognize_speech,
    synthesize_speech,
)

router = APIRouter()


@router.post("/image-classify")
def image_classify(request: ImageTaskRequest) -> dict:
    """模拟图像分类接口。"""
    return classify_image(request)


@router.post("/object-detect")
def object_detect(request: ImageTaskRequest) -> dict:
    """模拟目标检测接口。"""
    return detect_objects(request)


@router.post("/image-segment")
def image_segment(request: ImageTaskRequest) -> dict:
    """模拟图像分割接口。"""
    return segment_image(request)


@router.post("/chat")
def chat(request: ChatRequest) -> dict:
    """模拟考籍业务智能问答接口。"""
    return answer_question(request)


@router.post("/asr")
def asr(request: SpeechRequest) -> dict:
    """模拟 ASR 语音识别接口。"""
    return recognize_speech(request)


@router.post("/tts")
def tts(request: ChatRequest) -> dict:
    """模拟 TTS 语音合成接口。"""
    return synthesize_speech(request)

