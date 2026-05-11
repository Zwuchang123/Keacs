from typing import Any
import json
import re

import httpx

from app.agent.prompts import SYSTEM_PROMPT
from app.agent.schemas import AgentChatRequest
from app.config import Settings


class ModelProviderClient:
    def __init__(self, settings: Settings):
        self.settings = settings

    async def generate(self, request: AgentChatRequest) -> dict[str, Any]:
        if self.settings.model_provider == "mock":
            return self._mock_response(request)
        if not self.settings.model_base_url or not self.settings.model_api_key:
            raise RuntimeError("model_provider_missing_config")
        try:
            return await self._call_openai_compatible(request)
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

    async def _call_openai_compatible(self, request: AgentChatRequest) -> dict[str, Any]:
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
            "max_completion_tokens": 256,
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
        headers = {"Authorization": f"Bearer {self.settings.model_api_key}"}
        async with httpx.AsyncClient(timeout=90) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
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

    # 防止模型将 JSON 暴露给用户
    if isinstance(payload.get("reply"), str):
        reply_str = payload["reply"].strip()
        if (reply_str.startswith("{") and reply_str.endswith("}")) or (reply_str.startswith("[") and reply_str.endswith("]")):
            try:
                # 检查是否为合法的 JSON
                json.loads(reply_str)
                payload["reply"] = "好的，我已经收到并处理了你的请求。"
            except json.JSONDecodeError:
                pass
    return payload


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
