# YOLO 材料分类模型训练说明

## 一、用途

本文档说明如何使用 `algorithm-service/dataset/classification_material_dataset` 数据集训练考籍材料图像分类模型。训练完成后，最佳权重会复制到 `algorithm-service/app/models/weights/material-cls.pt`，供后续算法服务推理模块加载。

## 二、数据集结构

当前分类数据集采用 Ultralytics YOLO 分类任务目录结构：

```text
algorithm-service/dataset/classification_material_dataset/
  train/
    ADMISSION_TICKET/
    DIPLOMA/
    EXEMPTION_CERTIFICATE/
    ID_CARD/
    PHOTO/
    TRANSCRIPT/
  val/
    ADMISSION_TICKET/
    DIPLOMA/
    EXEMPTION_CERTIFICATE/
    ID_CARD/
    PHOTO/
    TRANSCRIPT/
  data.yaml
```

训练脚本会校验以下内容：

- `train` 和 `val` 目录必须存在。
- 训练集和验证集类别目录必须一致。
- `data.yaml` 中的类别名称必须与目录类别一致。
- 每个类别至少包含 1 张训练图片和 1 张验证图片。
- 每类训练样本少于 50 张时会给出 PoC 风险提示。

## 三、安装依赖

在 `algorithm-service` 目录下安装依赖：

```powershell
cd E:\shixun\ExaminationFileManagement\algorithm-service
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

如果使用 NVIDIA GPU，请按本机 CUDA 版本安装匹配的 PyTorch，再运行训练脚本。通用依赖文件不固定 CUDA 版本，避免影响不同机器部署。

## 四、训练前校验

先运行 dry-run，确认数据集结构和类别配置没有问题：

```powershell
cd E:\shixun\ExaminationFileManagement\algorithm-service
.\.venv\Scripts\python.exe .\scripts\train_yolo_classifier.py --dry-run
```

校验通过后会输出每个类别的训练集和验证集样本数量。

## 五、开始训练

CPU PoC 训练命令：

```powershell
cd E:\shixun\ExaminationFileManagement\algorithm-service
.\.venv\Scripts\python.exe .\scripts\train_yolo_classifier.py --device cpu --epochs 80 --imgsz 224 --batch 16
```

GPU 训练命令示例：

```powershell
cd E:\shixun\ExaminationFileManagement\algorithm-service
.\.venv\Scripts\python.exe .\scripts\train_yolo_classifier.py --device 0 --epochs 80 --imgsz 224 --batch 16
```

如显存不足，可将 `--batch` 调整为 `8` 或 `4`。

## 六、训练输出

默认输出目录：

```text
algorithm-service/runs/classify/material-cls/
```

关键文件：

- `weights/best.pt`：验证集指标最优权重。
- `weights/last.pt`：最后一轮训练权重。
- `material_cls_training_summary.json`：本次训练的数据集、类别、权重路径和训练时间摘要。
- `algorithm-service/app/models/weights/material-cls.pt`：复制后的服务推理权重。

## 七、常用参数

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `--data` | `algorithm-service/dataset/classification_material_dataset` | 分类数据集根目录 |
| `--model` | `yolo11n-cls.pt` | 预训练分类模型 |
| `--epochs` | `80` | 训练轮数 |
| `--imgsz` | `224` | 输入图片尺寸 |
| `--batch` | `16` | 批大小 |
| `--device` | `cpu` | 训练设备 |
| `--workers` | `0` | 数据加载线程数 |
| `--export-path` | `algorithm-service/app/models/weights/material-cls.pt` | 服务权重输出路径 |

## 八、验收建议

PoC 阶段建议先确认：

- Top-1 准确率不低于 70%。
- Top-3 准确率不低于 85%。
- 每个类别在验证集中都有样本。
- 推理权重已生成到 `app/models/weights/material-cls.pt`。

后续接入 `/api/image-classify` 前，应补充模型加载失败回退策略，确保权重缺失时仍能返回可控结果。
