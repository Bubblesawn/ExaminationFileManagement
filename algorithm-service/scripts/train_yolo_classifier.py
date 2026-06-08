"""YOLO 材料图像分类模型训练入口。"""

from __future__ import annotations

import argparse
import json
import shutil
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import yaml  # type: ignore 

PROJECT_ROOT = Path(__file__).resolve().parents[2]
SERVICE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATASET_DIR = SERVICE_ROOT / "dataset" / "classification_material_dataset"
DEFAULT_MODEL = "yolo11n-cls.pt"
DEFAULT_PROJECT_DIR = SERVICE_ROOT / "runs" / "classify"
DEFAULT_WEIGHTS_DIR = SERVICE_ROOT / "app" / "models" / "weights"
DEFAULT_EXPORT_PATH = DEFAULT_WEIGHTS_DIR / "material-cls.pt"
DEFAULT_IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


@dataclass(frozen=True)
class DatasetClassSummary:
    """@brief 分类数据集中单个类别的样本统计。"""

    name: str
    train_count: int
    val_count: int


@dataclass(frozen=True)
class DatasetValidationResult:
    """@brief 分类数据集训练前校验结果。"""

    dataset_dir: str
    classes: list[DatasetClassSummary]
    warnings: list[str]


def parse_args() -> argparse.Namespace:
    """@brief 解析 YOLO 分类训练命令行参数。

    @return argparse 解析后的训练参数命名空间。
    """
    parser = argparse.ArgumentParser(description="训练考籍材料 YOLO 分类模型")
    parser.add_argument("--data", default=str(DEFAULT_DATASET_DIR), help="分类数据集根目录，需包含 train 和 val 子目录")
    parser.add_argument("--model", default=DEFAULT_MODEL, help="预训练分类模型权重，例如 yolo11n-cls.pt")
    parser.add_argument("--epochs", type=int, default=80, help="训练轮数")
    parser.add_argument("--imgsz", type=int, default=224, help="输入图片尺寸")
    parser.add_argument("--batch", type=int, default=16, help="批大小，显存不足时可降低")
    parser.add_argument("--device", default="cpu", help="训练设备，例如 cpu、0、cuda:0")
    parser.add_argument("--workers", type=int, default=0, help="数据加载线程数，Windows 本地训练建议先使用 0")
    parser.add_argument("--patience", type=int, default=20, help="早停等待轮数")
    parser.add_argument("--project", default=str(DEFAULT_PROJECT_DIR), help="训练输出目录")
    parser.add_argument("--name", default="material-cls", help="本次训练名称")
    parser.add_argument("--export-path", default=str(DEFAULT_EXPORT_PATH), help="best.pt 复制后的服务权重路径")
    parser.add_argument("--dry-run", action="store_true", help="仅校验数据集和参数，不启动训练")
    return parser.parse_args()


def validate_dataset(dataset_dir: Path) -> DatasetValidationResult:
    """@brief 校验 YOLO 分类数据集目录和类别样本数量。

    @param dataset_dir 分类数据集根目录。
    @return 数据集类别统计和非阻塞警告。
    @raises FileNotFoundError 当 train 或 val 目录不存在时抛出。
    @raises ValueError 当训练集、验证集类别不一致或没有可训练图片时抛出。
    """
    train_dir = dataset_dir / "train"
    val_dir = dataset_dir / "val"
    if not train_dir.is_dir():
        raise FileNotFoundError(f"训练集目录不存在：{train_dir}")
    if not val_dir.is_dir():
        raise FileNotFoundError(f"验证集目录不存在：{val_dir}")

    train_classes = _list_class_dirs(train_dir)
    val_classes = _list_class_dirs(val_dir)
    if not train_classes:
        raise ValueError("训练集未发现任何类别目录")
    if train_classes != val_classes:
        raise ValueError(f"训练集和验证集类别不一致：train={train_classes}, val={val_classes}")

    data_yaml_classes = _load_data_yaml_classes(dataset_dir / "data.yaml")
    if data_yaml_classes and sorted(data_yaml_classes) != train_classes:
        raise ValueError(f"data.yaml 类别与目录类别不一致：yaml={data_yaml_classes}, dirs={train_classes}")

    summaries: list[DatasetClassSummary] = []
    warnings: list[str] = []
    for class_name in train_classes:
        train_count = _count_images(train_dir / class_name)
        val_count = _count_images(val_dir / class_name)
        if train_count == 0:
            raise ValueError(f"类别 {class_name} 的训练样本为空")
        if val_count == 0:
            raise ValueError(f"类别 {class_name} 的验证样本为空")
        if train_count < 50:
            warnings.append(f"类别 {class_name} 训练样本少于 PoC 建议值 50 张")
        if val_count < 10:
            warnings.append(f"类别 {class_name} 验证样本少于 10 张，指标波动可能较大")
        summaries.append(DatasetClassSummary(class_name, train_count, val_count))

    return DatasetValidationResult(str(dataset_dir), summaries, warnings)


def train_classifier(args: argparse.Namespace, validation: DatasetValidationResult) -> dict[str, Any]:
    """@brief 调用 Ultralytics YOLO Python API 训练材料分类模型。

    @param args 命令行训练参数。
    @param validation 已完成的数据集校验结果。
    @return 包含训练输出目录和导出权重路径的摘要。
    """
    try:
        from ultralytics import YOLO
    except ImportError as exc:
        raise RuntimeError("未安装 ultralytics，请先执行 pip install -r requirements.txt") from exc

    model = YOLO(args.model)
    results = model.train(
        data=str(Path(args.data).resolve()),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        workers=args.workers,
        patience=args.patience,
        project=str(Path(args.project).resolve()),
        name=args.name,
        task="classify",
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
        "classes": [asdict(item) for item in validation.classes],
        "save_dir": str(save_dir),
        "best_weight": str(best_weight),
        "export_path": str(export_path),
        "trained_at": datetime.now().isoformat(timespec="seconds"),
    }
    _write_training_summary(save_dir / "material_cls_training_summary.json", summary)
    return summary


def _list_class_dirs(split_dir: Path) -> list[str]:
    """@brief 按名称列出数据集划分目录下的类别目录。"""
    return sorted(item.name for item in split_dir.iterdir() if item.is_dir())


def _count_images(class_dir: Path) -> int:
    """@brief 统计类别目录下可被 YOLO 分类训练读取的图片数量。"""
    return sum(1 for item in class_dir.iterdir() if item.is_file() and item.suffix.lower() in DEFAULT_IMAGE_SUFFIXES)


def _load_data_yaml_classes(data_yaml: Path) -> list[str]:
    """@brief 从 data.yaml 读取类别名称，用于和目录结构做一致性校验。"""
    if not data_yaml.exists():
        return []
    with data_yaml.open("r", encoding="utf-8") as file:
        payload = yaml.safe_load(file) or {}
    names = payload.get("names") or {}
    if isinstance(names, dict):
        return sorted(str(name) for name in names.values())
    if isinstance(names, list):
        return sorted(str(name) for name in names)
    return []


def _write_training_summary(path: Path, summary: dict[str, Any]) -> None:
    """@brief 写入本次训练摘要，便于模型版本追踪和答辩说明。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        json.dump(summary, file, ensure_ascii=False, indent=2)


def main() -> None:
    """@brief 训练脚本主入口，负责串联数据集校验、训练和权重归档。"""
    args = parse_args()
    dataset_dir = Path(args.data).resolve()
    validation = validate_dataset(dataset_dir)
    print(json.dumps(asdict(validation), ensure_ascii=False, indent=2))
    if args.dry_run:
        print("数据集校验通过，dry-run 模式未启动训练。")
        return

    summary = train_classifier(args, validation)
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
