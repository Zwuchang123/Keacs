import sqlite3

from fastapi import APIRouter, Request, status
from fastapi.responses import JSONResponse

router = APIRouter()


@router.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/ready")
async def ready(request: Request) -> JSONResponse:
    settings = request.app.state.settings
    checks: dict[str, str] = {}
    is_ready = True

    try:
        with sqlite3.connect(settings.audit_db_path) as connection:
            connection.execute("SELECT 1")
        checks["audit_log"] = "ok"
    except sqlite3.Error:
        checks["audit_log"] = "failed"
        is_ready = False

    if settings.model_provider == "mock":
        checks["model_provider"] = "mock"
    elif settings.model_base_url and settings.model_api_key:
        checks["model_provider"] = "configured"
    else:
        checks["model_provider"] = "missing_config"
        is_ready = False

    return JSONResponse(
        status_code=status.HTTP_200_OK if is_ready else status.HTTP_503_SERVICE_UNAVAILABLE,
        content={"status": "ready" if is_ready else "not_ready", "checks": checks},
    )
