from app.agent.schemas import AgentActionPreview, LocalContext


def count_context_items(context: LocalContext) -> int:
    return (
        len(context.categories)
        + len(context.accounts)
        + len(context.records)
        + len(context.scheduled_records)
        + len(context.stats)
    )


def extract_action_types(actions: list[AgentActionPreview]) -> list[str]:
    return [action.type for action in actions]
