import sqlite3
from uuid import uuid4

import httpx
import pytest
from fastapi.testclient import TestClient

from app.agent.model_client import ModelProviderClient
from app.agent.orchestrator import AgentOrchestrator
from app.config import Settings
from app.agent.model_client import _parse_model_content, _timeout_response, _upstream_limited_response
from app.agent.schemas import AgentChatRequest
from app.main import create_app


def _client(tmp_path, per_minute_limit: int = 20) -> TestClient:
    settings = Settings(
        request_per_minute_limit=per_minute_limit,
        request_per_day_limit=200,
        audit_db_path=str(tmp_path / "audit.sqlite3"),
    )
    return TestClient(create_app(settings))


def _payload(message: str = "这个月花了多少") -> dict:
    return {
        "clientRequestId": str(uuid4()),
        "deviceIdHash": "abcdef1234567890abcdef",
        "message": message,
        "localContext": {
            "timeContext": {"today": "2026-05-12", "thisMonth": "2026-05", "thisYear": "2026"},
            "categories": [
                {"name": "餐饮", "direction": "EXPENSE"},
                {"name": "交通", "direction": "EXPENSE"},
                {"name": "住房", "direction": "EXPENSE"},
                {"name": "医疗", "direction": "EXPENSE"},
                {"name": "其他", "direction": "EXPENSE"},
                {"name": "工资", "direction": "INCOME"},
                {"name": "报销", "direction": "INCOME"},
                {"name": "其他", "direction": "INCOME"},
            ],
            "accounts": [
                {"name": "微信", "balanceCent": 100000},
                {"name": "银行卡", "balanceCent": 500000},
            ],
            "records": [
                {
                    "id": 1,
                    "type": "EXPENSE",
                    "amountCent": 1800,
                    "occurredAt": 1778518800000,
                    "date": "2026-05-11",
                    "categoryName": "餐饮",
                    "fromAccountName": "微信",
                    "note": "午饭",
                }
            ],
            "stats": {
                "rangeLabel": "本月",
                "incomeCent": 800000,
                "expenseCent": 123400,
                "balanceCent": 676600,
                "categoryTotals": [
                    {"categoryName": "餐饮", "type": "EXPENSE", "amountCent": 45600},
                    {"categoryName": "交通", "type": "EXPENSE", "amountCent": 12000},
                ],
            },
            "scheduledRecords": [],
        },
        "timezone": "Asia/Shanghai",
        "appVersion": "1.2.2",
    }


def test_health_returns_ok(tmp_path):
    client = _client(tmp_path)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_ready_returns_ok_for_mock_mode(tmp_path):
    client = _client(tmp_path)

    response = client.get("/ready")

    assert response.status_code == 200
    assert response.json()["status"] == "ready"
    assert response.json()["checks"]["audit_log"] == "ok"
    assert response.json()["checks"]["model_provider"] == "mock"


def test_ready_returns_503_when_upstream_config_missing(tmp_path):
    settings = Settings(
        model_provider="openai_compatible",
        audit_db_path=str(tmp_path / "audit.sqlite3"),
    )
    client = TestClient(create_app(settings))

    response = client.get("/ready")

    assert response.status_code == 503
    assert response.json()["status"] == "not_ready"
    assert response.json()["checks"]["model_provider"] == "missing_config"


def test_chat_uses_mock_model_and_returns_structured_result(tmp_path):
    client = _client(tmp_path)

    response = client.post("/api/agent/chat", json=_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["reply"]
    assert body["needsMoreContext"] is False
    assert body["actions"][0]["type"] == "answer_only"


def test_chat_accepts_conversation_history(tmp_path):
    client = _client(tmp_path)
    payload = _payload("继续分析")
    payload["conversationHistory"] = [
        {"role": "user", "content": "这个月花了多少"},
        {"role": "assistant", "content": "本月支出 18 元"},
    ]

    response = client.post("/api/agent/chat", json=payload)

    assert response.status_code == 200
    assert "已结合上下文" in response.json()["reply"]


def test_model_failure_returns_readable_fallback(tmp_path):
    settings = Settings(
        model_provider="openai_compatible",
        audit_db_path=str(tmp_path / "audit.sqlite3"),
    )
    client = TestClient(create_app(settings))

    response = client.post("/api/agent/chat", json=_payload("昨天午饭 18 微信"))

    assert response.status_code == 200
    body = response.json()
    assert "请确认" in body["reply"]
    assert body["actions"][0]["type"] == "create_record"


@pytest.mark.anyio
async def test_orchestrator_replaces_unclear_model_reply_for_record_creation():
    class _FakeRequest:
        async def is_disconnected(self) -> bool:
            return False

    class _FakeModelClient:
        async def generate(self, request, raw_request):
            return {
                "reply": "抱歉，您的消息似乎出现了乱码，我没能理解您的意思。",
                "needsMoreContext": False,
                "contextRequests": [],
                "actions": [],
                "warnings": [],
            }

    request = AgentChatRequest.model_validate(_payload("昨天午饭 18 微信"))
    response = await AgentOrchestrator(_FakeModelClient()).handle_chat(request, _FakeRequest())

    assert "请确认" in response.reply
    assert response.actions[0].type == "create_record"


@pytest.mark.anyio
async def test_model_client_parses_openai_compatible_response(tmp_path):
    class _FakeRequest:
        async def is_disconnected(self) -> bool:
            return False

    async def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={
                "choices": [
                    {
                        "message": {
                            "content": '{"reply":"ok","actions":[],"warnings":[]}',
                        }
                    }
                ]
            },
        )

    settings = Settings(
        model_provider="openai_compatible",
        model_base_url="https://example.com/v1",
        model_api_key="secret",
        audit_db_path=str(tmp_path / "audit.sqlite3"),
    )
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as http_client:
        model_client = ModelProviderClient(settings, http_client)
        request = AgentChatRequest.model_validate(_payload("昨天午饭 18 微信"))
        response = await model_client.generate(
            request=request,
            raw_request=_FakeRequest(),
        )

    assert response["reply"] == "ok"
    assert response["actions"] == []


def test_chat_rejects_empty_message(tmp_path):
    client = _client(tmp_path)

    response = client.post("/api/agent/chat", json=_payload("   "))

    assert response.status_code == 422


def test_rate_limit_returns_clear_error(tmp_path):
    client = _client(tmp_path, per_minute_limit=1)

    first = client.post("/api/agent/chat", json=_payload())
    second = client.post("/api/agent/chat", json=_payload())

    assert first.status_code == 200
    assert second.status_code == 429
    assert "请求过于频繁" in second.json()["detail"]


def test_audit_log_does_not_store_message_or_api_key(tmp_path):
    db_path = tmp_path / "audit.sqlite3"
    settings = Settings(audit_db_path=str(db_path), model_api_key="secret-key")
    client = TestClient(create_app(settings))

    response = client.post("/api/agent/chat", json=_payload("昨天午饭 18 微信"))

    assert response.status_code == 200
    with sqlite3.connect(db_path) as connection:
        rows = connection.execute("SELECT endpoint, status, model_provider, action_types FROM audit_logs").fetchall()
    stored_text = str(rows)
    assert "/api/agent/chat" in stored_text
    assert "昨天午饭" not in stored_text
    assert "secret-key" not in stored_text


def test_high_risk_financial_advice_returns_boundary_message(tmp_path):
    client = _client(tmp_path)

    response = client.post("/api/agent/chat", json=_payload("我该不该买股票，推荐哪只收益高？"))

    assert response.status_code == 200
    body = response.json()
    assert "不能给投资" in body["reply"]
    assert body["actions"][0]["type"] == "answer_only"


def test_model_content_accepts_json_code_block():
    payload = _parse_model_content(
        """```json
{"reply":"ok","actions":[]}
```"""
    )

    assert payload["reply"] == "ok"
    assert payload["needsMoreContext"] is False
    assert payload["contextRequests"] == []
    assert payload["warnings"] == []


def test_model_content_falls_back_to_reply_text():
    payload = _parse_model_content("可以，我来帮你看看本月支出。")

    assert payload["reply"] == "可以，我来帮你看看本月支出。"
    assert payload["needsMoreContext"] is False
    assert payload["actions"] == []


def test_model_content_extracts_json_after_think_block():
    payload = _parse_model_content("<think>分析过程</think>\n\n{\"reply\":\"ok\"}")

    assert payload["reply"] == "ok"
    assert payload["needsMoreContext"] is False


def test_model_content_empty_reply_gets_readable_message():
    payload = _parse_model_content('{"reply":"","actions":[{"type":"create_record","title":"新增账目"}]}')

    assert "待确认" in payload["reply"]


def test_model_timeout_returns_fast_user_message():
    payload = _timeout_response()

    assert "没有拿到稳定的结果" in payload["reply"]
    assert payload["needsMoreContext"] is False
    assert payload["actions"] == []
    assert payload["warnings"] == []


def test_model_limit_returns_clear_user_message():
    payload = _upstream_limited_response()

    assert "请求受限" in payload["reply"]
    assert payload["needsMoreContext"] is False
    assert payload["actions"] == []


def test_chat_returns_content_for_200_agent_questions_and_long_history(tmp_path):
    settings = Settings(
        model_provider="openai_compatible",
        request_per_minute_limit=1000,
        request_per_day_limit=1000,
        audit_db_path=str(tmp_path / "audit.sqlite3"),
    )
    client = TestClient(create_app(settings))
    questions = _agent_test_questions()
    assert len(questions) == 200
    assert len(set(questions)) == 200

    long_history = [
        {"role": "user" if index % 2 == 0 else "assistant", "content": f"第 {index} 轮上下文，继续围绕账本分析。"}
        for index in range(30)
    ]
    passed = 0
    for index, question in enumerate(questions):
        payload = _payload(question)
        payload["deviceIdHash"] = f"abcdef1234567890{index:04d}"
        if index >= 170:
            payload["conversationHistory"] = long_history
        response = client.post("/api/agent/chat", json=payload)
        assert response.status_code == 200
        body = response.json()
        assert body["reply"].strip()
        assert not body["reply"].lstrip().startswith("{")
        passed += 1
    assert passed == 200


def test_run_stream_emits_ordered_task_events(tmp_path):
    client = _client(tmp_path)
    payload = {
        "clientRequestId": str(uuid4()),
        "deviceIdHash": "abcdef1234567890abcdef",
        "message": "昨天午饭 18 微信",
        "conversationHistory": [],
        "timezone": "Asia/Shanghai",
        "appVersion": "1.4.2",
    }

    with client.stream("POST", "/api/agent/runs/stream", json=payload) as response:
        lines = [line for line in response.iter_lines() if line.startswith("data: ")]

    assert response.status_code == 200
    event_types = [line.split('"type":"', 1)[1].split('"', 1)[0] for line in lines]
    assert event_types[:3] == ["run_started", "stage_changed", "context_requested"]
    assert "action_preview" in event_types
    assert "awaiting_confirmation" in event_types


def test_run_context_resume_feedback_and_suggestions(tmp_path):
    client = _client(tmp_path)
    run_id = str(uuid4())
    context_response = client.post(
        f"/api/agent/runs/{run_id}/context",
        json={
            "clientRequestId": str(uuid4()),
            "deviceIdHash": "abcdef1234567890abcdef",
            "observations": [
                {"status": "success", "summary": "已读取本月账目", "data": {}, "nextActions": [], "artifacts": []}
            ],
        },
    )
    resume_response = client.post(
        f"/api/agent/runs/{run_id}/resume",
        json={
            "clientRequestId": str(uuid4()),
            "deviceIdHash": "abcdef1234567890abcdef",
            "decision": "cancelled",
            "actionResults": [],
            "errorType": "",
        },
    )
    feedback_response = client.post(
        f"/api/agent/runs/{run_id}/feedback",
        json={
            "clientRequestId": str(uuid4()),
            "deviceIdHash": "abcdef1234567890abcdef",
            "messageId": "m1",
            "feedback": "like",
            "reason": "",
        },
    )
    suggestions_response = client.post(
        "/api/agent/suggestions",
        json={
            "deviceIdHash": "abcdef1234567890abcdef",
            "today": "2026-05-31",
            "timezone": "Asia/Shanghai",
            "recentConversation": [{"role": "user", "content": "本月餐饮花了多少"}],
            "localSummary": {"largeExpense": True},
            "limit": 4,
        },
    )

    assert context_response.status_code == 200
    assert resume_response.status_code == 200
    assert feedback_response.status_code == 200
    assert suggestions_response.status_code == 200
    suggestions = suggestions_response.json()["suggestions"]
    assert 2 <= len(suggestions) <= 4
    assert len({item["text"] for item in suggestions}) == len(suggestions)


def _agent_test_questions() -> list[str]:
    creation_templates = [
        "今天午饭 {amount} 元 微信",
        "昨天晚饭 {amount} 元 微信",
        "记一笔早餐 {amount} 元",
        "买咖啡花了 {amount} 元",
        "地铁支出 {amount} 元",
        "打车 {amount} 元 银行卡",
        "房租 {amount} 元 银行卡",
        "报销到账 {amount} 元 银行卡",
        "工资收入 {amount} 元 银行卡",
        "从微信转 {amount} 元到银行卡",
    ]
    analysis_templates = [
        "这个月花了多少",
        "本月餐饮支出多少",
        "最近7天消费复盘",
        "最近30天有哪些大额支出",
        "最近三个月消费结构",
        "最近半年支出趋势",
        "最近一年账单分析",
        "今年收入支出总结",
        "去年餐饮和交通对比",
        "全部账单帮我分析消费习惯",
    ]
    operation_templates = [
        "删除昨天午饭",
        "删掉重复的午饭",
        "把昨天午饭改成 {amount} 元",
        "把那笔打车改成交通",
        "停用房租定时记账",
        "每月1号房租 {amount} 元",
        "每周一早餐 {amount} 元",
        "本月哪些支出异常",
        "所有历史账单总共花了多少",
        "从开始到现在结余多少",
    ]
    boundary_templates = [
        "推荐一只收益高的股票",
        "我该不该买基金",
        "贷款买车划算吗",
        "保险怎么买最合适",
        "帮我做税务筹划",
        "医疗报销怎么处理",
        "随便聊聊天",
        "继续分析",
        "这笔是什么分类",
        "没有金额也能记账吗",
    ]
    questions: list[str] = []
    for index in range(5):
        for template in creation_templates:
            questions.append(template.format(amount=10 + index * 3))
        for template in analysis_templates:
            questions.append(f"{template}，第{index + 1}次")
        for template in operation_templates:
            questions.append(f"{template.format(amount=20 + index * 5)}，第{index + 1}次")
        for template in boundary_templates:
            questions.append(f"{template}，第{index + 1}次")
    return questions
