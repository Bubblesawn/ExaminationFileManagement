"""YOLO 材料图像目标检测推理服务。"""

from __future__ import annotations

import os
from pathlib import Path
from threading import Lock
from typing import Any

from app.models.schemas import DetectedObject, ImageTaskRequest, ObjectBoundingBox
from app.services.image_analysis_service import resolve_image_path
from app.services.yolo_classify_service import MATERIAL_CATEGORY_NAMES


SERVICE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DETECT_MODEL_PATH = SERVICE_ROOT / "app" / "models" / "weights" / "material-det.pt"
DEFAULT_DETECT_CONFIDENCE = 0.25

_MODEL_LOCK = Lock()
_DETECT_MODEL: Any | None = None
_DETECT_MODEL_PATH: Path | None = None
_LOAD_FAILED = False


def build_yolo_detected_objects(request: ImageTaskRequest) -> list[DetectedObject] | None:
    """@brief 使用 YOLO 检测模型定位材料图片中的目标区域。

    @param request 图片算法任务请求。
    @return 模型可用且推理成功时返回检测对象列表；模型不可用时返回 None 交由规则兜底。
    """
    image_path = resolve_image_path(request.file_url)
    if image_path is None:
        return None

    model = _get_detect_model()
    if model is None:
        return None

    device = os.environ.get("YOLO_DEVICE", "cpu")
    image_size = int(os.environ.get("YOLO_DETECT_IMAGE_SIZE", os.environ.get("YOLO_IMAGE_SIZE", "640")))
    confidence = float(os.environ.get("YOLO_DETECT_CONFIDENCE", str(DEFAULT_DETECT_CONFIDENCE)))
    results = model.predict(
        source=str(image_path),
        imgsz=image_size,
        conf=confidence,
        device=device,
        verbose=False,
    )
    if not results:
        return None

    objects = _convert_boxes_to_objects(results[0])
    return objects if objects else None


def _get_detect_model() -> Any | None:
    """@brief 懒加载并缓存 YOLO 检测模型。

    @return 已加载的 Ultralytics YOLO 模型；依赖缺失、权重缺失或加载失败时返回 None。
    """
    global _DETECT_MODEL, _DETECT_MODEL_PATH, _LOAD_FAILED
    model_path = Path(os.environ.get("YOLO_DETECT_MODEL", str(DEFAULT_DETECT_MODEL_PATH))).expanduser()
    if not model_path.is_absolute():
        model_path = (SERVICE_ROOT / model_path).resolve()

    if _LOAD_FAILED and _DETECT_MODEL_PATH == model_path:
        return None
    if _DETECT_MODEL is not None and _DETECT_MODEL_PATH == model_path:
        return _DETECT_MODEL
    if not model_path.exists():
        _LOAD_FAILED = True
        _DETECT_MODEL_PATH = model_path
        return None

    with _MODEL_LOCK:
        if _DETECT_MODEL is not None and _DETECT_MODEL_PATH == model_path:
            return _DETECT_MODEL
        try:
            from ultralytics import YOLO

            _DETECT_MODEL = YOLO(str(model_path))
            _DETECT_MODEL_PATH = model_path
            _LOAD_FAILED = False
            return _DETECT_MODEL
        except Exception:
            _DETECT_MODEL = None
            _DETECT_MODEL_PATH = model_path
            _LOAD_FAILED = True
            return None


def _convert_boxes_to_objects(result: Any) -> list[DetectedObject]:
    """@brief 将 Ultralytics 检测框转换为接口检测对象结构。

    @param result Ultralytics 单张图片检测结果。
    @return 按置信度倒序排列的检测对象列表。
    """
    boxes = getattr(result, "boxes", None)
    names = getattr(result, "names", {}) or {}
    if boxes is None or len(boxes) == 0:
        return []

    objects: list[DetectedObject] = []
    for index in range(len(boxes)):
        raw_box = boxes[index]
        class_index = int(_read_scalar(getattr(raw_box, "cls", 0)))
        confidence = float(_read_scalar(getattr(raw_box, "conf", 0.0)))
        object_code = str(names.get(class_index, class_index))
        x1, y1, x2, y2 = _read_xyxy(raw_box)
        bbox = ObjectBoundingBox(
            x=max(0, round(x1)),
            y=max(0, round(y1)),
            width=max(1, round(x2 - x1)),
            height=max(1, round(y2 - y1)),
        )
        objects.append(
            DetectedObject(
                object_code=object_code,
                object_name=MATERIAL_CATEGORY_NAMES.get(object_code, object_code),
                confidence=round(max(0.0, min(1.0, confidence)), 4),
                bbox=bbox,
                risk_level="LOW" if confidence >= 0.6 else "MEDIUM",
                remark="YOLO 材料检测模型识别结果",
            )
        )
    return sorted(objects, key=lambda item: item.confidence, reverse=True)


def _read_xyxy(box: Any) -> tuple[float, float, float, float]:
    """@brief 读取单个检测框的 xyxy 坐标。"""
    xyxy = getattr(box, "xyxy", [])
    values = _tensor_to_list(xyxy)
    if values and isinstance(values[0], list):
        values = values[0]
    x1, y1, x2, y2 = values[:4]
    return float(x1), float(y1), float(x2), float(y2)


def _read_scalar(value: Any) -> float:
    """@brief 从 Tensor、NumPy 数组或普通数值中读取单个浮点值。"""
    values = _tensor_to_list(value)
    if isinstance(values, list):
        return float(values[0]) if values else 0.0
    return float(values)


def _tensor_to_list(value: Any) -> Any:
    """@brief 将 PyTorch Tensor、NumPy 数组或普通序列转换为 Python 对象。"""
    if hasattr(value, "cpu"):
        value = value.cpu()
    if hasattr(value, "tolist"):
        return value.tolist()
    return value
