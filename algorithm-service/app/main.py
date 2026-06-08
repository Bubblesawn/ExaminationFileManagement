from pathlib import Path

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.api import routes

app = FastAPI(title="考籍管理系统算法服务", version="0.0.1")
app.include_router(routes.router, prefix="/api")

MASK_OUTPUT_DIR = Path(__file__).resolve().parents[1] / "uploads" / "masks"
MASK_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
app.mount("/generated/masks", StaticFiles(directory=MASK_OUTPUT_DIR), name="generated-masks")

SEGMENT_OUTPUT_DIR = Path(__file__).resolve().parents[1] / "uploads" / "segments"
SEGMENT_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
app.mount("/generated/segments", StaticFiles(directory=SEGMENT_OUTPUT_DIR), name="generated-segments")


@app.get("/health")
def health() -> dict[str, str]:
    """返回算法服务健康状态。"""
    return {"status": "UP", "service": "考籍管理系统算法服务"}

