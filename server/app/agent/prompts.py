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

返回要求：
- 只返回 JSON。
- reply 必须是给用户看的中文短回复。
- 写入类操作只放在 actions 中，等待 App 展示预览和用户确认。
- 查询类、复盘类和建议类不要生成写入动作。
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
