import sqlite3
from uuid import uuid4

from fastapi.testclient import TestClient

from app.config import Settings
from app.agent.model_client import _parse_model_content, _timeout_response, _upstream_limited_response
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
            "categories": [],
            "accounts": [],
            "records": [],
            "stats": {},
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
    assert "没有拿到稳定的结果" in response.json()["reply"]


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
