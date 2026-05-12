# Agent 助手架构

## 1. 架构边界

| 模块 | 职责 | 禁止 |
| --- | --- | --- |
| Android App | 对话展示、动态引导展示、反馈采集、确认卡片、本地写入执行、本地任务恢复 | 模型直接写库、绕过确认执行写入 |
| Agent 后端 | Harness 编排、状态图运行、上下文组织、模型调用、长期记忆、自我优化、动态引导生成 | 替 App 写本地账本、自动发布未验证的全局策略 |
| 大模型 | 意图理解、候选回答、候选操作、摘要生成、引导词候选生成 | 决定最终写入、声明写入已完成 |
| 本地 UseCase | 新增、修改、删除、定时记账等真实写入 | 接收未经确认的 Agent 写入 |
| 后端存储 | 账本镜像、对话、偏好、反馈、运行状态、评估结果、策略版本 | 用户自定义 API Key、设备原始标识 |

## 2. 总体架构

```mermaid
flowchart TB
  User["用户"]

  subgraph App["Android App"]
    ChatUI["助手 UI"]
    FeedbackUI["反馈图标<br/>点赞/点踩/重新生成"]
    SuggestionUI["动态引导词"]
    PendingAction["待确认操作"]
    LocalExecutor["本地执行器"]
    UseCase["现有 UseCase"]
    Room["Room 本地账本"]
  end

  subgraph Backend["Agent 后端"]
    API["Agent API / SSE"]
    Graph["状态图运行时"]
    Harness["Harness 控制层"]
    ModelClient["模型适配层"]
    Optimizer["自我优化任务"]
    SuggestionEngine["动态引导引擎"]
  end

  subgraph HarnessLayer["Harness 六要素"]
    Context["1 上下文工程<br/>输入管控"]
    Tools["2 工具编排与沙箱<br/>执行能力"]
    Memory["3 记忆与状态管理<br/>持久化"]
    Planner["4 任务规划与分解<br/>流程化"]
    Guard["5 硬约束与安全护栏<br/>可控性"]
    Verify["6 自验证反馈循环<br/>质量兜底"]
  end

  subgraph Store["后端存储"]
    LedgerStore["账本镜像"]
    ConversationStore["对话记录"]
    ProfileStore["用户偏好"]
    RunStore["任务状态"]
    FeedbackStore["反馈事件"]
    EvalStore["评估结果"]
    PolicyStore["策略版本"]
  end

  Model["大模型服务"]

  User --> ChatUI
  User --> FeedbackUI
  SuggestionUI --> ChatUI
  ChatUI --> API
  FeedbackUI --> API
  API --> Graph
  Graph --> Harness
  Harness --> Context
  Harness --> Tools
  Harness --> Memory
  Harness --> Planner
  Harness --> Guard
  Harness --> Verify
  Context --> Store
  Memory --> Store
  Verify --> FeedbackStore
  Optimizer --> EvalStore
  Optimizer --> PolicyStore
  SuggestionEngine --> ProfileStore
  SuggestionEngine --> SuggestionUI
  Harness --> ModelClient
  ModelClient --> Model
  Model --> ModelClient
  API --> ChatUI
  API --> PendingAction
  PendingAction --> LocalExecutor
  LocalExecutor --> UseCase
  UseCase --> Room
```

## 3. Harness 六大要素

| 要素 | 后端模块 | 输入 | 输出 | 关键约束 |
| --- | --- | --- | --- | --- |
| 上下文工程（输入管控） | `ContextBudgeter`、`ContextRetriever`、`PromptAssembler`、`SuggestionContextBuilder` | 用户输入、日期、对话、账本、偏好、反馈、任务状态 | 有预算的模型上下文、动态引导上下文、摘要和检索结果 | 单次调用限制 token、耗时、成本；禁止用户自定义 API Key 入模；长对话先摘要再检索 |
| 工具编排与沙箱（执行能力） | `ToolRegistry`、`ToolSandbox`、`ActionBuilder`、`PreviewBuilder` | 状态图节点请求、模型候选操作、上下文观察结果 | 结构化操作、确认预览、工具观察结果 | 工具 Schema 固定；工具超时和重试受控；后端工具不得直接写本地账本 |
| 记忆与状态管理（持久化） | `RunStore`、`ConversationStore`、`LedgerStore`、`ProfileStore`、`Checkpointer` | 完整账本、完整对话、任务阶段、用户偏好、模型输入输出 | 任务检查点、用户画像、账本摘要、长期记忆 | 允许保存完整数据；设备 ID 只存不可逆摘要；自定义 API Key 不入库 |
| 任务规划与分解（流程化） | `AgentGraph`、`IntentRouter`、`PlannerNode`、`ContextNode`、`ReasonNode`、`ConfirmNode`、`FinalNode` | 用户目标、当前状态、上下文预算、工具结果 | 可恢复任务流、阶段事件、上下文请求、待确认动作 | 写入任务必须进入确认节点；任务状态只按图推进；失败进入恢复节点 |
| 硬约束与安全护栏（可控性） | `InputGuard`、`ActionValidator`、`PolicyGuard`、`WriteGuard`、`RateLimiter` | 用户输入、模型输出、操作预览、执行结果 | 拦截结果、可读错误、重试指令、安全警告 | 金额、日期、分类、账户必须校验；禁止跳过确认；禁止模型声明已写入 |
| 自验证反馈循环（质量兜底） | `FeedbackCollector`、`RunScorer`、`MemoryUpdater`、`PromptEvaluator`、`PolicyOptimizer` | 点赞、点踩、重新生成、追问、取消、执行失败、耗时、错误 | 质量评分、用户记忆更新、提示词候选、路由优化候选 | 个人记忆可自动更新；全局提示词、工具路由和状态图变更需评估后发布 |

## 4. 任务状态图

```mermaid
stateDiagram-v2
  [*] --> input_guard
  input_guard --> intent_router
  intent_router --> context_planner
  context_planner --> memory_loader
  memory_loader --> reasoner
  reasoner --> action_builder
  action_builder --> validation_guard
  validation_guard --> final_responder: answer_only
  validation_guard --> confirmation_interrupt: write_action
  confirmation_interrupt --> execution_feedback: confirmed
  confirmation_interrupt --> final_responder: cancelled
  execution_feedback --> feedback_collector
  final_responder --> feedback_collector
  feedback_collector --> memory_update
  memory_update --> [*]

  input_guard --> error_recovery
  context_planner --> error_recovery
  reasoner --> error_recovery
  action_builder --> error_recovery
  validation_guard --> error_recovery
  execution_feedback --> error_recovery
  error_recovery --> feedback_collector
```

## 5. 数据流

```mermaid
sequenceDiagram
  participant U as 用户
  participant A as Android App
  participant B as Agent API
  participant G as 状态图
  participant H as Harness
  participant S as 后端存储
  participant M as 大模型
  participant L as 本地 UseCase

  U->>A: 输入问题
  A->>B: 创建任务
  B->>G: run_started
  G->>H: 规划上下文
  H->>S: 读取账本/对话/偏好/反馈
  H->>M: 有预算的模型请求
  M-->>H: 候选回答或候选操作
  H->>G: 校验结果
  G-->>A: SSE 阶段事件/回答/预览
  A-->>U: 展示回答或确认卡片
  U->>A: 确认/取消/点赞/点踩/重新生成
  A->>B: 回传决策和反馈
  alt 用户确认写入
    A->>L: 执行本地写入
    L-->>A: 执行结果
    A->>B: 回传执行结果
  end
  B->>S: 保存对话/反馈/评估/记忆
```

## 6. 存储结构

| 存储 | 内容 | 用途 |
| --- | --- | --- |
| `ledger_store` | 账本镜像、统计摘要、明细索引 | 查账、复盘、上下文检索 |
| `conversation_store` | 用户消息、助手消息、模型输入输出 | 多轮对话、质量评估、个性化 |
| `profile_store` | 常用分类、账户、表达习惯、回答偏好 | 个性化理解、动态引导 |
| `run_store` | 任务 ID、阶段、状态、检查点、错误 | 任务恢复、审计、调试 |
| `feedback_store` | 点赞、点踩、重新生成、取消、失败 | 质量评分、自我优化 |
| `evaluation_store` | 完成率、重试率、耗时、成本、评分 | 策略评估、版本对比 |
| `policy_store` | 提示词版本、路由规则、上下文策略 | 灰度、回滚、发布控制 |

## 7. 接口

| 接口 | 方法 | 作用 |
| --- | --- | --- |
| `/health` | `GET` | 健康检查 |
| `/api/agent/runs/stream` | `POST` | 创建任务并返回 SSE |
| `/api/agent/runs/{runId}/context` | `POST` | App 回传本地上下文观察结果 |
| `/api/agent/runs/{runId}/resume` | `POST` | App 回传确认、取消或执行结果 |
| `/api/agent/runs/{runId}/feedback` | `POST` | 回传点赞、点踩、重新生成 |
| `/api/agent/suggestions` | `POST` | 获取动态引导词 |
| `/api/agent/chat` | `POST` | 旧版本兼容 |
| `/api/agent/feedback` | `POST` | 旧版本兼容 |

## 8. 前端能力

| 能力 | UI 位置 | 数据来源 |
| --- | --- | --- |
| 任务阶段 | 消息流内轻量状态 | SSE `stage_changed` |
| 流式回复 | 助手消息 | SSE `partial_message`、`final_message` |
| 确认卡片 | 助手消息下方 | SSE `action_preview` |
| 点赞/点踩/重新生成 | 助手消息右下角小图标 | `AgentFeedbackEvent` |
| 动态引导词 | 输入框上方或会话底部 | `/api/agent/suggestions` |
| 待确认恢复 | 进入助手页时 | `agent_pending_actions`、`run_store` |

## 9. 发布约束

| 变更 | 发布要求 |
| --- | --- |
| 用户个人记忆 | 可自动更新 |
| 动态引导词排序 | 可自动更新 |
| 上下文选择阈值 | 需评估指标通过 |
| 提示词版本 | 需离线评估或人工确认 |
| 工具路由规则 | 需离线评估或人工确认 |
| 状态图结构 | 需人工确认和回归测试 |
