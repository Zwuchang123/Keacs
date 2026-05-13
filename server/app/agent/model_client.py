from typing import Any
import json
import re

import httpx
from fastapi import Request

from app.agent.prompts import SYSTEM_PROMPT
from app.agent.schemas import AgentChatRequest
from app.config import Settings


class ModelProviderClient:
    def __init__(self, settings: Settings, http_client: httpx.AsyncClient):
        self.settings = settings
        self.http_client = http_client

    async def generate(self, request: AgentChatRequest, raw_request: Request) -> dict[str, Any]:
        if self.settings.model_provider == "mock":
            return self._mock_response(request)
        if not self.settings.model_base_url or not self.settings.model_api_key:
            raise RuntimeError("model_provider_missing_config")
        try:
            return await self._call_openai_compatible(request, raw_request)
        except httpx.TimeoutException:
            return _timeout_response()
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code == 429:
                return _upstream_limited_response()
            raise

    def _mock_response(self, request: AgentChatRequest) -> dict[str, Any]:
        history_note = "，已结合上下文" if request.conversation_history else ""
        return {
            "reply": f"已收到{history_note}：{request.message[:40]}",
            "needsMoreContext": False,
            "contextRequests": [],
            "actions": [
                {
                    "type": "answer_only",
                    "title": "仅回复",
                    "description": "mock 模型返回的结构化结果。",
                    "impactCount": 0,
                    "records": [],
                    "scheduledRecords": [],
                    "riskNotice": "",
                }
            ],
            "warnings": [],
        }

    async def _call_openai_compatible(self, request: AgentChatRequest, raw_request: Request) -> dict[str, Any]:
        url = self.settings.model_base_url.rstrip("/") + "/chat/completions"
        local_context = json.dumps(
            request.local_context.model_dump(by_alias=True),
            ensure_ascii=False,
            separators=(",", ":"),
        )
        payload = {
            "model": self.settings.model_name,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
            ],
            "temperature": 0.2,
            "max_completion_tokens": self.settings.model_max_completion_tokens,
            "response_format": {"type": "json_object"},
        }
        for turn in request.conversation_history[-24:]:
            payload["messages"].append(
                {
                    "role": turn.role,
                    "content": turn.content,
                }
            )
        payload["messages"].append(
            {
                "role": "user",
                "content": f"用户问题：{request.message}\n本地上下文：{local_context}",
            }
        )
        if self.settings.model_reasoning_split:
            payload["reasoning_split"] = True
        headers = {
            "Authorization": f"Bearer {self.settings.model_api_key}",
            "X-Client-Request-Id": request.client_request_id,
        }
        response = await _post_with_retries(
            client=self.http_client,
            url=url,
            json_body=payload,
            headers=headers,
            max_retries=self.settings.model_max_retries,
            request=raw_request,
        )
        response.raise_for_status()
        data = response.json()
        content = _extract_message_content(data)
        return _parse_model_content(content)


def _parse_model_content(content: str) -> dict[str, Any]:
    normalized = content.strip()
    if normalized.startswith("```"):
        lines = normalized.splitlines()
        if lines and lines[0].strip().startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        normalized = "\n".join(lines).strip()
    normalized = re.sub(r"<think>.*?</think>", "", normalized, flags=re.DOTALL).strip()
    if not normalized:
        return _empty_model_content_response()
    try:
        payload = json.loads(normalized)
    except json.JSONDecodeError:
        payload = _parse_json_fragment(normalized) or {"reply": normalized}
    if not isinstance(payload, dict):
        payload = {
            "reply": "模型结果格式不清晰，请换个说法再试。",
            "warnings": ["模型返回内容不是对象结构。"],
        }
    payload.setdefault("needsMoreContext", False)
    payload.setdefault("contextRequests", [])
    payload.setdefault("actions", [])
    payload.setdefault("warnings", [])
    if not str(payload.get("reply", "")).strip():
        payload["reply"] = _readable_reply_for_actions(payload.get("actions", []))

    # 防止模型将 JSON 暴露给用户
    if isinstance(payload.get("reply"), str):
        reply_str = payload["reply"].strip()
        # 检查 reply 是否以 JSON 对象或数组开始
        starts_with_json = (reply_str.startswith("{") and reply_str.endswith("}")) or (reply_str.startswith("[") and reply_str.endswith("]"))
        if starts_with_json:
            try:
                json.loads(reply_str)
                payload["reply"] = "好的，我已经收到并处理了你的请求。"
            except json.JSONDecodeError:
                pass
        # 检查 reply 中是否嵌入 JSON 对象结构（如 reply: "根据分析...\n{...}\n..."）
        elif _contains_embedded_json(reply_str):
            # 只替换 JSON 部分，保留其他文字
            payload["reply"] = _strip_embedded_json(reply_str)
    return payload


def _extract_message_content(data: dict[str, Any]) -> str:
    choices = data.get("choices")
    if not isinstance(choices, list) or not choices:
        return ""
    first_choice = choices[0]
    if not isinstance(first_choice, dict):
        return ""
    message = first_choice.get("message")
    if not isinstance(message, dict):
        return ""
    content = message.get("content")
    return content if isinstance(content, str) else ""


def _readable_reply_for_actions(actions: Any) -> str:
    if isinstance(actions, list):
        for action in actions:
            if isinstance(action, dict) and action.get("type") not in {"answer_only", "ask_user", None, ""}:
                return "我整理好了待确认内容。请查看下面的预览，确认后才会写入本机账本。"
    return "模型没有返回可展示内容。请换个说法再试。"


def _empty_model_content_response() -> dict[str, Any]:
    return {
        "reply": "模型没有返回可展示内容。请换个说法再试。",
        "needsMoreContext": False,
        "contextRequests": [],
        "actions": [],
        "warnings": ["模型返回为空。"],
    }


def _parse_json_fragment(content: str) -> dict[str, Any] | None:
    start = content.find("{")
    end = content.rfind("}")
    if start == -1 or end == -1 or end <= start:
        return None
    try:
        payload = json.loads(content[start : end + 1])
    except json.JSONDecodeError:
        return None
    return payload if isinstance(payload, dict) else None


def _timeout_response() -> dict[str, Any]:
    return {
        "reply": "我没有拿到稳定的结果。请继续补充金额、日期、分类或账户，我会结合上下文重新判断。",
        "needsMoreContext": False,
        "contextRequests": [],
        "actions": [],
        "warnings": [],
    }


def _upstream_limited_response() -> dict[str, Any]:
    return {
        "reply": "当前请求受限，请稍后再试。",
        "needsMoreContext": False,
        "contextRequests": [],
        "actions": [],
        "warnings": [],
    }


_JSON_OBJECT_PATTERN = re.compile(r"\{\s*[\"\'][a-zA-Z]")


def _contains_embedded_json(text: str) -> bool:
    return bool(_JSON_OBJECT_PATTERN.search(text))


def _strip_embedded_json(text: str) -> str:
    result = text
    while True:
        match = _JSON_OBJECT_PATTERN.search(result)
        if not match:
            break
        start = match.start()
        bracket_count = 0
        in_string = False
        escape = False
        end = start
        for i in range(start, len(result)):
            char = result[i]
            if escape:
                escape = False
                continue
            if char == "\\":
                escape = True
                continue
            if char == '"':
                in_string = not in_string
                continue
            if in_string:
                continue
            if char == "{":
                bracket_count += 1
            elif char == "}":
                bracket_count -= 1
                if bracket_count == 0:
                    end = i + 1
                    break
        result = result[:start].strip() + (" " if result[start:].strip() else "") + result[end:].strip()
        result = re.sub(r"\n{3,}", "\n\n", result).strip()
    return result if result else "好的，我已经收到并处理了你的请求。"


async def _post_with_retries(
    client: httpx.AsyncClient,
    url: str,
    json_body: dict[str, Any],
    headers: dict[str, str],
    max_retries: int,
    request: Request,
) -> httpx.Response:
    last_error: Exception | None = None
    for attempt in range(max_retries + 1):
        try:
            return await client.post(url, json=json_body, headers=headers)
        except (httpx.TimeoutException, httpx.ConnectError, httpx.RemoteProtocolError) as exc:
            last_error = exc
            if await request.is_disconnected() or attempt >= max_retries:
                raise
    if last_error is not None:
        raise last_error
    raise RuntimeError("unexpected_request_retry_state")
