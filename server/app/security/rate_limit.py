from dataclasses import dataclass
from datetime import datetime, timedelta, timezone


@dataclass(frozen=True)
class RateLimitResult:
    allowed: bool
    message: str = ""
    retry_after_seconds: int = 60


class DeviceRateLimiter:
    def __init__(self, per_minute_limit: int, per_day_limit: int):
        self.per_minute_limit = per_minute_limit
        self.per_day_limit = per_day_limit
        self._hits: dict[str, list[datetime]] = {}

    def check(self, device_id_hash: str) -> RateLimitResult:
        # Python 3.10 兼容：datetime.UTC 仅在 3.11+ 提供
        now = datetime.now(timezone.utc)
        day_start = now - timedelta(days=1)
        minute_start = now - timedelta(minutes=1)
        hits = [hit for hit in self._hits.get(device_id_hash, []) if hit >= day_start]

        minute_count = sum(1 for hit in hits if hit >= minute_start)
        if minute_count >= self.per_minute_limit:
            self._hits[device_id_hash] = hits
            return RateLimitResult(False, "请求过于频繁，请稍后再试。", 60)

        if len(hits) >= self.per_day_limit:
            self._hits[device_id_hash] = hits
            return RateLimitResult(False, "今日助手请求次数已达上限，请明天再试。", 3600)

        hits.append(now)
        self._hits[device_id_hash] = hits
        return RateLimitResult(True)
