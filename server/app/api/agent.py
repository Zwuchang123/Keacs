from time import perf_counter
from uuid import uuid4
import json
import asyncio

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
        owned_client: httpx.AsyncClient | None = None
        shared_client = getattr(request.app.state, "model_http_client", None)
        if shared_client is None:
            owned_client = httpx.AsyncClient(timeout=settings.model_request_timeout_seconds)
            shared_client = owned_client
        model_client = ModelProviderClient(settings, shared_client)
        orchestrator = AgentOrchestrator(model_client)
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
            return StreamingResponse(
                _stream_agent_run(
                    run_id=run_id,
                    payload=payload,
                    request=request,
                    settings=settings,
                    orchestrator=orchestrator,
                    run_store=run_store,
                    audit_log=audit_log,
                    model_provider=settings.model_provider,
                    started_at=started_at,
                    close_client=owned_client,
                ),
                # 明确声明 utf-8，避免部分客户端/代理按默认编码解码导致乱码
                media_type="text/event-stream; charset=utf-8",
                headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
            )
        except HTTPException as exc:
            error_type = error_type or _http_error_type(exc.status_code)
            raise exc
        finally:
            if error_type:
                audit_log.record(
                    endpoint=str(request.url.path),
                    status="failed",
                    duration_ms=int((perf_counter() - started_at) * 1000),
                    device_id_hash=payload.device_id_hash,
                    model_provider=settings.model_provider,
                    action_types=action_types,
                    error_type=error_type,
                )
                if owned_client is not None:
                    await owned_client.aclose()

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
    if count_context_items(payload.local_context) > settings.max_context_items:
        raise HTTPException(status_code=400, detail="本次上下文过大，请缩小查询范围后再试。")
    history_chars = sum(len(item.content) for item in payload.conversation_history)
    if len(payload.conversation_history) > 40 or history_chars > 12_000:
        raise HTTPException(status_code=400, detail="对话内容过长，请先清空对话后再继续。")


def _chat_request_from_run(payload: AgentRunRequest) -> AgentChatRequest:
    return AgentChatRequest(
        clientRequestId=payload.client_request_id,
        deviceIdHash=payload.device_id_hash,
        message=payload.message,
        localContext=_local_context_for_run(payload),
        conversationHistory=payload.conversation_history,
        timezone=payload.timezone,
        appVersion=payload.app_version,
    )


def _build_stream_events(
    run_id: str,
    payload: AgentRunRequest,
    response: AgentChatResponse,
) -> list[dict]:
    context_requests = [
        item.model_dump(by_alias=True)
        for item in response.context_requests
    ] or _context_requests_for(payload.message)
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


async def _stream_agent_run(
    run_id: str,
    payload: AgentRunRequest,
    request: Request,
    settings: Settings,
    orchestrator: AgentOrchestrator,
    run_store: AgentRunStore,
    audit_log: AuditLog,
    model_provider: str,
    started_at: float,
    close_client: httpx.AsyncClient | None,
):
    error_type = ""
    action_types: list[str] = []
    try:
        early_events = [
            {"type": "run_started", "runId": run_id},
            {"type": "stage_changed", "stage": "understanding"},
        ]
        for event in early_events:
            yield _format_sse(event)
            await asyncio.sleep(0)

        context_requests = _context_requests_for(payload.message)
        if context_requests:
            yield _format_sse({"type": "context_requested", "runId": run_id, "requests": context_requests})
            yield _format_sse({"type": "stage_changed", "stage": "reading_context"})
            await asyncio.sleep(0)

        yield _format_sse({"type": "stage_changed", "stage": "reasoning"})
        chat_request = _chat_request_from_run(payload)
        # 在模型调用期间持续输出“思考过程”的增量内容（同一条系统消息内），
        # 避免长时间无输出导致用户感觉卡住/流式不明显。
        async def _run_model_call() -> AgentChatResponse:
            return await orchestrator.handle_chat(chat_request, request)

        model_task = asyncio.create_task(_run_model_call())
        thinking_steps = [
            "解析输入，识别意图与关键信息…",
            "检查是否需要读取分类/账户/账目候选…",
            "组织回答结构，准备生成自然语言结果…",
        ]
        if context_requests:
            thinking_steps.insert(1, "已请求本地上下文，等待账本数据…")

        step_index = 0
        response: AgentChatResponse | None = None
        while True:
            if await request.is_disconnected():
                error_type = error_type or "client_disconnected"
                model_task.cancel()
                return
            if model_task.done():
                try:
                    response = model_task.result()
                except Exception:
                    error_type = "model_call_failed"
                    response = build_fallback_response(
                        chat_request,
                        warning="模型服务暂时不稳定，已使用本地规则生成结果。",
                    )
                break
            # 每隔一段时间追加一条“思考步骤”，让用户感知到阶段推进
            if step_index < len(thinking_steps):
                yield _format_sse({"type": "thinking_step", "content": thinking_steps[step_index]})
                step_index += 1
            else:
                # 保持连接活跃（部分中间层会因长时间无数据而断开）
                yield ": heartbeat\n\n"
            await asyncio.sleep(0.6)
        assert response is not None

        reply = response.reply or ""
        for chunk in _chunk_text(reply):
            yield _format_sse({"type": "partial_message", "content": chunk})
            await asyncio.sleep(0.035)

        yield _format_sse({"type": "stage_changed", "stage": "validating"})
        action_payloads = [
            _action_to_event_payload(action, index)
            for index, action in enumerate(response.actions)
            if action.type != "answer_only"
        ]
        if action_payloads and any(action["type"] != "ask_user" for action in action_payloads):
            action_ids = [action["actionId"] for action in action_payloads]
            action_types = [str(action.get("type")) for action in action_payloads]
            run_store.set_pending_actions(run_id, action_ids)
            yield _format_sse({"type": "action_preview", "runId": run_id, "actions": action_payloads})
            yield _format_sse({"type": "stage_changed", "stage": "awaiting_confirmation"})
            yield _format_sse({"type": "awaiting_confirmation", "runId": run_id, "actionIds": action_ids})
            # 让“思考过程”和“最终回复”在同一条系统回复中落地：即使需要确认，也发送 final_message。
            yield _format_sse({"type": "final_message", "reply": reply, "warnings": response.warnings})
        else:
            yield _format_sse({"type": "stage_changed", "stage": "finalizing"})
            yield _format_sse({"type": "final_message", "reply": reply, "warnings": response.warnings})
    except Exception:
        error_type = error_type or "stream_failed"
        yield _format_sse(
            {
                "type": "run_failed",
                "errorType": error_type,
                "message": "助手处理失败，请稍后再试。",
                "retryable": True,
            }
        )
    finally:
        audit_log.record(
            endpoint=str(request.url.path),
            status="failed" if error_type else "ok",
            duration_ms=int((perf_counter() - started_at) * 1000),
            device_id_hash=payload.device_id_hash,
            model_provider=model_provider,
            action_types=action_types,
            error_type=error_type,
        )
        if close_client is not None:
            await close_client.aclose()


def _local_context_for_run(payload: AgentRunRequest) -> LocalContext:
    context = payload.local_context
    has_client_context = any(
        [
            context.categories,
            context.accounts,
            context.records,
            context.stats,
            context.scheduled_records,
        ]
    )
    if has_client_context:
        return context
    return LocalContext(
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
        yield _format_sse(event)


def _format_sse(event: dict) -> str:
    return "data: " + json.dumps(event, ensure_ascii=False, separators=(",", ":")) + "\n\n"


def _chunk_text(text: str, size: int = 8) -> list[str]:
    if not text:
        return []
    return [text[index : index + size] for index in range(0, len(text), size)]


def _build_suggestions(payload: AgentSuggestionRequest) -> list[AgentSuggestion]:
    candidates: list[AgentSuggestion] = []
    festival_name = str(payload.local_summary.get("festivalName") or "")
    top_expense_category = str(payload.local_summary.get("topExpenseCategory") or "")
    if festival_name:
        candidates.append(AgentSuggestion(text=f"{festival_name}花销看一下", reason="festival"))
    if payload.local_summary.get("isLikelySalaryDay"):
        candidates.append(AgentSuggestion(text="看看工资到账了吗", reason="salary_day"))
    if payload.local_summary.get("largeExpense") or payload.local_summary.get("hasLargeExpense"):
        candidates.append(AgentSuggestion(text="找出最近的大额消费", reason="spending_change"))
    if payload.today.endswith(("-28", "-29", "-30", "-31")):
        candidates.append(AgentSuggestion(text="月末复盘本月支出", reason="month_end"))
    if payload.today.endswith("-01"):
        candidates.append(AgentSuggestion(text="看看上月结余", reason="month_start"))
    recent_text = " ".join(item.content for item in payload.recent_conversation)
    if "餐饮" in recent_text:
        candidates.append(AgentSuggestion(text="继续看本月餐饮明细", reason="recent_topic"))
    elif top_expense_category:
        candidates.append(AgentSuggestion(text=f"看看{top_expense_category}花了多少", reason="top_category"))
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
