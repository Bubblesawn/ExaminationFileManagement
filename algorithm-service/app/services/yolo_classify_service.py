"""YOLO 材料图像分类推理服务。"""

from __future__ import annotations

import os
from pathlib import Path
from threading import Lock
from typing import Any

from app.models.schemas import ImageTaskRequest, MaterialCategoryCandidate
from app.services.image_analysis_service import ImageAnalysis, estimate_visual_confidence, resolve_image_path


SERVICE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CLASSIFY_MODEL_PATH = SERVICE_ROOT / "app" / "models" / "weights" / "material-cls.pt"
DEFAULT_TOP_K = 3

MATERIAL_CATEGORY_NAMES = {
    "ID_CARD": "身份证材料",
    "ADMISSION_TICKET": "准考证材料",
    "DIPLOMA": "学历证书材料",
    "TRANSCRIPT": "成绩单材料",
    "EXEMPTION_CERTIFICATE": "免考证明材料",
    "PHOTO": "考生照片",
}

_MODEL_LOCK = Lock()
_CLASSIFY_MODEL: Any | None = None
_CLASSIFY_MODEL_PATH: Path | None = None
_LOAD_FAILED = False


def build_yolo_material_candidates(
        request: ImageTaskRequest,
        analysis: ImageAnalysis | None = None) -> list[MaterialCategoryCandidate] | None:
    """@brief 使用 YOLO 分类模型生成材料类别候选项。

    @param request 图片算法任务请求。
    @param analysis 图片视觉分析结果，用于质量置信度修正。
    @return 模型可用且推理成功时返回候选类别；模型不可用时返回 None 交由规则兜底。
    """
    image_path = resolve_image_path(request.file_url)
    if image_path is None:
        return None

    model = _get_classify_model()
    if model is None:
        return None

    device = os.environ.get("YOLO_DEVICE", "cpu")
    image_size = int(os.environ.get("YOLO_CLASSIFY_IMAGE_SIZE", os.environ.get("YOLO_IMAGE_SIZE", "224")))
    results = model.predict(source=str(image_path), imgsz=image_size, device=device, verbose=False)
    if not results:
        return None

    candidates = _convert_probs_to_candidates(results[0], analysis)
    return candidates or None


def _get_classify_model() -> Any | None:
    """@brief 懒加载并缓存 YOLO 分类模型。

    @return 已加载的 Ultralytics YOLO 模型；依赖缺失、权重缺失或加载失败时返回 None。
    """
    global _CLASSIFY_MODEL, _CLASSIFY_MODEL_PATH, _LOAD_FAILED
    model_path = Path(os.environ.get("YOLO_CLASSIFY_MODEL", str(DEFAULT_CLASSIFY_MODEL_PATH))).expanduser()
    if not model_path.is_absolute():
        model_path = (SERVICE_ROOT / model_path).resolve()

    if _LOAD_FAILED and _CLASSIFY_MODEL_PATH == model_path:
        return None
    if _CLASSIFY_MODEL is not None and _CLASSIFY_MODEL_PATH == model_path:
        return _CLASSIFY_MODEL
    if not model_path.exists():
        _LOAD_FAILED = True
        _CLASSIFY_MODEL_PATH = model_path
        return None

    with _MODEL_LOCK:
        if _CLASSIFY_MODEL is not None and _CLASSIFY_MODEL_PATH == model_path:
            return _CLASSIFY_MODEL
        try:
            from ultralytics import YOLO

            _CLASSIFY_MODEL = YOLO(str(model_path))
            _CLASSIFY_MODEL_PATH = model_path
            _LOAD_FAILED = False
            return _CLASSIFY_MODEL
        except Exception:
            _CLASSIFY_MODEL = None
            _CLASSIFY_MODEL_PATH = model_path
            _LOAD_FAILED = True
            return None


def _convert_probs_to_candidates(result: Any, analysis: ImageAnalysis | None) -> list[MaterialCategoryCandidate]:
    """@brief 将 Ultralytics 分类概率转换为接口候选类别结构。

    @param result Ultralytics 单张图片分类结果。
    @param analysis 图片视觉分析结果。
    @return 按置信度倒序排列的材料类别候选项。
    """
    probs = getattr(result, "probs", None)
    names = getattr(result, "names", {}) or {}
    if probs is None:
        return []

    top_indexes = _tensor_to_list(getattr(probs, "top5", []))[:DEFAULT_TOP_K]
    top_confidences = _tensor_to_list(getattr(probs, "top5conf", []))[:DEFAULT_TOP_K]
    candidates: list[MaterialCategoryCandidate] = []
    for index, raw_confidence in zip(top_indexes, top_confidences):
        category_code = str(names.get(int(index), index))
        category_name = MATERIAL_CATEGORY_NAMES.get(category_code, category_code)
        confidence = float(raw_confidence)
        if analysis is not None:
            confidence = estimate_visual_confidence(confidence, analysis)
        candidates.append(
            MaterialCategoryCandidate(
                category_code=category_code,
                category_name=category_name,
                confidence=round(max(0.0, min(1.0, confidence)), 4),
            )
        )
    return sorted(candidates, key=lambda item: item.confidence, reverse=True)


def _tensor_to_list(value: Any) -> list[Any]:
    """@brief 将 PyTorch Tensor、NumPy 数组或普通序列转换为 Python 列表。"""
    if hasattr(value, "cpu"):
        value = value.cpu()
    if hasattr(value, "tolist"):
        return value.tolist()
    return list(value)
