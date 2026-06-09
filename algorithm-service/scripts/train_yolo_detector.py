"""YOLO 材料目标检测模型训练入口。"""

from __future__ import annotations

import argparse
import json
import shutil
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import yaml  # type: ignore

SERVICE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATASET_DIR = SERVICE_ROOT / "dataset" / "detection_material_dataset"
DEFAULT_MODEL = "yolo11n.pt"
DEFAULT_PROJECT_DIR = SERVICE_ROOT / "runs" / "detect"
DEFAULT_WEIGHTS_DIR = SERVICE_ROOT / "app" / "models" / "weights"
DEFAULT_EXPORT_PATH = DEFAULT_WEIGHTS_DIR / "material-det.pt"
DEFAULT_IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


@dataclass(frozen=True)
class DatasetSplitSummary:
    """@brief 检测数据集中单个划分的统计信息。"""

    images: int
    labels: int
    objects: int


@dataclass(frozen=True)
class DatasetValidationResult:
    """@brief 检测数据集训练前校验结果。"""

    dataset_dir: str
    classes: list[str]
    train: DatasetSplitSummary
    val: DatasetSplitSummary
    warnings: list[str]


def parse_args() -> argparse.Namespace:
    """@brief 解析 YOLO 检测训练参数。"""
    parser = argparse.ArgumentParser(description="训练考籍材料 YOLO 目标检测模型")
    parser.add_argument("--data", default=str(DEFAULT_DATASET_DIR), help="检测数据集根目录，需包含 images 和 labels 子目录")
    parser.add_argument("--model", default=DEFAULT_MODEL, help="预训练检测模型权重，例如 yolo11n.pt")
    parser.add_argument("--epochs", type=int, default=120, help="训练轮数")
    parser.add_argument("--imgsz", type=int, default=640, help="输入图片尺寸")
    parser.add_argument("--batch", type=int, default=8, help="批大小，显存不足时可降低")
    parser.add_argument("--device", default="cpu", help="训练设备，例如 cpu、0、cuda:0")
    parser.add_argument("--workers", type=int, default=0, help="数据加载线程数，Windows 本地训练建议先使用 0")
    parser.add_argument("--patience", type=int, default=30, help="早停等待轮数")
    parser.add_argument("--project", default=str(DEFAULT_PROJECT_DIR), help="训练输出目录")
    parser.add_argument("--name", default="material-det", help="本次训练名称")
    parser.add_argument("--export-path", default=str(DEFAULT_EXPORT_PATH), help="best.pt 复制后的服务权重路径")
    parser.add_argument("--dry-run", action="store_true", help="仅校验数据集和参数，不启动训练")
    return parser.parse_args()


def main() -> None:
    """@brief 检测模型训练脚本主入口。"""
    args = parse_args()
    dataset_dir = Path(args.data).resolve()
    validation = validate_dataset(dataset_dir)
    print(json.dumps(asdict(validation), ensure_ascii=False, indent=2))
    if args.dry_run:
        print("数据集校验通过，dry-run 模式未启动训练。")
        return

    summary = train_detector(args, validation)
    print(json.dumps(summary, ensure_ascii=False, indent=2))


def validate_dataset(dataset_dir: Path) -> DatasetValidationResult:
    """@brief 校验 YOLO 检测数据集目录、类别和标注格式。"""
    train_images_dir = dataset_dir / "images" / "train"
    val_images_dir = dataset_dir / "images" / "val"
    train_labels_dir = dataset_dir / "labels" / "train"
    val_labels_dir = dataset_dir / "labels" / "val"
    for path in [train_images_dir, val_images_dir, train_labels_dir, val_labels_dir]:
        if not path.is_dir():
            raise FileNotFoundError(f"目录不存在：{path}")

    data_yaml = _load_data_yaml(dataset_dir / "data.yaml", dataset_dir / "data.yml")
    classes = _extract_class_names(data_yaml)
    if not classes:
        raise ValueError("未能从 data.yaml 读取到类别名称")

    train_summary = _scan_split(train_images_dir, train_labels_dir, len(classes))
    val_summary = _scan_split(val_images_dir, val_labels_dir, len(classes))
    warnings = train_summary[3] + val_summary[3]
    if train_summary[0] == 0:
        raise ValueError("训练集图片为空")
    if val_summary[0] == 0:
        raise ValueError("验证集图片为空")

    return DatasetValidationResult(
        dataset_dir=str(dataset_dir),
        classes=classes,
        train=DatasetSplitSummary(images=train_summary[0], labels=train_summary[1], objects=train_summary[2]),
        val=DatasetSplitSummary(images=val_summary[0], labels=val_summary[1], objects=val_summary[2]),
        warnings=warnings,
    )


def train_detector(args: argparse.Namespace, validation: DatasetValidationResult) -> dict[str, Any]:
    """@brief 调用 Ultralytics YOLO Python API 训练材料目标检测模型。"""
    try:
        from ultralytics import YOLO
    except ImportError as exc:
        raise RuntimeError("未安装 ultralytics，请先执行 pip install -r requirements.txt") from exc

    model = YOLO(args.model)
    results = model.train(
        data=str(Path(args.data).resolve() / "data.yaml"),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        workers=args.workers,
        patience=args.patience,
        project=str(Path(args.project).resolve()),
        name=args.name,
        task="detect",
    )

    save_dir = Path(getattr(results, "save_dir", Path(args.project) / args.name)).resolve()
    best_weight = save_dir / "weights" / "best.pt"
    if not best_weight.exists():
        raise FileNotFoundError(f"训练完成但未找到最佳权重：{best_weight}")

    export_path = Path(args.export_path).resolve()
    export_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(best_weight, export_path)

    summary = {
        "dataset": validation.dataset_dir,
        "classes": validation.classes,
        "train": asdict(validation.train),
        "val": asdict(validation.val),
        "save_dir": str(save_dir),
        "best_weight": str(best_weight),
        "export_path": str(export_path),
        "trained_at": datetime.now().isoformat(timespec="seconds"),
    }
    _write_training_summary(save_dir / "material_det_training_summary.json", summary)
    return summary


def _scan_split(images_dir: Path, labels_dir: Path, class_count: int) -> tuple[int, int, int, list[str]]:
    """@brief 扫描单个划分的图片、标注和目标数量。"""
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

        objects_in_file = 0
        with label_path.open("r", encoding="utf-8") as file:
            for line_no, raw_line in enumerate(file, start=1):
                raw_line = raw_line.strip()
                if not raw_line:
                    continue
                parts = raw_line.split()
                if len(parts) != 5:
                    warnings.append(f"标注格式异常：{label_path.name} 第 {line_no} 行")
                    continue
                try:
                    class_id = int(parts[0])
                    x_center, y_center, width, height = map(float, parts[1:])
                except ValueError:
                    warnings.append(f"标注数值异常：{label_path.name} 第 {line_no} 行")
                    continue
                if class_id < 0 or class_id >= class_count:
                    warnings.append(f"类别编号越界：{label_path.name} 第 {line_no} 行")
                    continue
                if not _is_valid_det_box(x_center, y_center, width, height):
                    warnings.append(f"检测框坐标越界：{label_path.name} 第 {line_no} 行")
                    continue
                objects_in_file += 1

        if objects_in_file == 0:
            warnings.append(f"标注为空：{label_path.name}")
            continue

        label_count += 1
        object_count += objects_in_file

    return image_count, label_count, object_count, warnings


def _is_valid_det_box(x_center: float, y_center: float, width: float, height: float) -> bool:
    """@brief 判断归一化检测框是否合法。"""
    return 0.0 <= x_center <= 1.0 and 0.0 <= y_center <= 1.0 and 0.0 < width <= 1.0 and 0.0 < height <= 1.0


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


def _write_training_summary(path: Path, summary: dict[str, Any]) -> None:
    """@brief 写入本次训练摘要。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        json.dump(summary, file, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
