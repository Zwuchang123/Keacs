from __future__ import annotations

from calendar import monthrange
from datetime import datetime, time, timedelta, timezone
import re
from typing import Any

from app.agent.schemas import AgentActionPreview, AgentChatRequest, AgentChatResponse


EXPENSE = "EXPENSE"
INCOME = "INCOME"
TRANSFER = "TRANSFER"


def build_fallback_response(request: AgentChatRequest, warning: str = "") -> AgentChatResponse:
    message = request.message.strip()
    response = (
        _build_record_operation_response(request, message)
        or _build_scheduled_response(request, message)
        or _build_record_creation_response(request, message)
        or _build_stats_response(request, message)
        or _build_general_response(message)
    )
    if warning:
        response.warnings.append(warning)
    return response


def should_replace_model_response(request: AgentChatRequest, response: AgentChatResponse) -> bool:
    if any(action.type not in {"answer_only", "ask_user"} for action in response.actions):
        return False
    message = request.message.strip()
    if _extract_amount_cent(message) is not None and _looks_like_record_creation(message):
        return True
    if _looks_like_agent_task(message) and _looks_like_generic_model_reply(response.reply):
        return True
    return False


def _build_record_creation_response(request: AgentChatRequest, message: str) -> AgentChatResponse | None:
    amount_cent = _extract_amount_cent(message)
    if amount_cent is None or not _looks_like_record_creation(message):
        return None
    if _looks_like_analysis_request(message):
        return None
    if _contains_any(message, ("明天", "后天", "下周", "下月", "下个月", "明年")):
        return _ask_user_response("日期不能是未来时间", "请换成今天或过去日期后再发送。")

    record_type = _resolve_record_type(message)
    if record_type == TRANSFER:
        return _build_transfer_response(request, message, amount_cent)

    category_name = _resolve_category_name(request, record_type, message)
    if not category_name:
        return _ask_user_response("还缺少分类", "请补充这笔账目的分类，例如餐饮、交通或工资。")
    account_name = _resolve_account_name(request, message)
    occurred_at, date_label = _resolve_occurred_at(request, message)
    record: dict[str, Any] = {
        "type": record_type,
        "amountCent": amount_cent,
        "occurredAt": occurred_at,
        "date": date_label,
        "categoryName": category_name,
        "note": message[:40],
    }
    if record_type == INCOME:
        if account_name:
            record["toAccountName"] = account_name
    elif account_name:
        record["fromAccountName"] = account_name

    type_label = "收入" if record_type == INCOME else "支出"
    return AgentChatResponse(
        reply=f"请确认这笔{type_label}。\n\n确认后才会保存到本机账本。",
        needs_more_context=False,
        context_requests=[],
        actions=[
            AgentActionPreview(
                type="create_record",
                title="新增账目",
                description=f"新增 1 笔{type_label}账目。",
                impact_count=1,
                records=[record],
                scheduled_records=[],
                risk_notice="请核对金额、日期、分类和账户。",
            )
        ],
        warnings=[],
    )


def _build_transfer_response(
    request: AgentChatRequest,
    message: str,
    amount_cent: int,
) -> AgentChatResponse | None:
    matched_accounts = _matched_account_names(request, message)
    if len(matched_accounts) < 2:
        return _ask_user_response("还缺少转账账户", "请说明转出账户和转入账户，例如“从微信转 200 到银行卡”。")
    occurred_at, date_label = _resolve_occurred_at(request, message)
    return AgentChatResponse(
        reply="请确认这笔转账。\n\n确认后才会更新本机账户余额。",
        needs_more_context=False,
        context_requests=[],
        actions=[
            AgentActionPreview(
                type="create_record",
                title="新增转账",
                description="新增 1 笔转账账目。",
                impact_count=1,
                records=[
                    {
                        "type": TRANSFER,
                        "amountCent": amount_cent,
                        "occurredAt": occurred_at,
                        "date": date_label,
                        "fromAccountName": matched_accounts[0],
                        "toAccountName": matched_accounts[1],
                        "note": message[:40],
                    }
                ],
                scheduled_records=[],
                risk_notice="请核对转出账户、转入账户和金额。",
            )
        ],
        warnings=[],
    )


def _build_stats_response(request: AgentChatRequest, message: str) -> AgentChatResponse | None:
    if not _contains_any(message, ("多少", "花了", "支出", "收入", "结余", "总结", "复盘", "本月", "这个月", "最近", "分析")):
        return None
    stats = request.local_context.stats
    range_label = str(stats.get("rangeLabel") or "本次范围")
    income = _as_int(stats.get("incomeCent")) or 0
    expense = _as_int(stats.get("expenseCent")) or 0
    balance = _as_int(stats.get("balanceCent"))
    if balance is None:
        balance = income - expense
    reply = f"{range_label}账本摘要：\n\n收入：{_format_money(income)}\n支出：{_format_money(expense)}\n结余：{_format_money(balance)}"
    category_totals = stats.get("categoryTotals")
    if isinstance(category_totals, list) and category_totals:
        top_items = []
        for item in category_totals[:3]:
            if isinstance(item, dict):
                name = item.get("categoryName") or "未分类"
                amount = _as_int(item.get("amountCent")) or 0
                top_items.append(f"{name} {_format_money(amount)}")
        if top_items:
            reply += "\n\n分类参考：" + "、".join(top_items)
    return AgentChatResponse(
        reply=reply,
        needs_more_context=False,
        context_requests=[],
        actions=[],
        warnings=[],
    )


def _build_record_operation_response(request: AgentChatRequest, message: str) -> AgentChatResponse | None:
    records = request.local_context.records
    if _contains_any(message, ("删", "删除", "去掉")):
        if not records:
            return _ask_user_response("没有找到可删除的账目", "请补充日期、金额、分类或账户后再试。")
        if len(records) == 1:
            record = records[0]
            return AgentChatResponse(
                reply="请确认是否删除这笔账目。\n\n删除前请核对明细。",
                needs_more_context=False,
                context_requests=[],
                actions=[
                    AgentActionPreview(
                        type="delete_record",
                        title="删除账目",
                        description="删除 1 笔账目。",
                        impact_count=1,
                        records=[record],
                        scheduled_records=[],
                        risk_notice="删除后无法在助手里直接恢复。",
                    )
                ],
                warnings=[],
            )
        return _ask_user_response("匹配到多笔账目", "请先说明要处理哪一笔，或补充更明确的日期、金额、分类。")
    if _contains_any(message, ("改", "修改", "调成", "改成")):
        if not records:
            return _ask_user_response("没有找到可修改的账目", "请补充日期、金额、分类或账户后再试。")
        if len(records) > 1:
            return _ask_user_response("匹配到多笔账目", "请先说明要修改哪一笔，或补充更明确的日期、金额、分类。")
        updated_record = _record_with_updates(request, records[0], message)
        if updated_record is None:
            return _ask_user_response("还缺少修改内容", "请说明要改金额、分类、账户、日期还是备注。")
        return AgentChatResponse(
            reply="请确认这笔修改。\n\n确认后才会更新本机账本。",
            needs_more_context=False,
            context_requests=[],
            actions=[
                AgentActionPreview(
                    type="update_record",
                    title="修改账目",
                    description="修改 1 笔账目。",
                    impact_count=1,
                    records=[updated_record],
                    scheduled_records=[],
                    risk_notice="请核对修改后的金额、日期、分类和账户。",
                )
            ],
            warnings=[],
        )
    return None


def _build_scheduled_response(request: AgentChatRequest, message: str) -> AgentChatResponse | None:
    if not _contains_any(message, ("定时", "每周", "每月", "每年", "周期", "房租")):
        return None
    amount_cent = _extract_amount_cent(message)
    if _contains_any(message, ("停用", "关闭", "取消")):
        schedules = request.local_context.scheduled_records
        if not schedules:
            return _ask_user_response("没有找到定时记账", "请补充定时记账名称、金额或周期后再试。")
        if len(schedules) > 1:
            return _ask_user_response("匹配到多条定时记账", "请说明要停用哪一条定时记账。")
        return AgentChatResponse(
            reply="请确认是否停用这条定时记账。\n\n确认后不再自动生成。",
            needs_more_context=False,
            context_requests=[],
            actions=[
                AgentActionPreview(
                    type="disable_scheduled_record",
                    title="停用定时记账",
                    description="停用 1 条定时记账。",
                    impact_count=1,
                    records=[],
                    scheduled_records=[schedules[0]],
                    risk_notice="停用后可在定时记账页重新开启。",
                )
            ],
            warnings=[],
        )
    if amount_cent is None:
        return _ask_user_response("还缺少金额", "请补充定时记账的金额。")
    frequency = _resolve_frequency(message)
    if frequency is None:
        return _ask_user_response("还缺少定时规则", "请补充周期和生成日期，例如“每月 1 号房租 2500”。")
    record_type = _resolve_record_type(message)
    category_name = _resolve_category_name(request, record_type, message)
    if record_type != TRANSFER and not category_name:
        return _ask_user_response("还缺少分类", "请补充这条定时记账的分类。")
    account_name = _resolve_account_name(request, message)
    next_run_at = _resolve_next_run_at(request, message, frequency)
    schedule: dict[str, Any] = {
        "type": record_type,
        "amountCent": amount_cent,
        "frequency": frequency,
        "nextRunAt": next_run_at,
        "note": message[:40],
        "isEnabled": True,
    }
    if record_type == TRANSFER:
        matched_accounts = _matched_account_names(request, message)
        if len(matched_accounts) < 2:
            return _ask_user_response("还缺少转账账户", "请说明转出账户和转入账户。")
        schedule["fromAccountName"] = matched_accounts[0]
        schedule["toAccountName"] = matched_accounts[1]
    else:
        schedule["categoryName"] = category_name
        if record_type == INCOME:
            if account_name:
                schedule["toAccountName"] = account_name
        elif account_name:
            schedule["fromAccountName"] = account_name
    return AgentChatResponse(
        reply="请确认这条定时记账。\n\n确认后会按周期自动生成账目。",
        needs_more_context=False,
        context_requests=[],
        actions=[
            AgentActionPreview(
                type="create_scheduled_record",
                title="新增定时记账",
                description="新增 1 条定时记账。",
                impact_count=1,
                records=[],
                scheduled_records=[schedule],
                risk_notice="请核对金额、周期、分类、账户和下次生成时间。",
            )
        ],
        warnings=[],
    )


def _build_general_response(message: str) -> AgentChatResponse:
    if _contains_any(message, ("股票", "基金", "投资", "贷款", "保险", "税", "法律", "医疗")):
        return AgentChatResponse(
            reply="这个问题超出了记账助手范围。我可以帮你回顾账本记录、支出结构和消费习惯。",
            needs_more_context=False,
            context_requests=[],
            actions=[],
            warnings=[],
        )
    return AgentChatResponse(
        reply="我可以帮你记账、查账、改账、删账和做消费复盘。请补充金额、日期、分类或账户，我会继续判断。",
        needs_more_context=False,
        context_requests=[],
        actions=[],
        warnings=[],
    )


def _ask_user_response(title: str, description: str) -> AgentChatResponse:
    return AgentChatResponse(
        reply=f"{title}。\n\n{description}",
        needs_more_context=False,
        context_requests=[],
        actions=[
            AgentActionPreview(
                type="ask_user",
                title=title,
                description=description,
                impact_count=0,
                records=[],
                scheduled_records=[],
                risk_notice="",
            )
        ],
        warnings=[],
    )


def _resolve_record_type(message: str) -> str:
    if _contains_any(message, ("转账", "转到", "转入", "转出", "从")) and "转" in message:
        return TRANSFER
    if _contains_any(message, ("收入", "工资", "奖金", "报销", "兼职", "到账", "收款")):
        return INCOME
    return EXPENSE


def _record_with_updates(
    request: AgentChatRequest,
    record: dict[str, Any],
    message: str,
) -> dict[str, Any] | None:
    updated = dict(record)
    changed = False
    amount_cent = _extract_update_amount_cent(message)
    if amount_cent is not None:
        updated["amountCent"] = amount_cent
        changed = True
    category_name = _resolve_target_category_name(request, str(updated.get("type") or EXPENSE), message)
    if category_name:
        updated["categoryName"] = category_name
        changed = True
    account_name = _resolve_target_account_name(request, message)
    if account_name:
        if str(updated.get("type")) == INCOME:
            updated["toAccountName"] = account_name
        else:
            updated["fromAccountName"] = account_name
        changed = True
    occurred_at, date_label = _resolve_update_date(request, message)
    if occurred_at is not None:
        updated["occurredAt"] = occurred_at
        updated["date"] = date_label
        changed = True
    return updated if changed else None


def _extract_update_amount_cent(message: str) -> int | None:
    match = re.search(r"(?:改成|调成|改为|变成|改到)\s*(\d+(?:\.\d{1,2})?)\s*(?:元|块)?", message)
    if match:
        amount = round(float(match.group(1)) * 100)
        return amount if amount > 0 else None
    return _extract_amount_cent(message)


def _resolve_target_category_name(request: AgentChatRequest, record_type: str, message: str) -> str:
    direction = INCOME if record_type == INCOME else EXPENSE
    names = [
        str(item.get("name"))
        for item in request.local_context.categories
        if str(item.get("direction", "")).upper() == direction and item.get("name")
    ]
    tail = _text_after_update_keyword(message)
    for name in names:
        if name and name in tail:
            return name
    return ""


def _resolve_target_account_name(request: AgentChatRequest, message: str) -> str:
    tail = _text_after_update_keyword(message)
    for item in request.local_context.accounts:
        name = item.get("name")
        if name and str(name) in tail:
            return str(name)
    return ""


def _text_after_update_keyword(message: str) -> str:
    for keyword in ("改成", "调成", "改为", "变成", "改到"):
        if keyword in message:
            return message.split(keyword, 1)[1]
    return message


def _resolve_update_date(request: AgentChatRequest, message: str) -> tuple[int | None, str]:
    if not _contains_any(message, ("今天", "昨天", "昨日", "前天")):
        return None, ""
    return _resolve_occurred_at(request, message)


def _resolve_frequency(message: str) -> str | None:
    if _contains_any(message, ("每周", "周一", "周二", "周三", "周四", "周五", "周六", "周日", "星期")):
        return "WEEKLY"
    if _contains_any(message, ("每年", "每一年", "每逢")):
        return "YEARLY"
    if _contains_any(message, ("每月", "每个月", "月初", "月末", "房租")):
        return "MONTHLY"
    return None


def _resolve_next_run_at(request: AgentChatRequest, message: str, frequency: str) -> int:
    today_text = request.local_context.time_context.get("today") or datetime.now().strftime("%Y-%m-%d")
    base_date = datetime.strptime(today_text, "%Y-%m-%d").date()
    timezone_value = _safe_timezone(request.timezone)
    if frequency == "WEEKLY":
        target_weekday = _resolve_weekday(message)
        days = (target_weekday - base_date.weekday()) % 7
        if days == 0:
            days = 7
        next_date = base_date + timedelta(days=days)
    elif frequency == "YEARLY":
        next_date = base_date.replace(year=base_date.year + 1)
    else:
        day = _resolve_month_day(message)
        year = base_date.year
        month = base_date.month
        if base_date.day >= day:
            month += 1
            if month > 12:
                month = 1
                year += 1
        next_date = base_date.replace(year=year, month=month, day=min(day, monthrange(year, month)[1]))
    return int(datetime.combine(next_date, time(hour=9), tzinfo=timezone_value).timestamp() * 1000)


def _resolve_month_day(message: str) -> int:
    match = re.search(r"(\d{1,2})\s*号", message)
    if match:
        return max(1, min(28, int(match.group(1))))
    if "月末" in message:
        return 28
    return 1


def _resolve_weekday(message: str) -> int:
    mapping = {
        "一": 0,
        "二": 1,
        "三": 2,
        "四": 3,
        "五": 4,
        "六": 5,
        "日": 6,
        "天": 6,
    }
    for key, value in mapping.items():
        if f"周{key}" in message or f"星期{key}" in message:
            return value
    return 0


def _looks_like_record_creation(message: str) -> bool:
    return _contains_any(message, ("记", "花", "买", "饭", "餐", "工资", "收入", "支出", "转", "报销", "消费", "付款", "收款"))


def _looks_like_agent_task(message: str) -> bool:
    return (
        _looks_like_record_creation(message)
        or _contains_any(message, ("多少", "花了", "支出", "收入", "结余", "复盘", "分析", "全部", "历史", "总结"))
        or _contains_any(message, ("改", "删", "删除", "定时", "每周", "每月", "每年", "房租"))
    )


def _looks_like_generic_model_reply(reply: str) -> bool:
    return _contains_any(reply, ("已收到", "mock 模型", "仅回复", "结构化结果"))


def _looks_like_analysis_request(message: str) -> bool:
    return _contains_any(message, ("多少", "复盘", "分析", "总结", "趋势", "排行", "异常", "结构"))


def _resolve_category_name(request: AgentChatRequest, record_type: str, message: str) -> str:
    direction = INCOME if record_type == INCOME else EXPENSE
    names = [
        str(item.get("name"))
        for item in request.local_context.categories
        if str(item.get("direction", "")).upper() == direction and item.get("name")
    ]
    for name in names:
        if name and name in message:
            return name
    keyword_map = {
        EXPENSE: [
            (("饭", "餐", "早餐", "午饭", "晚饭", "咖啡", "奶茶", "外卖"), "餐饮"),
            (("打车", "地铁", "公交", "车费", "加油"), "交通"),
            (("房租", "租房", "物业"), "住房"),
            (("水电", "燃气", "电费", "水费"), "水电煤"),
            (("药", "医院", "门诊"), "医疗"),
            (("电影", "游戏", "娱乐"), "娱乐"),
        ],
        INCOME: [
            (("工资", "薪资", "发薪"), "工资"),
            (("奖金",), "奖金"),
            (("报销",), "报销"),
            (("兼职",), "兼职"),
        ],
    }
    for keywords, category in keyword_map.get(direction, []):
        if _contains_any(message, keywords) and category in names:
            return category
    if "其他" in names:
        return "其他"
    return names[0] if names else ""


def _resolve_account_name(request: AgentChatRequest, message: str) -> str:
    names = _matched_account_names(request, message)
    if names:
        return names[0]
    for item in request.local_context.accounts:
        if item.get("isDefaultRecordAccount") is True and item.get("name"):
            return str(item["name"])
    default_name = request.local_context.stats.get("defaultRecordAccountName")
    if default_name:
        return str(default_name)
    return ""


def _matched_account_names(request: AgentChatRequest, message: str) -> list[str]:
    names: list[str] = []
    for item in request.local_context.accounts:
        name = item.get("name")
        if name and str(name) in message:
            names.append(str(name))
    return names


def _resolve_occurred_at(request: AgentChatRequest, message: str) -> tuple[int, str]:
    today_text = request.local_context.time_context.get("today") or datetime.now().strftime("%Y-%m-%d")
    base_date = datetime.strptime(today_text, "%Y-%m-%d").date()
    if _contains_any(message, ("昨天", "昨日")):
        base_date -= timedelta(days=1)
    elif "前天" in message:
        base_date -= timedelta(days=2)
    timezone = _safe_timezone(request.timezone)
    occurred = datetime.combine(base_date, time(hour=9), tzinfo=timezone)
    return int(occurred.timestamp() * 1000), base_date.isoformat()


def _safe_timezone(name: str) -> timezone:
    if name == "Asia/Shanghai":
        return timezone(timedelta(hours=8))
    return timezone.utc


def _extract_amount_cent(message: str) -> int | None:
    for match in re.finditer(r"(\d+(?:\.\d{1,2})?)\s*(?:元|块)?", message):
        next_text = message[match.end() : match.end() + 1]
        if next_text == "号":
            continue
        amount = round(float(match.group(1)) * 100)
        if amount > 0:
            return amount
    return None


def _as_int(value: Any) -> int | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    if isinstance(value, str):
        try:
            return int(value)
        except ValueError:
            return None
    return None


def _format_money(amount_cent: int) -> str:
    amount = amount_cent / 100
    return f"¥{amount:,.2f}".rstrip("0").rstrip(".")


def _contains_any(message: str, terms: tuple[str, ...]) -> bool:
    return any(term in message for term in terms)
