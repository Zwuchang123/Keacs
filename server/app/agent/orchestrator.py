from app.agent.model_client import ModelProviderClient
from app.agent.prompts import boundary_response, is_high_risk_advice
from app.agent.schemas import AgentChatRequest, AgentChatResponse
from app.agent.validators import validate_agent_response


class AgentOrchestrator:
    def __init__(self, model_client: ModelProviderClient):
        self.model_client = model_client

    async def handle_chat(self, request: AgentChatRequest) -> AgentChatResponse:
        if is_high_risk_advice(request.message):
            return boundary_response()
        raw_response = await self.model_client.generate(request)
        response = AgentChatResponse.model_validate(raw_response)
        validate_agent_response(response)
        return response
