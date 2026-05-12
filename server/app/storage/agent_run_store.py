from __future__ import annotations

from dataclasses import dataclass, field
from time import time
from typing import Any


@dataclass
class StoredAgentRun:
    run_id: str
    client_request_id: str
    device_id_hash: str
    message: str
    stage: str = "understanding"
    status: str = "running"
    pending_action_ids: list[str] = field(default_factory=list)
    observations: list[dict[str, Any]] = field(default_factory=list)
    feedback: list[dict[str, Any]] = field(default_factory=list)
    created_at: float = field(default_factory=time)
    updated_at: float = field(default_factory=time)


class AgentRunStore:
    def __init__(self) -> None:
        self._runs: dict[str, StoredAgentRun] = {}

    def create(self, run: StoredAgentRun) -> StoredAgentRun:
        self._runs[run.run_id] = run
        return run

    def get_or_create(
        self,
        run_id: str,
        client_request_id: str,
        device_id_hash: str,
        message: str = "",
    ) -> StoredAgentRun:
        existing = self._runs.get(run_id)
        if existing is not None:
            return existing
        return self.create(
            StoredAgentRun(
                run_id=run_id,
                client_request_id=client_request_id,
                device_id_hash=device_id_hash,
                message=message,
            )
        )

    def update_stage(self, run_id: str, stage: str, status: str = "running") -> None:
        run = self._runs.get(run_id)
        if run is None:
            return
        run.stage = stage
        run.status = status
        run.updated_at = time()

    def set_pending_actions(self, run_id: str, action_ids: list[str]) -> None:
        run = self._runs.get(run_id)
        if run is None:
            return
        run.pending_action_ids = action_ids
        run.stage = "awaiting_confirmation"
        run.status = "awaiting_confirmation"
        run.updated_at = time()

    def add_observations(self, run_id: str, observations: list[dict[str, Any]]) -> None:
        run = self._runs.get(run_id)
        if run is None:
            return
        run.observations.extend(observations)
        run.stage = "reasoning"
        run.updated_at = time()

    def resume(self, run_id: str, decision: str) -> None:
        run = self._runs.get(run_id)
        if run is None:
            return
        run.pending_action_ids = []
        run.stage = "finalizing"
        run.status = decision
        run.updated_at = time()

    def add_feedback(self, run_id: str, feedback: dict[str, Any]) -> None:
        run = self._runs.get(run_id)
        if run is None:
            return
        run.feedback.append(feedback)
        run.updated_at = time()
