from pydantic import BaseModel, Field


class ImageTaskRequest(BaseModel):
    """图片算法任务请求。"""

    file_url: str = Field(..., description="图片文件地址")
    business_id: int | None = Field(default=None, description="业务ID")


class ChatRequest(BaseModel):
    """智能问答请求。"""

    content: str = Field(..., description="问题或待播报文本")


class SpeechRequest(BaseModel):
    """语音识别请求。"""

    audio_url: str = Field(..., description="音频文件地址")

