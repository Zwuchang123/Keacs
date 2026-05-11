from typing import Any
import json

import httpx

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
                {
                    "role": "system",
                    "content": "你是 Keacs 记账助手。查询类问题只根据本地上下文回答，写入类问题只生成待确认预览。只返回 JSON。",
                },
                {
                    "role": "user",
                    "content": f"用户问题：{request.message}\n本地上下文：{local_context}",
                },
            ],
            "temperature": 0.2,
            "response_format": {"type": "json_object"},
        }
        headers = {"Authorization": f"Bearer {self.settings.model_api_key}"}
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
        return httpx.Response(200, content=content).json()
