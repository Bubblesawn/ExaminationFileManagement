import os
import secrets

from fastapi import Header, HTTPException, status

ALGORITHM_API_KEY_HEADER = "X-Internal-Api-Key"
DEFAULT_ALGORITHM_API_KEY = "examination-algorithm-local-key"


def verify_internal_api_key(
    x_internal_api_key: str | None = Header(default=None, alias=ALGORITHM_API_KEY_HEADER),
) -> None:
    """@brief 校验后端访问算法服务的内部 API Key。

    @details
    算法服务只允许后端通过服务间密钥调用业务接口，前端用户 Token 不直接透传到算法服务。
    本地开发默认使用固定密钥，生产环境应通过 ALGORITHM_SERVICE_API_KEY 环境变量覆盖。

    @param x_internal_api_key 请求头 X-Internal-Api-Key 的值。
    @raises HTTPException 当请求头缺失或密钥不匹配时返回 401。
    """
    expected_api_key = os.getenv("ALGORITHM_SERVICE_API_KEY", DEFAULT_ALGORITHM_API_KEY)
    if not x_internal_api_key or not secrets.compare_digest(x_internal_api_key, expected_api_key):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="算法服务认证失败",
        )
