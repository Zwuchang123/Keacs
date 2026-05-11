from typing import Any
import json

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
        return await self._call_openai_compatible(request)

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
            "response_format": {"type": "json_object"},
        }
        if self.settings.model_reasoning_split:
            payload["reasoning_split"] = True
        headers = {"Authorization": f"Bearer {self.settings.model_api_key}"}
        async with httpx.AsyncClient(timeout=30) as client:
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
    payload = json.loads(normalized)
    payload.setdefault("needsMoreContext", False)
    payload.setdefault("contextRequests", [])
    payload.setdefault("actions", [])
    payload.setdefault("warnings", [])
    return payload
