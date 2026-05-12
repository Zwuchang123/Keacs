from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from pydantic import ValidationError

from app.api.agent import create_agent_router
from app.api.health import router as health_router
from app.config import Settings
from app.storage.audit_log import AuditLog


@asynccontextmanager
async def lifespan(app: FastAPI):
    timeout = httpx.Timeout(app.state.settings.model_request_timeout_seconds)
    limits = httpx.Limits(max_connections=50, max_keepalive_connections=20)
    app.state.model_http_client = httpx.AsyncClient(timeout=timeout, limits=limits)
    try:
        yield
    finally:
        await app.state.model_http_client.aclose()


def create_app(settings: Settings | None = None) -> FastAPI:
    app_settings = settings or Settings.from_env()
    app_settings.ensure_storage_parent()
    audit_log = AuditLog(app_settings.audit_db_path)

    app = FastAPI(title=app_settings.app_name, lifespan=lifespan)
    app.state.settings = app_settings
    app.state.audit_log = audit_log

    app.include_router(health_router)
    app.include_router(create_agent_router(app_settings, audit_log))

    @app.exception_handler(ValidationError)
    async def validation_error_handler(_: Request, exc: ValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=502,
            content={"detail": "模型返回内容格式不正确，已停止处理。", "errorType": "invalid_model_output"},
        )

    return app


app = create_app()
