"""YOLO 分割数据集转目标检测数据集。"""

from __future__ import annotations

import argparse
import json
import shutil
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

import yaml  # type: ignore

SERVICE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT_DIR = SERVICE_ROOT / "dataset" / "detection_material_dataset"
DEFAULT_IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


@dataclass(frozen=True)
class ConversionSummary:
    """@brief 分割转检测后的数据集统计信息。"""

    source_dataset: str
    output_dataset: str
    classes: list[str]
    train_images: int
    val_images: int
    train_labels: int
    val_labels: int
    total_objects: int
    warnings: list[str]


def parse_args() -> argparse.Namespace:
    """@brief 解析命令行参数。

    @return argparse 解析后的命名空间。
    """
    parser = argparse.ArgumentParser(description="将 YOLO 分割数据集转换为 YOLO 目标检测数据集")
    parser.add_argument(
        "--source",
        required=True,
        help="分割数据集根目录，需包含 images 和 labels 子目录",
    )
    parser.add_argument(
        "--output",
        default=str(DEFAULT_OUTPUT_DIR),
        help="检测数据集输出目录",
    )
    parser.add_argument(
        "--copy-images",
        action="store_true",
        help="复制图片到输出目录；默认仅生成 labels 和 data.yaml",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="仅检查和统计，不写入输出目录",
    )
    return parser.parse_args()


def main() -> None:
    """@brief 转换脚本主入口。"""
    args = parse_args()
    source_dir = Path(args.source).expanduser().resolve()
    output_dir = Path(args.output).expanduser().resolve()
    summary = convert_seg_dataset(source_dir, output_dir, copy_images=args.copy_images, dry_run=args.dry_run)
    print(json.dumps(asdict(summary), ensure_ascii=False, indent=2))


def convert_seg_dataset(source_dir: Path, output_dir: Path, copy_images: bool, dry_run: bool) -> ConversionSummary:
    """@brief 将 YOLO 分割数据集转换为 YOLO 检测数据集。

    @param source_dir YOLO 分割数据集根目录。
    @param output_dir YOLO 检测数据集输出目录。
    @param copy_images 是否复制图片文件。
    @param dry_run 是否仅执行校验和统计。
    @return 转换统计摘要。
    """
    images_root = source_dir / "images"
    labels_root = source_dir / "labels"
    if not images_root.is_dir():
        raise FileNotFoundError(f"未找到 images 目录：{images_root}")
    if not labels_root.is_dir():
        raise FileNotFoundError(f"未找到 labels 目录：{labels_root}")

    data_yaml = _load_data_yaml(source_dir / "data.yaml", source_dir / "data.yml")
    class_names = _extract_class_names(data_yaml)
    if not class_names:
        raise ValueError("未能从 data.yaml 读取到类别名称")

    warnings: list[str] = []
    if dry_run:
        train_stats = _scan_split(images_root / "train", labels_root / "train")
        val_stats = _scan_split(images_root / "val", labels_root / "val")
        warnings.extend(train_stats[2])
        warnings.extend(val_stats[2])
        return ConversionSummary(
            source_dataset=str(source_dir),
            output_dataset=str(output_dir),
            classes=class_names,
            train_images=train_stats[0],
            val_images=val_stats[0],
            train_labels=train_stats[1],
            val_labels=val_stats[1],
            total_objects=train_stats[3] + val_stats[3],
            warnings=warnings,
        )

    output_dir.mkdir(parents=True, exist_ok=True)
    total_objects = 0
    train_images, train_labels = _convert_split(
        images_root / "train",
        labels_root / "train",
        output_dir / "images" / "train",
        output_dir / "labels" / "train",
        copy_images=copy_images,
        warnings=warnings,
    )
    val_images, val_labels = _convert_split(
        images_root / "val",
        labels_root / "val",
        output_dir / "images" / "val",
        output_dir / "labels" / "val",
        copy_images=copy_images,
        warnings=warnings,
    )
    total_objects += train_labels[1] + val_labels[1]

    _write_detection_data_yaml(output_dir / "data.yaml", source_dir, class_names)

    return ConversionSummary(
        source_dataset=str(source_dir),
        output_dataset=str(output_dir),
        classes=class_names,
        train_images=train_images,
        val_images=val_images,
        train_labels=train_labels[0],
        val_labels=val_labels[0],
        total_objects=total_objects,
        warnings=warnings,
    )


def _convert_split(
    source_images_dir: Path,
    source_labels_dir: Path,
    output_images_dir: Path,
    output_labels_dir: Path,
    copy_images: bool,
    warnings: list[str],
) -> tuple[int, tuple[int, int]]:
    """@brief 转换单个训练/验证划分。

    @param source_images_dir 源图片目录。
    @param source_labels_dir 源分割标注目录。
    @param output_images_dir 输出图片目录。
    @param output_labels_dir 输出检测标注目录。
    @param copy_images 是否复制图片。
    @param warnings 警告列表。
    @return 图片数量和标注统计。
    """
    if not source_images_dir.is_dir():
        raise FileNotFoundError(f"未找到图片目录：{source_images_dir}")
    if not source_labels_dir.is_dir():
        raise FileNotFoundError(f"未找到标注目录：{source_labels_dir}")

    output_labels_dir.mkdir(parents=True, exist_ok=True)
    if copy_images:
        output_images_dir.mkdir(parents=True, exist_ok=True)

    image_files = sorted(
        item for item in source_images_dir.iterdir() if item.is_file() and item.suffix.lower() in DEFAULT_IMAGE_SUFFIXES
    )
    label_count = 0
    object_count = 0
    for image_path in image_files:
        label_path = source_labels_dir / f"{image_path.stem}.txt"
        if not label_path.exists():
            warnings.append(f"图片缺少对应标注：{image_path.name}")
            continue

        det_lines, object_num = _convert_label_file(label_path)
        if object_num == 0:
            warnings.append(f"标注为空，已跳过：{label_path.name}")
            continue

        with (output_labels_dir / f"{image_path.stem}.txt").open("w", encoding="utf-8") as file:
            file.write("\n".join(det_lines) + "\n")
        label_count += 1
        object_count += object_num

        if copy_images:
            shutil.copy2(image_path, output_images_dir / image_path.name)

    return len(image_files), (label_count, object_count)


def _convert_label_file(label_path: Path) -> tuple[list[str], int]:
    """@brief 将单个 YOLO 分割标注转换为 YOLO 检测标注。

    @param label_path 分割标注文件路径。
    @return 检测标注行列表及对象数量。
    """
    converted_lines: list[str] = []
    object_count = 0
    with label_path.open("r", encoding="utf-8") as file:
        for raw_line in file:
            raw_line = raw_line.strip()
            if not raw_line:
                continue
            parts = raw_line.split()
            if len(parts) < 7:
                continue
            class_id = parts[0]
            coords = [float(value) for value in parts[1:]]
            xs = coords[0::2]
            ys = coords[1::2]
            if not xs or not ys:
                continue

            x_min = max(0.0, min(xs))
            y_min = max(0.0, min(ys))
            x_max = min(1.0, max(xs))
            y_max = min(1.0, max(ys))
            if x_max <= x_min or y_max <= y_min:
                continue

            center_x = (x_min + x_max) / 2
            center_y = (y_min + y_max) / 2
            width = x_max - x_min
            height = y_max - y_min
            converted_lines.append(
                f"{class_id} {_format_float(center_x)} {_format_float(center_y)} {_format_float(width)} {_format_float(height)}"
            )
            object_count += 1

    return converted_lines, object_count


def _scan_split(images_dir: Path, labels_dir: Path) -> tuple[int, int, list[str], int]:
    """@brief 扫描划分中的图片和标注数量，用于 dry-run。

    @return 图片数量、有效标注文件数量、警告和目标总数。
    """
    if not images_dir.is_dir():
        raise FileNotFoundError(f"未找到图片目录：{images_dir}")
    if not labels_dir.is_dir():
        raise FileNotFoundError(f"未找到标注目录：{labels_dir}")

    warnings: list[str] = []
    image_count = 0
    label_count = 0
    object_count = 0
    for image_path in sorted(item for item in images_dir.iterdir() if item.is_file() and item.suffix.lower() in DEFAULT_IMAGE_SUFFIXES):
        image_count += 1
        label_path = labels_dir / f"{image_path.stem}.txt"
        if not label_path.exists():
            warnings.append(f"图片缺少对应标注：{image_path.name}")
            continue
        _, object_num = _convert_label_file(label_path)
        if object_num == 0:
            warnings.append(f"标注为空：{label_path.name}")
            continue
        label_count += 1
        object_count += object_num
    return image_count, label_count, warnings, object_count


def _load_data_yaml(*candidates: Path) -> dict[str, Any]:
    """@brief 加载数据集配置文件。"""
    for candidate in candidates:
        if candidate.exists():
            with candidate.open("r", encoding="utf-8") as file:
                return yaml.safe_load(file) or {}
    raise FileNotFoundError("未找到 data.yaml 或 data.yml")


def _extract_class_names(data_yaml: dict[str, Any]) -> list[str]:
    """@brief 从 data.yaml 提取类别名称。"""
    names = data_yaml.get("names") or {}
    if isinstance(names, dict):
        ordered_items = sorted(names.items(), key=lambda item: int(item[0]))
        return [str(name) for _, name in ordered_items]
    if isinstance(names, list):
        return [str(name) for name in names]
    return []


def _write_detection_data_yaml(output_path: Path, source_dir: Path, class_names: list[str]) -> None:
    """@brief 写入检测数据集配置文件。"""
    payload = {
        "path": str(output_path.parent).replace("\\", "/"),
        "train": "images/train",
        "val": "images/val",
        "test": "images/val",
        "nc": len(class_names),
        "names": {index: class_name for index, class_name in enumerate(class_names)},
        "source_dataset": str(source_dir).replace("\\", "/"),
    }
    with output_path.open("w", encoding="utf-8") as file:
        yaml.safe_dump(payload, file, allow_unicode=True, sort_keys=False)


def _format_float(value: float) -> str:
    """@brief 格式化归一化坐标，保留 6 位小数。"""
    return f"{value:.6f}".rstrip("0").rstrip(".")


if __name__ == "__main__":
    main()
