"""YOLO 材料图像分割推理服务。"""

from __future__ import annotations

import os
from hashlib import md5
from pathlib import Path
from threading import Lock
from typing import Any

import numpy as np
from PIL import Image, ImageDraw

from app.models.schemas import MaterialSegment, ObjectBoundingBox, SegmentationPoint
from app.services.image_analysis_service import ImageAnalysis, estimate_visual_confidence, resolve_image_path
from app.services.yolo_classify_service import MATERIAL_CATEGORY_NAMES


SERVICE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SEGMENT_MODEL_PATH = SERVICE_ROOT / "app" / "models" / "weights" / "material-seg.pt"
MASK_OUTPUT_DIR = SERVICE_ROOT / "uploads" / "masks"
SEGMENT_OUTPUT_DIR = SERVICE_ROOT / "uploads" / "segments"
DEFAULT_CONFIDENCE_THRESHOLD = 0.4
DEFAULT_IMAGE_SIZE = 640

SEGMENT_CLASS_TYPE_MAPPING = {
    "ADMISSION_TICKET": "DOCUMENT",
    "DIPLOMA": "DOCUMENT",
    "EXEMPTION_CERTIFICATE": "DOCUMENT",
    "PHOTO": "PHOTO",
    "TRANSCRIPT": "DOCUMENT",
    "ID_CARD": "DOCUMENT",
}

_MODEL_LOCK = Lock()
_SEGMENT_MODEL: Any | None = None
_SEGMENT_MODEL_PATH: Path | None = None
_LOAD_FAILED = False


def build_yolo_material_segments(
        file_url: str,
        analysis: ImageAnalysis | None = None) -> tuple[str, str, list[MaterialSegment], str | None] | None:
    """@brief 使用 YOLO 分割模型生成材料主体区域。

    @param file_url 图片文件地址，支持本地路径、上传访问路径和静态 URL 路径。
    @param analysis 图片基础视觉分析结果，用于读取图片尺寸和修正置信度。
    @return 推理成功且存在掩码时返回材料类别编码、类别名称和分割区域列表；模型不可用或图片不可访问时返回 None。
    """
    image_path = resolve_image_path(file_url)
    if image_path is None:
        return None

    model = _get_segment_model()
    if model is None:
        return None

    device = os.environ.get("YOLO_DEVICE", "cpu")
    image_size = int(os.environ.get("YOLO_SEGMENT_IMAGE_SIZE", os.environ.get("YOLO_IMAGE_SIZE", str(DEFAULT_IMAGE_SIZE))))
    confidence = float(os.environ.get("YOLO_SEGMENT_CONFIDENCE", str(DEFAULT_CONFIDENCE_THRESHOLD)))
    results = model.predict(source=str(image_path), imgsz=image_size, conf=confidence, device=device, verbose=False)
    if not results:
        return None

    segments = _convert_result_to_segments(results[0], file_url, analysis)
    if not segments:
        return None

    category_code = _pick_category_code(segments)
    category_name = MATERIAL_CATEGORY_NAMES.get(category_code, category_code)
    segmentation_image_url = write_yolo_plot_image(file_url, results[0])
    return category_code, category_name, segments, segmentation_image_url


def _get_segment_model() -> Any | None:
    """@brief 懒加载并缓存 YOLO 分割模型。

    @return 已加载的 Ultralytics YOLO 模型；依赖缺失、权重缺失或加载失败时返回 None。
    """
    global _SEGMENT_MODEL, _SEGMENT_MODEL_PATH, _LOAD_FAILED
    model_path = Path(os.environ.get("YOLO_SEGMENT_MODEL", str(DEFAULT_SEGMENT_MODEL_PATH))).expanduser()
    if not model_path.is_absolute():
        model_path = (SERVICE_ROOT / model_path).resolve()

    if _LOAD_FAILED and _SEGMENT_MODEL_PATH == model_path:
        return None
    if _SEGMENT_MODEL is not None and _SEGMENT_MODEL_PATH == model_path:
        return _SEGMENT_MODEL
    if not model_path.exists():
        _LOAD_FAILED = True
        _SEGMENT_MODEL_PATH = model_path
        return None

    with _MODEL_LOCK:
        if _SEGMENT_MODEL is not None and _SEGMENT_MODEL_PATH == model_path:
            return _SEGMENT_MODEL
        try:
            from ultralytics import YOLO

            _SEGMENT_MODEL = YOLO(str(model_path))
            _SEGMENT_MODEL_PATH = model_path
            _LOAD_FAILED = False
            return _SEGMENT_MODEL
        except Exception:
            _SEGMENT_MODEL = None
            _SEGMENT_MODEL_PATH = model_path
            _LOAD_FAILED = True
            return None


def _convert_result_to_segments(result: Any, file_url: str, analysis: ImageAnalysis | None) -> list[MaterialSegment]:
    """@brief 将 Ultralytics 单张图片分割结果转换为接口分割区域。

    @param result Ultralytics 单张图片预测结果。
    @param analysis 图片基础视觉分析结果。
    @return 按面积占比倒序排列的分割区域列表。
    """
    masks = getattr(result, "masks", None)
    boxes = getattr(result, "boxes", None)
    names = getattr(result, "names", {}) or {}
    if masks is None or boxes is None:
        return []

    polygons = _read_mask_polygons(masks)
    xyxy_boxes = _tensor_to_list(getattr(boxes, "xyxy", []))
    class_indexes = _tensor_to_list(getattr(boxes, "cls", []))
    confidences = _tensor_to_list(getattr(boxes, "conf", []))
    image_height, image_width = _read_image_shape(result, analysis)
    image_area = max(1, image_width * image_height)

    segments: list[MaterialSegment] = []
    for index, (box, class_index, raw_confidence) in enumerate(zip(xyxy_boxes, class_indexes, confidences), start=1):
        if len(box) < 4:
            continue
        category_code = str(names.get(int(class_index), int(class_index)))
        bbox = _box_to_bbox(box, image_width, image_height)
        polygon = _normalize_polygon(polygons[index - 1] if index - 1 < len(polygons) else [], bbox, image_width, image_height)
        confidence = float(raw_confidence)
        if analysis is not None:
            confidence = estimate_visual_confidence(confidence, analysis)
        mask_url = write_polygon_mask_image(file_url, category_code, index, polygon, image_width, image_height)
        segments.append(
            MaterialSegment(
                segment_code=f"{category_code}_SEGMENT_{index}",
                segment_name=f"{MATERIAL_CATEGORY_NAMES.get(category_code, category_code)}分割区域",
                segment_type=SEGMENT_CLASS_TYPE_MAPPING.get(category_code, "DOCUMENT"),
                confidence=round(max(0.0, min(1.0, confidence)), 4),
                bbox=bbox,
                polygon=polygon,
                mask_url=mask_url,
                area_ratio=round((bbox.width * bbox.height) / image_area, 4),
                extraction_priority=index,
                need_manual_review=confidence < 0.6,
                remark="由训练后的 YOLO 分割模型识别生成",
            )
        )
    return sorted(segments, key=lambda item: item.area_ratio, reverse=True)


def _read_mask_polygons(masks: Any) -> list[list[Any]]:
    """@brief 读取 YOLO 掩码轮廓点。"""
    xy = getattr(masks, "xy", None)
    if xy is None:
        return []
    return list(xy)


def _read_image_shape(result: Any, analysis: ImageAnalysis | None) -> tuple[int, int]:
    """@brief 读取预测结果对应的图片高宽。"""
    if analysis is not None and analysis.width > 0 and analysis.height > 0:
        return analysis.height, analysis.width
    original_shape = getattr(result, "orig_shape", None)
    if original_shape and len(original_shape) >= 2:
        return int(original_shape[0]), int(original_shape[1])
    return 800, 600


def _box_to_bbox(box: list[float], image_width: int, image_height: int) -> ObjectBoundingBox:
    """@brief 将 xyxy 边界框转换为接口使用的矩形结构。"""
    left = max(0, min(int(round(box[0])), max(0, image_width - 1)))
    top = max(0, min(int(round(box[1])), max(0, image_height - 1)))
    right = max(left + 1, min(int(round(box[2])), image_width))
    bottom = max(top + 1, min(int(round(box[3])), image_height))
    return ObjectBoundingBox(x=left, y=top, width=right - left, height=bottom - top)


def _normalize_polygon(
        raw_polygon: list[Any],
        bbox: ObjectBoundingBox,
        image_width: int,
        image_height: int) -> list[SegmentationPoint]:
    """@brief 将模型轮廓点归一化为整数坐标；缺失时退化为矩形轮廓。"""
    points: list[SegmentationPoint] = []
    for point in raw_polygon:
        if len(point) < 2:
            continue
        x = max(0, min(int(round(float(point[0]))), image_width))
        y = max(0, min(int(round(float(point[1]))), image_height))
        points.append(SegmentationPoint(x=x, y=y))
    if points:
        return points

    right = bbox.x + bbox.width
    bottom = bbox.y + bbox.height
    return [
        SegmentationPoint(x=bbox.x, y=bbox.y),
        SegmentationPoint(x=right, y=bbox.y),
        SegmentationPoint(x=right, y=bottom),
        SegmentationPoint(x=bbox.x, y=bottom),
    ]


def _pick_category_code(segments: list[MaterialSegment]) -> str:
    """@brief 从分割结果中选择面积最大的类别作为材料类别。"""
    if not segments:
        return "UNKNOWN"
    best_segment = max(segments, key=lambda item: item.confidence)
    return best_segment.segment_code.rsplit("_SEGMENT_", 1)[0]


def write_polygon_mask_image(
        file_url: str,
        category_code: str,
        index: int,
        polygon: list[SegmentationPoint],
        image_width: int,
        image_height: int) -> str:
    """@brief 将分割轮廓写出为可访问的 PNG 掩码文件。

    @param file_url 原始图片地址。
    @param category_code 模型识别到的材料类别编码。
    @param index 当前分割区域序号。
    @param polygon 分割轮廓点。
    @param image_width 原图宽度。
    @param image_height 原图高度。
    @return 算法服务静态资源访问路径。
    """
    MASK_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    hash_source = "|".join(
        [
            file_url,
            category_code,
            str(index),
            str(image_width),
            str(image_height),
            ";".join(f"{point.x},{point.y}" for point in polygon),
        ]
    )
    filename = f"yolo-{category_code.lower()}-{md5(hash_source.encode('utf-8')).hexdigest()[:16]}.png"
    target_path = MASK_OUTPUT_DIR / filename
    if not target_path.exists():
        mask = Image.new("L", (image_width, image_height), 0)
        draw = ImageDraw.Draw(mask)
        draw.polygon([(point.x, point.y) for point in polygon], fill=255)
        mask.save(target_path)
    return f"/generated/masks/{filename}"


def write_segmentation_preview_image(
        file_url: str,
        segments: list[MaterialSegment],
        analysis: ImageAnalysis | None = None) -> str | None:
    """@brief 生成带彩色蒙版和轮廓的图像分割可视化结果图。

    @param file_url 原始图片地址。
    @param segments 分割区域列表。
    @param analysis 图片基础视觉分析结果。
    @return 可访问的分割结果图地址；原图不可读取或无分割区域时返回 None。
    """
    if not segments:
        return None

    image_path = resolve_image_path(file_url)
    if image_path is None:
        return None

    SEGMENT_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    hash_source = "|".join(
        [
            file_url,
            str(analysis.width if analysis else ""),
            str(analysis.height if analysis else ""),
            ";".join(
                f"{segment.segment_code}:{','.join(f'{point.x}-{point.y}' for point in segment.polygon)}"
                for segment in segments
            ),
        ]
    )
    filename = f"segmentation-{md5(hash_source.encode('utf-8')).hexdigest()[:16]}.png"
    target_path = SEGMENT_OUTPUT_DIR / filename
    if target_path.exists():
        return f"/generated/segments/{filename}"

    with Image.open(image_path) as source:
        base = source.convert("RGBA")
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    palette = [
        (56, 189, 248, 92),
        (168, 85, 247, 92),
        (248, 113, 113, 92),
        (251, 191, 36, 92),
        (52, 211, 153, 92),
        (96, 165, 250, 92),
    ]
    outline_palette = [
        (2, 132, 199, 255),
        (126, 34, 206, 255),
        (220, 38, 38, 255),
        (217, 119, 6, 255),
        (5, 150, 105, 255),
        (37, 99, 235, 255),
    ]
    for index, segment in enumerate(segments):
        points = [(point.x, point.y) for point in segment.polygon]
        if len(points) < 3:
            continue
        fill = palette[index % len(palette)]
        outline = outline_palette[index % len(outline_palette)]
        draw.polygon(points, fill=fill)
        draw.line(points + [points[0]], fill=outline, width=4)

    result = Image.alpha_composite(base, overlay).convert("RGB")
    result.save(target_path, quality=95)
    return f"/generated/segments/{filename}"


def write_yolo_plot_image(file_url: str, result: Any) -> str | None:
    """@brief 使用 Ultralytics 原生绘制结果生成分割预览图。

    @param file_url 原始图片地址，用于生成稳定文件名。
    @param result Ultralytics 单张图片预测结果。
    @return 可访问的分割结果图地址；绘制失败时返回 None。
    """
    boxes = getattr(result, "boxes", None)
    masks = getattr(result, "masks", None)
    if boxes is None or masks is None:
        return None

    try:
        xyxy_boxes = _tensor_to_list(getattr(boxes, "xyxy", []))
        class_indexes = _tensor_to_list(getattr(boxes, "cls", []))
        confidences = _tensor_to_list(getattr(boxes, "conf", []))
        hash_source = "|".join(
            [
                file_url,
                ";".join(
                    f"{cls}:{conf}:{','.join(str(round(float(value), 2)) for value in box)}"
                    for box, cls, conf in zip(xyxy_boxes, class_indexes, confidences)
                ),
            ]
        )
        filename = f"segmentation-{md5(hash_source.encode('utf-8')).hexdigest()[:16]}.jpg"
        SEGMENT_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        target_path = SEGMENT_OUTPUT_DIR / filename
        if target_path.exists():
            return f"/generated/segments/{filename}"

        plotted = result.plot()
        image = Image.fromarray(np.asarray(plotted)[..., ::-1])
        image.save(target_path, quality=95)
        return f"/generated/segments/{filename}"
    except Exception:
        return None


def _tensor_to_list(value: Any) -> list[Any]:
    """@brief 将 PyTorch Tensor、NumPy 数组或普通序列转换为 Python 列表。"""
    if hasattr(value, "cpu"):
        value = value.cpu()
    if hasattr(value, "tolist"):
        return value.tolist()
    return list(value)
