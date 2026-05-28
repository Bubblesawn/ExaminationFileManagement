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
