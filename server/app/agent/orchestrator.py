from app.agent.model_client import ModelProviderClient
from app.agent.schemas import AgentChatRequest, AgentChatResponse


class AgentOrchestrator:
    def __init__(self, model_client: ModelProviderClient):
        self.model_client = model_client

    async def handle_chat(self, request: AgentChatRequest) -> AgentChatResponse:
        raw_response = await self.model_client.generate(request)
        return AgentChatResponse.model_validate(raw_response)
