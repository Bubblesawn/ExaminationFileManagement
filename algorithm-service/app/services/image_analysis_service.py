import os
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import unquote, urlparse

from PIL import Image, ImageChops, ImageFilter, ImageStat


@dataclass(frozen=True)
class BoundingBox:
    """@brief 图像区域边界框。"""

    x: int
    y: int
    width: int
    height: int


@dataclass(frozen=True)
class ImageAnalysis:
    """@brief 材料图片基础视觉分析结果。"""

    loaded: bool
    width: int
    height: int
    issues: tuple[str, ...]
    brightness: float
    contrast: float
    edge_score: float
    document_bbox: BoundingBox | None
    source_path: str | None = None


SUPPORTED_IMAGE_SUFFIXES = (".jpg", ".jpeg", ".png", ".bmp", ".webp")
MIN_READABLE_WIDTH = 480
MIN_READABLE_HEIGHT = 480


def analyze_image(file_url: str) -> ImageAnalysis:
    """@brief 读取真实图片并计算质量、清晰度和主体边界。

    @param file_url 前端或后端传入的图片访问地址。
    @return 图片基础视觉分析结果；读取失败时返回不可读结果。
    """
    issues: list[str] = []
    if not file_url.lower().endswith(SUPPORTED_IMAGE_SUFFIXES):
        issues.append("UNSUPPORTED_IMAGE_FORMAT")

    image_path = resolve_image_path(file_url)
    if image_path is None:
        issues.append("IMAGE_NOT_ACCESSIBLE")
        return ImageAnalysis(False, 0, 0, tuple(issues), 0, 0, 0, None)

    try:
        with Image.open(image_path) as image:
            image = image.convert("RGB")
            width, height = image.size
            sample = image.copy()
            sample.thumbnail((480, 480))
            gray = sample.convert("L")

            stat = ImageStat.Stat(gray)
            brightness = round(stat.mean[0] / 255, 4)
            contrast = round(stat.stddev[0] / 128, 4)
            edge_score = round(ImageStat.Stat(gray.filter(ImageFilter.FIND_EDGES)).mean[0] / 255, 4)
            document_bbox = _detect_document_bbox(gray, width, height)

            if width < MIN_READABLE_WIDTH or height < MIN_READABLE_HEIGHT:
                issues.append("LOW_RESOLUTION")
            if brightness < 0.18:
                issues.append("LOW_LIGHT")
            if brightness > 0.9:
                issues.append("OVER_EXPOSURE")
            if contrast < 0.12:
                issues.append("LOW_CONTRAST")
            if edge_score < 0.018:
                issues.append("BLUR_RISK")

            return ImageAnalysis(
                True,
                width,
                height,
                tuple(issues),
                brightness,
                contrast,
                edge_score,
                document_bbox,
                str(image_path),
            )
    except OSError:
        issues.append("IMAGE_OPEN_FAILED")
        return ImageAnalysis(False, 0, 0, tuple(issues), 0, 0, 0, None, str(image_path))


def resolve_image_path(file_url: str) -> Path | None:
    """@brief 将上传访问地址解析为算法服务可读取的本地文件路径。

    @param file_url 图片相对地址、绝对路径或本地服务 URL。
    @return 存在的本地图片路径；无法解析时返回 None。
    """
    parsed = urlparse(file_url)
    raw_path = unquote(parsed.path if parsed.scheme else file_url).replace("\\", "/")
    candidate_path = Path(raw_path)
    if candidate_path.is_absolute() and candidate_path.exists():
        return candidate_path

    material_relative_path = _extract_material_relative_path(raw_path)
    if material_relative_path is None:
        return None

    for root in _candidate_upload_roots():
        candidate = root.joinpath(material_relative_path).resolve()
        if candidate.exists() and candidate.is_file():
            return candidate
    return None


def scale_template_bbox(template: tuple[int, int, int, int], analysis: ImageAnalysis) -> BoundingBox:
    """@brief 将 600x800 模板区域映射到真实图片主体区域。

    @param template 模板坐标，顺序为 x、y、width、height。
    @param analysis 图片视觉分析结果。
    @return 映射到真实图片尺寸后的边界框。
    """
    base = analysis.document_bbox or BoundingBox(0, 0, analysis.width or 600, analysis.height or 800)
    x, y, width, height = template
    return _clamp_bbox(
        BoundingBox(
            base.x + round(x / 600 * base.width),
            base.y + round(y / 800 * base.height),
            max(1, round(width / 600 * base.width)),
            max(1, round(height / 800 * base.height)),
        ),
        analysis.width or 600,
        analysis.height or 800,
    )


def relative_bbox(left: float, top: float, width: float, height: float, analysis: ImageAnalysis) -> BoundingBox:
    """@brief 根据真实图片主体区域生成相对边界框。

    @param left 区域左侧相对位置。
    @param top 区域顶部相对位置。
    @param width 区域相对宽度。
    @param height 区域相对高度。
    @param analysis 图片视觉分析结果。
    @return 映射到真实图片尺寸后的边界框。
    """
    base = analysis.document_bbox or BoundingBox(0, 0, analysis.width or 600, analysis.height or 800)
    return _clamp_bbox(
        BoundingBox(
            base.x + round(left * base.width),
            base.y + round(top * base.height),
            max(1, round(width * base.width)),
            max(1, round(height * base.height)),
        ),
        analysis.width or 600,
        analysis.height or 800,
    )


def estimate_visual_confidence(base_confidence: float, analysis: ImageAnalysis) -> float:
    """@brief 根据真实图片质量修正算法置信度。

    @param base_confidence 模板或规则给出的基础置信度。
    @param analysis 图片视觉分析结果。
    @return 修正后的 0 到 1 置信度。
    """
    if not analysis.loaded:
        return min(base_confidence, 0.55)

    penalty = 0.0
    penalty += 0.08 * sum(issue in analysis.issues for issue in ("LOW_RESOLUTION", "LOW_LIGHT", "OVER_EXPOSURE"))
    penalty += 0.12 * sum(issue in analysis.issues for issue in ("LOW_CONTRAST", "BLUR_RISK"))
    return round(max(0.2, min(0.98, base_confidence - penalty)), 2)


def looks_like_certificate_photo(analysis: ImageAnalysis) -> bool:
    """@brief 判断图片是否更像单独的考生证件照。

    @param analysis 图片视觉分析结果。
    @return 如果图片纵横比和主体边界更符合证件照则返回 True。
    """
    if not analysis.loaded or analysis.width == 0 or analysis.height == 0:
        return False
    ratio = analysis.width / analysis.height
    has_full_document = analysis.document_bbox is not None and analysis.document_bbox.width > analysis.width * 0.82
    return 0.62 <= ratio <= 0.86 and not has_full_document


def _extract_material_relative_path(raw_path: str) -> Path | None:
    """@brief 从上传 URL 中提取材料目录下的相对路径。"""
    marker = "/uploads/materials/"
    normalized = "/" + raw_path.lstrip("/")
    if marker not in normalized:
        return None
    return Path(normalized.split(marker, 1)[1])


def _candidate_upload_roots() -> list[Path]:
    """@brief 生成可能的材料上传根目录列表。"""
    service_root = Path(__file__).resolve().parents[2]
    project_root = service_root.parent
    roots = [
        os.environ.get("MATERIAL_UPLOAD_ROOT"),
        project_root / "backend" / "uploads" / "materials",
        project_root / "uploads" / "materials",
        service_root / "uploads" / "materials",
    ]
    return [Path(root).expanduser().resolve() for root in roots if root]


def _detect_document_bbox(gray: Image.Image, original_width: int, original_height: int) -> BoundingBox | None:
    """@brief 基于背景差异检测材料主体边界。"""
    width, height = gray.size
    corners = [
        gray.crop((0, 0, max(1, width // 8), max(1, height // 8))),
        gray.crop((max(0, width - width // 8), 0, width, max(1, height // 8))),
        gray.crop((0, max(0, height - height // 8), max(1, width // 8), height)),
        gray.crop((max(0, width - width // 8), max(0, height - height // 8), width, height)),
    ]
    background = int(sum(ImageStat.Stat(corner).mean[0] for corner in corners) / len(corners))
    diff = ImageChops.difference(gray, Image.new("L", gray.size, background))
    mask = diff.point(lambda pixel: 255 if pixel > 18 else 0)
    bbox = mask.getbbox()
    if bbox is None:
        return None

    left, top, right, bottom = bbox
    if (right - left) * (bottom - top) < width * height * 0.12:
        return None

    scale_x = original_width / width
    scale_y = original_height / height
    return _clamp_bbox(
        BoundingBox(
            round(left * scale_x),
            round(top * scale_y),
            round((right - left) * scale_x),
            round((bottom - top) * scale_y),
        ),
        original_width,
        original_height,
    )


def _clamp_bbox(bbox: BoundingBox, image_width: int, image_height: int) -> BoundingBox:
    """@brief 将边界框限制在图片范围内。"""
    x = max(0, min(bbox.x, max(0, image_width - 1)))
    y = max(0, min(bbox.y, max(0, image_height - 1)))
    width = max(1, min(bbox.width, max(1, image_width - x)))
    height = max(1, min(bbox.height, max(1, image_height - y)))
    return BoundingBox(x, y, width, height)
