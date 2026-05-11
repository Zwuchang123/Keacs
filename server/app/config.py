from dataclasses import dataclass
import os
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    app_name: str = "Keacs Agent Backend"
    model_provider: str = "mock"
    model_base_url: str = ""
    model_api_key: str = ""
    model_name: str = "keacs-agent-mock"
    model_reasoning_split: bool = False
    request_per_minute_limit: int = 20
    request_per_day_limit: int = 200
    max_message_length: int = 1500
    max_context_items: int = 260
    audit_db_path: str = "server/data/audit.sqlite3"

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            model_provider=os.getenv("KEACS_MODEL_PROVIDER", "mock").strip() or "mock",
            model_base_url=os.getenv("KEACS_MODEL_BASE_URL", "").strip(),
            model_api_key=os.getenv("KEACS_MODEL_API_KEY", "").strip(),
            model_name=os.getenv("KEACS_MODEL_NAME", "keacs-agent-mock").strip() or "keacs-agent-mock",
            model_reasoning_split=_read_bool("KEACS_MODEL_REASONING_SPLIT", False),
            request_per_minute_limit=_read_int("KEACS_RATE_LIMIT_PER_MINUTE", 20),
            request_per_day_limit=_read_int("KEACS_RATE_LIMIT_PER_DAY", 200),
            max_message_length=_read_int("KEACS_MAX_MESSAGE_LENGTH", 1500),
            max_context_items=_read_int("KEACS_MAX_CONTEXT_ITEMS", 260),
            audit_db_path=os.getenv("KEACS_AUDIT_DB_PATH", "server/data/audit.sqlite3").strip()
            or "server/data/audit.sqlite3",
        )

    def ensure_storage_parent(self) -> None:
        Path(self.audit_db_path).parent.mkdir(parents=True, exist_ok=True)


def _read_int(name: str, default: int) -> int:
    raw_value = os.getenv(name)
    if raw_value is None:
        return default
    try:
        value = int(raw_value)
    except ValueError:
        return default
    return value if value > 0 else default


def _read_bool(name: str, default: bool) -> bool:
    raw_value = os.getenv(name)
    if raw_value is None:
        return default
    return raw_value.strip().lower() in {"1", "true", "yes", "on"}
