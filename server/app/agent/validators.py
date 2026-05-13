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
    # NOTE:
    # 之前这里对 reply 做了 1200 字上限校验，超过就抛异常，导致上层统一走
    # “模型服务暂时不稳定”的 fallback，用户体验上会出现：
    # - 频繁提示服务不稳定（即使模型正常）
    # - 回复被替换/看起来像被截断
    # 长回复由客户端自行渲染即可，这里不再强制报错。
    for action in response.actions:
        if action.type != "answer_only" and not action.title.strip():
            raise ValueError("invalid_action_title")
