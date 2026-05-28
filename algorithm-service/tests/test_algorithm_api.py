from fastapi.testclient import TestClient

from app.api.security import DEFAULT_ALGORITHM_API_KEY
from app.main import app

client = TestClient(app)
headers = {"X-Internal-Api-Key": DEFAULT_ALGORITHM_API_KEY}


def test_health_without_api_key() -> None:
    """@brief 健康检查不要求内部密钥。"""
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_business_api_requires_internal_api_key() -> None:
    """@brief 算法业务接口未携带内部密钥时返回 401。"""
    response = client.post("/api/chat", json={"content": "免考申请需要哪些材料"})

    assert response.status_code == 401
    assert response.json()["detail"] == "算法服务认证失败"


def test_chat_returns_unified_response() -> None:
    """@brief 智能问答接口返回统一响应结构。"""
    response = client.post(
        "/api/chat",
        headers=headers,
        json={"content": "免考申请需要哪些材料", "scene": "EXEMPTION"},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["code"] == 200
    assert body["message"] == "操作成功"
    assert body["data"]["answer"]
    assert isinstance(body["data"]["references"], list)
    assert isinstance(body["data"]["suggestions"], list)


def test_chat_keeps_low_confidence_deepseek_answer(monkeypatch) -> None:
    """@brief DeepSeek 返回低置信度回答时仍展示模型回答，并标记人工复核。"""

    def fake_answer_with_deepseek(question, scene, references, matched_intent):
        return {
            "intent_code": "GENERAL_CONSULT",
            "intent_name": "通用考籍咨询",
            "answer": "问题内容不够明确，请补充具体办理事项或材料类型后再咨询。",
            "confidence": 0.1,
            "suggestions": ["补充具体问题"],
            "need_manual_review": True,
        }

    monkeypatch.setattr(
        "app.services.mock_algorithm_service.answer_with_deepseek",
        fake_answer_with_deepseek,
    )
    response = client.post(
        "/api/chat",
        headers=headers,
        json={"content": "？？？", "scene": "MATERIAL_AUDIT"},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["code"] == 200
    assert body["data"]["answer"] == "问题内容不够明确，请补充具体办理事项或材料类型后再咨询。"
    assert body["data"]["confidence"] == 0.1
    assert body["data"]["need_manual_review"] is True


def test_material_preprocess_rejects_negative_file_size() -> None:
    """@brief 材料预处理接口对负数文件大小返回参数校验错误。"""
    response = client.post(
        "/api/material-preprocess",
        headers=headers,
        json={"file_url": "/uploads/materials/id-card.jpg", "file_size_kb": -1},
    )

    assert response.status_code == 422


def test_application_material_audit_empty_materials_is_controlled_response() -> None:
    """@brief 申请材料为空时返回可控缺失材料提示。"""
    response = client.post(
        "/api/application-material-audit",
        headers=headers,
        json={"application_type": "EXEMPTION", "materials": []},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["code"] == 200
    assert body["data"]["suggested_action"] == "REJECT"
    assert body["data"]["missing_materials"]
