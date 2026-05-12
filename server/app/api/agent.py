from time import perf_counter
from uuid import uuid4
import json

import httpx
from fastapi import APIRouter, HTTPException, Request, status
from fastapi.responses import StreamingResponse

from app.agent.model_client import ModelProviderClient
from app.agent.fallback import build_fallback_response
from app.agent.orchestrator import AgentOrchestrator
from app.agent.schemas import (
    AgentActionPreview,
    AgentChatRequest,
    AgentChatResponse,
    AgentFeedbackRequest,
    AgentRunContextRequest,
    AgentRunFeedbackRequest,
    AgentRunRequest,
    AgentRunResumeRequest,
    AgentSuggestion,
    AgentSuggestionRequest,
    LocalContext,
)
from app.agent.validators import count_context_items, extract_action_types
from app.config import Settings
from app.security.rate_limit import DeviceRateLimiter
from app.storage.audit_log import AuditLog
from app.storage.agent_run_store import AgentRunStore, StoredAgentRun


def create_agent_router(settings: Settings, audit_log: AuditLog) -> APIRouter:
    router = APIRouter(prefix="/api/agent", tags=["agent"])
    rate_limiter = DeviceRateLimiter(settings.request_per_minute_limit, settings.request_per_day_limit)
    run_store = AgentRunStore()

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

    @router.post("/runs/stream")
    async def stream_run(payload: AgentRunRequest, request: Request) -> StreamingResponse:
        started_at = perf_counter()
        error_type = ""
        run_id = str(uuid4())
        action_types: list[str] = []
        try:
            _validate_run_request(payload, settings)
            rate_result = rate_limiter.check(payload.device_id_hash)
            if not rate_result.allowed:
                error_type = "rate_limited"
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail=rate_result.message,
                    headers={"Retry-After": str(rate_result.retry_after_seconds)},
                )
            run_store.create(
                StoredAgentRun(
                    run_id=run_id,
                    client_request_id=payload.client_request_id,
                    device_id_hash=payload.device_id_hash,
                    message=payload.message,
                )
            )
            events = _build_stream_events(run_id, payload)
            preview_actions = [
                action
                for event in events
                if event["type"] == "action_preview"
                for action in event.get("actions", [])
            ]
            action_types = [str(action.get("type")) for action in preview_actions]
            run_store.set_pending_actions(
                run_id,
                [
                    str(action.get("actionId") or action.get("title") or index)
                    for index, action in enumerate(preview_actions)
                ],
            )
            return StreamingResponse(
                _iter_sse(events),
                media_type="text/event-stream",
                headers={"Cache-Control": "no-cache"},
            )
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
                action_types=action_types,
                error_type=error_type,
            )

    @router.post("/runs/{run_id}/context")
    async def continue_context(run_id: str, payload: AgentRunContextRequest) -> dict[str, str]:
        run = run_store.get_or_create(run_id, payload.client_request_id, payload.device_id_hash)
        run_store.add_observations(
            run.run_id,
            [item.model_dump(by_alias=True) for item in payload.observations],
        )
        return {"status": "ok", "runId": run_id}

    @router.post("/runs/{run_id}/resume")
    async def resume_run(run_id: str, payload: AgentRunResumeRequest) -> dict[str, str]:
        run = run_store.get_or_create(run_id, payload.client_request_id, payload.device_id_hash)
        run_store.resume(run.run_id, payload.decision)
        return {"status": "ok", "runId": run_id}

    @router.post("/runs/{run_id}/feedback")
    async def run_feedback(run_id: str, payload: AgentRunFeedbackRequest) -> dict[str, str]:
        run = run_store.get_or_create(run_id, payload.client_request_id, payload.device_id_hash)
        run_store.add_feedback(
            run.run_id,
            {
                "messageId": payload.message_id,
                "feedback": payload.feedback,
                "reason": payload.reason,
            },
        )
        return {"status": "ok", "runId": run_id}

    @router.post("/suggestions")
    async def suggestions(payload: AgentSuggestionRequest) -> dict[str, list[AgentSuggestion]]:
        return {"suggestions": _build_suggestions(payload)}

    return router


def _validate_chat_request(payload: AgentChatRequest, settings: Settings) -> None:
    if len(payload.message) > settings.max_message_length:
        raise HTTPException(status_code=400, detail="输入内容过长，请缩短后再发送。")
    if count_context_items(payload.local_context) > settings.max_context_items:
        raise HTTPException(status_code=400, detail="本次上下文过大，请缩小查询范围后再试。")
    history_chars = sum(len(item.content) for item in payload.conversation_history)
    if len(payload.conversation_history) > 40 or history_chars > 12_000:
        raise HTTPException(status_code=400, detail="对话内容过长，请先清空对话后再继续。")


def _validate_run_request(payload: AgentRunRequest, settings: Settings) -> None:
    if len(payload.message) > settings.max_message_length:
        raise HTTPException(status_code=400, detail="输入内容过长，请缩短后再发送。")
    history_chars = sum(len(item.content) for item in payload.conversation_history)
    if len(payload.conversation_history) > 40 or history_chars > 12_000:
        raise HTTPException(status_code=400, detail="对话内容过长，请先清空对话后再继续。")


def _build_stream_events(run_id: str, payload: AgentRunRequest) -> list[dict]:
    context_requests = _context_requests_for(payload.message)
    local_context = LocalContext(
        timeContext={"today": "2026-05-12"},
        categories=[
            {"name": "餐饮", "direction": "EXPENSE"},
            {"name": "交通", "direction": "EXPENSE"},
            {"name": "其他", "direction": "EXPENSE"},
            {"name": "工资", "direction": "INCOME"},
            {"name": "其他", "direction": "INCOME"},
        ],
        accounts=[
            {"name": "微信"},
            {"name": "银行卡"},
            {"name": "现金"},
        ],
        records=[],
        stats={},
        scheduledRecords=[],
    )
    chat_request = AgentChatRequest(
        clientRequestId=payload.client_request_id,
        deviceIdHash=payload.device_id_hash,
        message=payload.message,
        localContext=local_context,
        conversationHistory=payload.conversation_history,
        timezone=payload.timezone,
        appVersion=payload.app_version,
    )
    response = build_fallback_response(chat_request)
    events: list[dict] = [
        {"type": "run_started", "runId": run_id},
        {"type": "stage_changed", "stage": "understanding"},
    ]
    if context_requests:
        events.extend(
            [
                {"type": "context_requested", "runId": run_id, "requests": context_requests},
                {"type": "stage_changed", "stage": "reading_context"},
            ]
        )
    events.extend(
        [
            {"type": "stage_changed", "stage": "reasoning"},
            {"type": "partial_message", "content": response.reply[:80]},
            {"type": "stage_changed", "stage": "validating"},
        ]
    )
    action_payloads = [
        _action_to_event_payload(action, index)
        for index, action in enumerate(response.actions)
        if action.type != "answer_only"
    ]
    if action_payloads and any(action["type"] != "ask_user" for action in action_payloads):
        action_ids = [action["actionId"] for action in action_payloads]
        events.extend(
            [
                {"type": "action_preview", "runId": run_id, "actions": action_payloads},
                {"type": "stage_changed", "stage": "awaiting_confirmation"},
                {"type": "awaiting_confirmation", "runId": run_id, "actionIds": action_ids},
            ]
        )
    else:
        events.extend(
            [
                {"type": "stage_changed", "stage": "finalizing"},
                {"type": "final_message", "reply": response.reply, "warnings": response.warnings},
            ]
        )
    return events


def _context_requests_for(message: str) -> list[dict[str, str]]:
    requests = [
        {"type": "category_list", "reason": "读取可用分类"},
        {"type": "account_list", "reason": "读取可用账户"},
    ]
    if any(term in message for term in ("改", "删", "删除", "那笔", "大额", "明细")):
        requests.append({"type": "record_candidates", "reason": "定位候选账目"})
    elif any(term in message for term in ("多少", "花了", "支出", "收入", "结余", "复盘", "分析")):
        requests.append({"type": "month_stats", "reason": "读取统计摘要"})
    if any(term in message for term in ("定时", "每周", "每月", "每年")):
        requests.append({"type": "scheduled_record_list", "reason": "读取定时记账"})
    return requests


def _action_to_event_payload(action: AgentActionPreview, index: int) -> dict:
    payload = action.model_dump(by_alias=True)
    payload["actionId"] = f"action-{index + 1}"
    return payload


def _iter_sse(events: list[dict]):
    for event in events:
        yield "data: " + json.dumps(event, ensure_ascii=False, separators=(",", ":")) + "\n\n"


def _build_suggestions(payload: AgentSuggestionRequest) -> list[AgentSuggestion]:
    candidates: list[AgentSuggestion] = []
    if payload.today.endswith(("-28", "-29", "-30", "-31")):
        candidates.append(AgentSuggestion(text="月末复盘本月支出", reason="month_end"))
    if payload.today.endswith("-01"):
        candidates.append(AgentSuggestion(text="看看上月结余", reason="month_start"))
    recent_text = " ".join(item.content for item in payload.recent_conversation)
    if "餐饮" in recent_text:
        candidates.append(AgentSuggestion(text="继续看本月餐饮明细", reason="recent_topic"))
    if payload.local_summary.get("largeExpense") or payload.local_summary.get("hasLargeExpense"):
        candidates.append(AgentSuggestion(text="找出最近的大额消费", reason="spending_change"))
    candidates.extend(
        [
            AgentSuggestion(text="记一笔今天的支出", reason="user_habit"),
            AgentSuggestion(text="分析最近7天消费", reason="date"),
            AgentSuggestion(text="查看本月收入支出", reason="ledger"),
        ]
    )
    deduped: list[AgentSuggestion] = []
    seen: set[str] = set()
    for item in candidates:
        if item.text not in seen:
            deduped.append(item)
            seen.add(item.text)
        if len(deduped) >= payload.limit:
            break
    return deduped[: payload.limit]


def _http_error_type(status_code: int) -> str:
    if status_code == 400:
        return "bad_request"
    if status_code == 429:
        return "rate_limited"
    return "http_error"
