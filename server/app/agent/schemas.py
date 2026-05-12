from typing import Any, Literal
from pydantic import BaseModel, ConfigDict, Field, field_validator


class LocalContext(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    time_context: dict[str, str] = Field(default_factory=dict, alias="timeContext")
    categories: list[dict[str, Any]] = Field(default_factory=list)
    accounts: list[dict[str, Any]] = Field(default_factory=list)
    records: list[dict[str, Any]] = Field(default_factory=list)
    stats: dict[str, Any] = Field(default_factory=dict)
    scheduled_records: list[dict[str, Any]] = Field(default_factory=list, alias="scheduledRecords")


class AgentChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    client_request_id: str = Field(alias="clientRequestId", min_length=1, max_length=64)
    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    message: str = Field(min_length=1)
    local_context: LocalContext = Field(alias="localContext")
    conversation_history: list["AgentConversationTurn"] = Field(default_factory=list, alias="conversationHistory")
    timezone: str = Field(min_length=1, max_length=64)
    app_version: str = Field(alias="appVersion", min_length=1, max_length=32)

    @field_validator("message")
    @classmethod
    def trim_message(cls, value: str) -> str:
        trimmed = value.strip()
        if not trimmed:
            raise ValueError("输入不能为空")
        return trimmed


class AgentConversationTurn(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=800)


class AgentContextRequest(BaseModel):
    type: Literal[
        "category_list",
        "account_list",
        "record_candidates",
        "month_stats",
        "date_range_stats",
        "account_summary",
        "scheduled_record_list",
    ]
    reason: str = ""


class AgentActionPreview(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    type: Literal[
        "create_record",
        "update_record",
        "delete_record",
        "batch_update_records",
        "create_scheduled_record",
        "update_scheduled_record",
        "disable_scheduled_record",
        "answer_only",
        "ask_user",
    ]
    title: str
    description: str = ""
    impact_count: int = Field(default=0, alias="impactCount", ge=0)
    records: list[dict[str, Any]] = Field(default_factory=list)
    scheduled_records: list[dict[str, Any]] = Field(default_factory=list, alias="scheduledRecords")
    risk_notice: str = Field(default="", alias="riskNotice")


class AgentRunRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    client_request_id: str = Field(alias="clientRequestId", min_length=1, max_length=64)
    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    message: str = Field(min_length=1)
    conversation_history: list[AgentConversationTurn] = Field(default_factory=list, alias="conversationHistory")
    timezone: str = Field(min_length=1, max_length=64)
    app_version: str = Field(alias="appVersion", min_length=1, max_length=32)

    @field_validator("message")
    @classmethod
    def trim_message(cls, value: str) -> str:
        trimmed = value.strip()
        if not trimmed:
            raise ValueError("输入不能为空")
        return trimmed


class AgentRunContextRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    client_request_id: str = Field(alias="clientRequestId", min_length=1, max_length=64)
    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    observations: list["AgentContextObservation"] = Field(default_factory=list)


class AgentContextObservation(BaseModel):
    status: Literal["success", "warning", "error"]
    summary: str
    data: dict[str, Any] = Field(default_factory=dict)
    next_actions: list[str] = Field(default_factory=list, alias="nextActions")
    artifacts: list[str] = Field(default_factory=list)


class AgentRunResumeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    client_request_id: str = Field(alias="clientRequestId", min_length=1, max_length=64)
    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    decision: Literal["confirmed", "cancelled", "failed"]
    action_results: list[dict[str, Any]] = Field(default_factory=list, alias="actionResults")
    error_type: str = Field(default="", alias="errorType")


class AgentRunFeedbackRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    client_request_id: str = Field(alias="clientRequestId", min_length=1, max_length=64)
    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    message_id: str = Field(alias="messageId", min_length=1, max_length=64)
    feedback: Literal["like", "dislike", "regenerate"]
    reason: str = ""


class AgentSuggestionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    today: str
    timezone: str = "Asia/Shanghai"
    recent_conversation: list[AgentConversationTurn] = Field(default_factory=list, alias="recentConversation")
    local_summary: dict[str, Any] = Field(default_factory=dict, alias="localSummary")
    limit: int = Field(default=4, ge=2, le=4)


class AgentSuggestion(BaseModel):
    text: str
    reason: str


class AgentChatResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    reply: str
    needs_more_context: bool = Field(alias="needsMoreContext")
    context_requests: list[AgentContextRequest] = Field(default_factory=list, alias="contextRequests")
    actions: list[AgentActionPreview] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)


class AgentFeedbackRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    client_request_id: str = Field(alias="clientRequestId", min_length=1, max_length=64)
    device_id_hash: str = Field(alias="deviceIdHash", min_length=16, max_length=128)
    result: Literal["confirmed", "cancelled", "failed"]
    action_types: list[str] = Field(default_factory=list, alias="actionTypes")
    error_type: str = Field(default="", alias="errorType")
