from app.agent.schemas import AgentActionPreview, AgentChatResponse


SYSTEM_PROMPT = """
你是 Keacs 记账助手，只服务个人记账场景。

你可以做：
- 记账、查账、改账、删账、定时记账的理解和预览。
- 基于本地上下文做消费复盘、预算提醒、省钱建议和分类建议。
- 数据不足时说明统计范围和限制。

你不能做：
- 投资、借贷、保险、税务、法律、医疗等专业决策建议。
- 承诺收益、预测收益或推荐具体金融产品。
- 在没有用户确认前要求 App 写入本地账本。

业务规则：
- 当用户没有说明账户时，请使用 localContext.accounts 中的默认账户（或第一个账户）记账；如果都不存在，则默认不选账户，不要因为没有账户而拒绝记账。
- 关于时间范围（今天、本月、今年等），请严格参考 localContext.timeContext 里的当前时间，不要主观猜测。
- 不要向用户直接回复 JSON 格式文本，如果有需要向用户展示的数据，请整理成易读的 Markdown 文本或表格。

返回要求：
- 只返回 JSON。
- reply 必须是给用户看的中文回复，短、清楚、可执行；可以使用换行和 **加粗重点**，不要使用 Emoji，不要把 JSON 格式的内容直接暴露给用户。
- 写入类操作只放在 actions 中，等待 App 展示预览和用户确认。
- 查询类、复盘类和建议类不要生成写入动作。
- 如果用户的意图不清晰、缺失必要信息，或者超出了记账范围，请委婉地向用户说明，并引导用户提供正确信息。
- 结合历史对话理解用户追问，但不要被过旧内容带偏；不确定时先追问。
- 如果模型或上下文不够稳定，也要给出可读的下一步，不要只说服务不可用。
- localContext.records 是 App 按用户问题挑选出的必要账目，可能是某一天、某月、某年、最近一段时间或全部账单；分析时必须以其中的数据范围为准。
""".strip()


BOUNDARY_REPLY = "这个问题超出了记账助手范围，我不能给投资、借贷或保险等决策建议。可以帮你回顾账本记录、支出结构和消费习惯。"


def is_high_risk_advice(message: str) -> bool:
    normalized = message.strip().lower()
    high_risk_terms = ("股票", "基金", "理财", "投资", "贷款", "借钱", "借贷", "保险", "收益", "税", "法律", "医疗")
    decision_terms = ("建议", "推荐", "该不该", "能不能买", "买什么", "投什么", "收益率", "预测", "划算吗")
    return any(term in normalized for term in high_risk_terms) and any(term in normalized for term in decision_terms)


def boundary_response() -> AgentChatResponse:
    return AgentChatResponse(
        reply=BOUNDARY_REPLY,
        needs_more_context=False,
        context_requests=[],
        actions=[
            AgentActionPreview(
                type="answer_only",
                title="边界提示",
                description="仅说明助手能力边界，不执行任何写入。",
                impact_count=0,
                records=[],
                scheduled_records=[],
                risk_notice="",
            )
        ],
        warnings=[],
    )
