import json
import sqlite3
from datetime import UTC, datetime
from pathlib import Path


class AuditLog:
    def __init__(self, db_path: str):
        self.db_path = db_path
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self._init_schema()

    def record(
        self,
        endpoint: str,
        status: str,
        duration_ms: int,
        device_id_hash: str,
        model_provider: str,
        action_types: list[str],
        error_type: str = "",
    ) -> None:
        device_digest = device_id_hash[:12]
        with sqlite3.connect(self.db_path) as connection:
            connection.execute(
                """
                INSERT INTO audit_logs (
                    created_at, endpoint, status, duration_ms, device_digest,
                    model_provider, action_types, error_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    datetime.now(UTC).isoformat(),
                    endpoint,
                    status,
                    duration_ms,
                    device_digest,
                    model_provider,
                    json.dumps(action_types, ensure_ascii=False),
                    error_type,
                ),
            )

    def _init_schema(self) -> None:
        with sqlite3.connect(self.db_path) as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    created_at TEXT NOT NULL,
                    endpoint TEXT NOT NULL,
                    status TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    device_digest TEXT NOT NULL,
                    model_provider TEXT NOT NULL,
                    action_types TEXT NOT NULL,
                    error_type TEXT NOT NULL
                )
                """
            )
