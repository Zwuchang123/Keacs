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
        return {
            "reply": f"已收到：{request.message[:40]}",
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
                {
                    "role": "user",
                    "content": f"用户问题：{request.message}\n本地上下文：{local_context}",
                },
            ],
            "temperature": 0.2,
            "max_completion_tokens": 256,
            "response_format": {"type": "json_object"},
        }
        if self.settings.model_reasoning_split:
            payload["reasoning_split"] = True
        headers = {"Authorization": f"Bearer {self.settings.model_api_key}"}
        async with httpx.AsyncClient(timeout=8) as client:
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
    payload.setdefault("needsMoreContext", False)
    payload.setdefault("contextRequests", [])
    payload.setdefault("actions", [])
    payload.setdefault("warnings", [])
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
        "reply": "模型响应较慢，这次先不继续等待。请稍后再试，或缩短问题内容。",
        "needsMoreContext": False,
        "contextRequests": [],
        "actions": [],
        "warnings": ["模型响应超过 8 秒，已自动停止等待。"],
    }


def _upstream_limited_response() -> dict[str, Any]:
    return {
        "reply": "官方模型服务当前请求受限，请稍后再试。",
        "needsMoreContext": False,
        "contextRequests": [],
        "actions": [],
        "warnings": ["模型服务返回限流提示。"],
    }
