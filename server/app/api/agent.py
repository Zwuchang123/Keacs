from time import perf_counter

import httpx
from fastapi import APIRouter, HTTPException, Request, status

from app.agent.model_client import ModelProviderClient
from app.agent.fallback import build_fallback_response
from app.agent.orchestrator import AgentOrchestrator
from app.agent.schemas import AgentChatRequest, AgentChatResponse, AgentFeedbackRequest
from app.agent.validators import count_context_items, extract_action_types
from app.config import Settings
from app.security.rate_limit import DeviceRateLimiter
from app.storage.audit_log import AuditLog


def create_agent_router(settings: Settings, audit_log: AuditLog) -> APIRouter:
    router = APIRouter(prefix="/api/agent", tags=["agent"])
    rate_limiter = DeviceRateLimiter(settings.request_per_minute_limit, settings.request_per_day_limit)

    @router.post("/chat", response_model=AgentChatResponse)
    async def chat(payload: AgentChatRequest, request: Request) -> AgentChatResponse:
        started_at = perf_counter()
        error_type = ""
        action_types: list[str] = []
        owned_client: httpx.AsyncClient | None = None
        shared_client = getattr(request.app.state, "model_http_client", None)
        if shared_client is None:
            owned_client = httpx.AsyncClient(timeout=settings.model_request_timeout_seconds)
            shared_client = owned_client
        model_client = ModelProviderClient(settings, shared_client)
        orchestrator = AgentOrchestrator(model_client)
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
            response = await orchestrator.handle_chat(payload, request)
            action_types = extract_action_types(response.actions)
            return response
        except HTTPException as exc:
            error_type = error_type or _http_error_type(exc.status_code)
            raise exc
        except Exception:
            error_type = "model_call_failed"
            response = build_fallback_response(
                payload,
                warning="模型服务暂时不稳定，已使用本地规则生成结果。",
            )
            action_types = extract_action_types(response.actions)
            return response
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
            if owned_client is not None:
                await owned_client.aclose()

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
    history_chars = sum(len(item.content) for item in payload.conversation_history)
    if len(payload.conversation_history) > 40 or history_chars > 12_000:
        raise HTTPException(status_code=400, detail="对话内容过长，请先清空对话后再继续。")


def _http_error_type(status_code: int) -> str:
    if status_code == 400:
        return "bad_request"
    if status_code == 429:
        return "rate_limited"
    return "http_error"
