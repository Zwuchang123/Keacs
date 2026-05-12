from app.agent.schemas import AgentActionPreview, LocalContext
from app.agent.schemas import AgentChatResponse


def count_context_items(context: LocalContext) -> int:
    return (
        len(context.time_context)
        + len(context.categories)
        + len(context.accounts)
        + len(context.records)
        + len(context.scheduled_records)
        + len(context.stats)
    )


def extract_action_types(actions: list[AgentActionPreview]) -> list[str]:
    return [action.type for action in actions]


def validate_agent_response(response: AgentChatResponse) -> None:
    if not response.reply.strip():
        raise ValueError("empty_reply")
    if len(response.reply) > 1200:
        raise ValueError("reply_too_long")
    for action in response.actions:
        if action.type != "answer_only" and not action.title.strip():
            raise ValueError("invalid_action_title")
