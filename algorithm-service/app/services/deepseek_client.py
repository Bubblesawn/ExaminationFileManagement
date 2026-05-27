import json
import os
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com"
DEFAULT_DEEPSEEK_MODEL = "deepseek-v4-flash"


def answer_with_deepseek(question: str, scene: str | None, references: list[dict], matched_intent: dict) -> dict | None:
    """@brief 调用 DeepSeek 生成考籍业务智能问答结果。

    @param question 用户提出的问题。
    @param scene 当前业务场景。
    @param references 规则库匹配出的参考依据。
    @param matched_intent 本地规则库匹配出的业务意图。
    @return 结构化问答结果；未配置或调用失败时返回 None。
    """
    _load_env_file()
    api_key = os.environ.get("DEEPSEEK_API_KEY")
    if not api_key:
        return None

    payload = {
        "model": os.environ.get("DEEPSEEK_MODEL", DEFAULT_DEEPSEEK_MODEL),
        "messages": [
            {
                "role": "system",
                "content": (
                    "你是省考试院自学考试考籍管理系统的智能助手。"
                    "只能围绕考籍档案、材料审核、免考、课程顶替、转考、毕业申请等系统业务回答。"
                    "回答要准确、简洁、可执行；不确定时提示转人工确认。"
                    "请严格输出 JSON，不要输出 Markdown。"
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "question": question,
                        "scene": scene,
                        "matched_intent": matched_intent,
                        "references": references,
                        "allowed_intent_codes": [
                            "ARCHIVE_QUERY",
                            "MATERIAL_UPLOAD",
                            "EXEMPTION_APPLY",
                            "COURSE_REPLACE",
                            "TRANSFER_PROCESS",
                            "GRADUATION_APPLY",
                            "GENERAL_CONSULT",
                        ],
                        "strict_rules": [
                            "intent_code 必须从 allowed_intent_codes 中选择，禁止输出 UNKNOWN。",
                            "如果 matched_intent 与问题明显相关，优先沿用 matched_intent 的 intent_code 和 intent_name。",
                            "answer 必须直接回答用户问题，不要说没有足够信息，除非确实超出系统业务范围。",
                        ],
                        "output_schema": {
                            "intent_code": "英文大写意图编码",
                            "intent_name": "中文意图名称",
                            "answer": "中文回答",
                            "confidence": "0到1之间的小数",
                            "suggestions": ["后续可追问或办理建议"],
                            "need_manual_review": "布尔值",
                        },
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        "stream": False,
        "temperature": 0.2,
        "max_tokens": 900,
        "response_format": {"type": "json_object"},
    }

    try:
        request = Request(
            _chat_completion_url(),
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        with urlopen(request, timeout=25) as response:
            response_payload = json.loads(response.read().decode("utf-8"))
        content = response_payload["choices"][0]["message"]["content"]
        return _normalize_answer_payload(json.loads(content))
    except (HTTPError, URLError, KeyError, json.JSONDecodeError, TimeoutError, ValueError):
        return None


def _chat_completion_url() -> str:
    """@brief 获取 DeepSeek Chat Completions 接口地址。"""
    base_url = os.environ.get("DEEPSEEK_BASE_URL", DEFAULT_DEEPSEEK_BASE_URL).rstrip("/")
    return f"{base_url}/chat/completions"


def _normalize_answer_payload(payload: dict) -> dict:
    """@brief 标准化模型返回的问答 JSON。"""
    confidence = payload.get("confidence", 0.72)
    try:
        confidence = float(confidence)
    except (TypeError, ValueError):
        confidence = 0.72
    payload["confidence"] = max(0.0, min(1.0, confidence))
    payload["suggestions"] = payload.get("suggestions") if isinstance(payload.get("suggestions"), list) else []
    payload["need_manual_review"] = bool(payload.get("need_manual_review", payload["confidence"] < 0.6))
    return payload


def _load_env_file() -> None:
    """@brief 读取算法服务本地 .env 配置，避免将密钥写入代码。"""
    env_path = Path(__file__).resolve().parents[2] / ".env"
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8-sig").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))
