import sqlite3
from uuid import uuid4

from fastapi.testclient import TestClient

from app.config import Settings
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
