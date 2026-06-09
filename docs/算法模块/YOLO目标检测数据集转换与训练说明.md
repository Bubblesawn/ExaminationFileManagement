# YOLO 目标检测数据集转换与训练说明

## 一、用途

本说明用于将 `segmentation_material_dataset` 这类 YOLO 分割数据集，转换为 YOLO 目标检测数据集，并重新训练目标检测模型。

当前转换策略是：将每个分割标注多边形计算为最小外接矩形，输出为检测任务所需的 `x_center`、`y_center`、`width`、`height` 归一化标注。

这样做适合你现在的需求：

- 目标检测需要在一张图中把多个物体都框出来。
- 你的原始数据已经有多个目标的分割标注。
- 可以直接把分割标注压成检测框，避免重新手工标注。

## 二、你的数据集格式

你的数据集目录应类似如下结构：

```text
D:/qqfile/segmentation_material_dataset/segmentation_material_dataset/
  images/
    train/
    val/
  labels/
    train/
    val/
  data.yaml
  data.yml
```

每个 `labels/*.txt` 文件内容应为 YOLO 分割格式，例如：

```text
0 0.388947 0.183056 0.541189 0.152799 0.571429 0.465961 0.419187 0.496218
```

即：

- 第 1 列是类别编号。
- 后续每 2 列是一组多边形点坐标。
- 坐标为 0 到 1 的归一化值。

## 三、转换脚本

脚本路径：

```text
algorithm-service/scripts/convert_yolo_seg_to_det.py
```

### 1. 仅检查数据

```powershell
cd E:\shixun\ExaminationFileManagement
python .\algorithm-service\scripts\convert_yolo_seg_to_det.py --source "D:\qqfile\segmentation_material_dataset\segmentation_material_dataset" --dry-run
```

### 2. 生成检测数据集

默认输出到：

```text
algorithm-service/dataset/detection_material_dataset
```

如果希望同时复制图片：

```powershell
cd E:\shixun\ExaminationFileManagement
python .\algorithm-service\scripts\convert_yolo_seg_to_det.py --source "D:\qqfile\segmentation_material_dataset\segmentation_material_dataset" --copy-images
```

## 四、训练检测模型

脚本路径：

```text
algorithm-service/scripts/train_yolo_detector.py
```

### 1. 训练前校验

```powershell
cd E:\shixun\ExaminationFileManagement
python .\algorithm-service\scripts\train_yolo_detector.py --dry-run
```

### 2. 开始训练

```powershell
cd E:\shixun\ExaminationFileManagement
python .\algorithm-service\scripts\train_yolo_detector.py --device cpu --epochs 120 --imgsz 640 --batch 8
```

训练完成后，最佳权重会复制到：

```text
algorithm-service/app/models/weights/material-det.pt
```

## 五、说明

- 这个转换不是重新人工标注，而是把分割轮廓压成检测框。
- 适合“一个图片里有多个目标都要被框出来”的场景。
- 如果你后面想进一步提升检测框贴合度，最好还是保留一份原始检测标注或单独做检测标注。

