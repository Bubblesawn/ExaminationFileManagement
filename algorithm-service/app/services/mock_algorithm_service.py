from app.models.schemas import ChatRequest, ImageTaskRequest, SpeechRequest


def classify_image(request: ImageTaskRequest) -> dict:
    """返回模拟材料类型识别结果。"""
    return {
        "business_id": request.business_id,
        "file_url": request.file_url,
        "label": "身份证材料",
        "confidence": 0.92,
    }


def detect_objects(request: ImageTaskRequest) -> dict:
    """返回模拟异常检测结果。"""
    return {
        "business_id": request.business_id,
        "file_url": request.file_url,
        "objects": [
            {"label": "照片区域", "confidence": 0.95, "bbox": [120, 80, 260, 220]},
            {"label": "疑似遮挡", "confidence": 0.71, "bbox": [300, 140, 360, 190]},
        ],
    }


def segment_image(request: ImageTaskRequest) -> dict:
    """返回模拟图像分割结果。"""
    return {
        "business_id": request.business_id,
        "file_url": request.file_url,
        "segments": [
            {"label": "姓名区域", "mask_url": "/mock/masks/name.png"},
            {"label": "证件号区域", "mask_url": "/mock/masks/id-card.png"},
        ],
    }


def answer_question(request: ChatRequest) -> dict:
    """返回模拟考籍业务问答结果。"""
    return {"answer": f"已收到问题：{request.content}。后续将接入正式大模型服务。"}


def recognize_speech(request: SpeechRequest) -> dict:
    """返回模拟语音识别结果。"""
    return {"audio_url": request.audio_url, "text": "查询考生考籍档案"}


def synthesize_speech(request: ChatRequest) -> dict:
    """返回模拟语音合成结果。"""
    return {"text": request.content, "audio_url": "/mock/audio/tts-result.mp3"}

