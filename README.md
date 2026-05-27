# 省考试院自学考试考籍管理系统

本项目用于建设省考试院自学考试考籍管理系统，包含考生考籍档案管理、考生免考管理、课程顶替、考籍转入转出、毕业管理、日志管理，以及图像分类、目标检测、图像分割、智能问答、ASR、TTS 等智能辅助能力。

## 技术栈

- 后端：Spring Boot、MyBatis-Plus、MySQL、Knife4j
- 前端：Vue3、Vite、TypeScript、Pinia、Vue Router、Element Plus
- 算法服务：FastAPI

## 目录结构

```text
backend             Spring Boot 后端服务
frontend            Vue3 前端应用
algorithm-service   Python 算法服务
sql                 数据库初始化脚本
docs                中文项目文档
```

## 启动说明

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8088`

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

### 算法服务

```bash
cd D:\idea\IDEAProject\ExaminationFileManagement\algorithm-service

python -m venv .venv
.\.venv\Scripts\Activate.ps1

python -m pip install -r requirements.txt

# 首次接入 DeepSeek 时复制一份本地配置，并填写自己的 API Key
copy .env.example .env

python -m uvicorn app.main:app --host 127.0.0.1 --port 9000 --reload
```

默认地址：`http://localhost:9000`

DeepSeek 配置项：

- `DEEPSEEK_API_KEY`：DeepSeek API Key，本地 `.env` 保存，禁止提交到仓库。
- `DEEPSEEK_BASE_URL`：默认 `https://api.deepseek.com`。
- `DEEPSEEK_MODEL`：默认 `deepseek-v4-flash`。
- `MATERIAL_UPLOAD_ROOT`：算法服务读取后端上传图片的材料目录。
