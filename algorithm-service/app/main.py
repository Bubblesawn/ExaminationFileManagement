from fastapi import FastAPI

from app.api import routes

app = FastAPI(title="考籍管理系统算法服务", version="0.0.1")
app.include_router(routes.router, prefix="/api")


@app.get("/health")
def health() -> dict[str, str]:
    """返回算法服务健康状态。"""
    return {"status": "UP", "service": "考籍管理系统算法服务"}

