from time import perf_counter

from fastapi import APIRouter, HTTPException, Request, status

from app.agent.model_client import ModelProviderClient
from app.agent.orchestrator import AgentOrchestrator
from app.agent.schemas import AgentChatRequest, AgentChatResponse, AgentFeedbackRequest
from app.agent.validators import count_context_items, extract_action_types
from app.config import Settings
from app.security.rate_limit import DeviceRateLimiter
from app.storage.audit_log import AuditLog


def create_agent_router(settings: Settings, audit_log: AuditLog) -> APIRouter:
    router = APIRouter(prefix="/api/agent", tags=["agent"])
    rate_limiter = DeviceRateLimiter(settings.request_per_minute_limit, settings.request_per_day_limit)
    model_client = ModelProviderClient(settings)
    orchestrator = AgentOrchestrator(model_client)

    @router.post("/chat", response_model=AgentChatResponse)
    async def chat(payload: AgentChatRequest, request: Request) -> AgentChatResponse:
        started_at = perf_counter()
        error_type = ""
        action_types: list[str] = []
        try:
            _validate_chat_request(payload, settings)
            rate_result = rate_limiter.check(payload.device_id_hash)
            if not rate_result.allowed:
                error_type = "rate_limited"
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail=rate_result.message,
                    headers={"Retry-After": str(rate_result.retry_after_seconds)},
                )
            response = await orchestrator.handle_chat(payload)
            action_types = extract_action_types(response.actions)
            return response
        except HTTPException as exc:
            error_type = error_type or _http_error_type(exc.status_code)
            raise exc
        except Exception:
            error_type = "model_call_failed"
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="模型服务暂时不可用，请稍后再试。",
            )
        finally:
            audit_log.record(
                endpoint=str(request.url.path),
                status="failed" if error_type else "ok",
                duration_ms=int((perf_counter() - started_at) * 1000),
                device_id_hash=payload.device_id_hash,
                model_provider=settings.model_provider,
                action_types=action_types,
                error_type=error_type,
            )

    @router.post("/feedback")
    async def feedback(payload: AgentFeedbackRequest, request: Request) -> dict[str, str]:
        started_at = perf_counter()
        error_type = ""
        try:
            rate_result = rate_limiter.check(payload.device_id_hash)
            if not rate_result.allowed:
                error_type = "rate_limited"
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail=rate_result.message,
                    headers={"Retry-After": str(rate_result.retry_after_seconds)},
                )
            return {"status": "ok"}
        except HTTPException as exc:
            error_type = error_type or _http_error_type(exc.status_code)
            raise exc
        finally:
            audit_log.record(
                endpoint=str(request.url.path),
                status="failed" if error_type else "ok",
                duration_ms=int((perf_counter() - started_at) * 1000),
                device_id_hash=payload.device_id_hash,
                model_provider=settings.model_provider,
                action_types=payload.action_types,
                error_type=error_type,
            )

    return router


def _validate_chat_request(payload: AgentChatRequest, settings: Settings) -> None:
    if len(payload.message) > settings.max_message_length:
        raise HTTPException(status_code=400, detail="输入内容过长，请缩短后再发送。")
    if count_context_items(payload.local_context) > settings.max_context_items:
        raise HTTPException(status_code=400, detail="本次上下文过大，请缩小查询范围后再试。")


def _http_error_type(status_code: int) -> str:
    if status_code == 400:
        return "bad_request"
    if status_code == 429:
        return "rate_limited"
    return "http_error"
