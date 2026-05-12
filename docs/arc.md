# 技术架构

<br />

## 0. 变更记录与开发定位

### 0.1 架构变更记录

| 改动时间       | 改动内容                                                         | 改动原因                                       | 影响范围                                 |
| ---------- | ------------------------------------------------------------ | ------------------------------------------ | ------------------------------------ |
| 2026-05-11 20:07:38 | 融合 Agent 助手架构，补充 Android 端模块、FastAPI 后端、大模型接入、接口协议、数据边界和安全规则 | Agent 助手是独立开发任务，需要在主架构文档中提供统一入口，方便后续开发快速定位 | 技术栈、目录结构、偏好配置、核心服务、业务规则入口、Agent 专项架构 |
| 2026-05-11 20:07:38 | 将 Agent 模型接入调整为通用大模型服务配置，支持官方免费服务和用户自定义访问地址/API Key          | 避免后端和 App 绑定单一模型供应商，保留后续切换和用户自选模型服务的空间     | 技术栈、偏好配置、Agent 后端、模型适配、接口协议、安全规则     |
| 2026-05-12 16:40:00 | 增加助手会话本机持久化、长对话上下文传递、模型慢响应兜底、可读操作预览和响应耗时展示 | 提升助手稳定性和可理解性，减少模型异常时的不可用提示 | Android Agent 模块、偏好配置、后端接口、模型适配、UI 状态 |
| 2026-05-12 16:23:50 | 调整 Agent 上下文选择策略，支持按问题范围上传单日、单周、单月、年度、最近区间或全部账单明细 | 提升长期账本分析能力，避免模型只能看到单月上下文 | Android AgentContextProvider、后端接口、数据边界 |
| 2026-05-12 18:19:27 | 基于 Harness 与 LangGraph 原理重设 Agent 后端，增加状态图编排、工具边界、可恢复确认、流式事件和隐私化检查点 | 提升复杂助手任务的稳定性、可观测性和安全性，同时保持账本本地可信 | Agent 后端、Android Agent 模块、接口协议、本地存储、安全日志 |
| 2026-05-12 18:47:00 | 放宽官方在线助手数据存储边界，新增用户反馈、个性化记忆、自我优化和动态引导词架构 | 支持助手越用越懂用户，同时通过上下文和资源管理控制成本与稳定性 | Agent 后端、Android Agent 模块、数据存储、提示词、评估系统 |

### 0.2 Agent 助手开发定位索引

| 开发内容       | 对应章节                      | 说明                                        |
| ---------- | ------------------------- | ----------------------------------------- |
| 技术栈变化      | `1. 技术栈`                  | 增加 Agent 后端、通用大模型接入和 HTTPS API 要求           |
| App 目录调整   | `3. 目录结构`                 | 增加 `ui/agent`、`domain/agent`、`data/agent` |
| 在线助手配置     | `5. 数据库`                  | 增加 DataStore 配置项                          |
| Agent 服务边界 | `6. 核心服务`                 | 明确 App 端、后端和模型各自职责                        |
| 写入入口       | `7. 业务规则入口`               | Agent 写入必须复用现有 UseCase                    |
| Agent 总体架构 | `11. Agent 助手架构`          | Agent 独立开发的主要架构集中在本章节                     |
| 后端接口       | `11.7 后端接口`               | 定义健康检查、任务流、上下文继续和确认继续接口                |
| 安全规则       | `11.9 安全规则`、`11.10 日志和限流` | 明确确认执行、日志、限流和权限边界                         |
| 自我优化       | `11.11 反馈、自我优化和动态引导` | 明确用户反馈、个性化记忆、自动评估和动态引导词机制              |
| 重构计划       | `docs\agent-plan.md`          | 仅维护 Agent 助手重构开发计划                            |

## 1. 技术栈

Android 端：

- 平台：Android 原生
- 语言：Kotlin
- UI：Jetpack Compose
- 架构：MVVM + Repository + UseCase
- 本地数据库：Room + SQLite
- 偏好存储：DataStore
- 异步：Kotlin Coroutines + Flow
- 导入导出：JSON 文件、XLSX 文件读取
- 网络：HTTPS API 请求
- 构建工具：Gradle

Agent 后端：

- 语言：Python
- Web 框架：FastAPI
- 数据校验：Pydantic
- HTTP 客户端：httpx
- Agent 编排：LangGraph 状态图或等价轻量状态图实现
- 流式事件：SSE
- 轻量存储：SQLite，官方服务多实例部署时可升级为 PostgreSQL
- 记忆检索：SQLite FTS 或 PostgreSQL pgvector，按部署阶段选择
- 后台任务：轻量定时任务或队列，用于摘要、评估和自我优化
- 容器编排：Docker Compose
- HTTPS 反向代理：Caddy 或 Nginx

大模型：

- 模型供应商：不写死，由官方后端配置或用户自定义配置决定
- 接入方式：优先按 OpenAI 兼容的访问地址、API Key 和模型名接入
- 能力：对话生成、Tool Use、结构化操作生成

## 2. 分层结构

```text
UI Layer
  Compose 页面
  ViewModel
  UiState

Domain Layer
  UseCase
  Domain Model
  Business Rule

Data Layer
  Repository
  Room DAO
  Entity
  Backup Reader/Writer
  DataStore Preferences
```

## 3. 目录结构

```text
app/
  ui/
    welcome/
    home/
    record/
    agent/
    account/
    category/
    stats/
    settings/
    backup/
    scheduled/
  domain/
    agent/
    model/
    usecase/
    rule/
  data/
    agent/
    local/
      dao/
      entity/
      database/
      PreferencesManager
    repository/
    backup/
    importer/
  common/
server/
  app/
    api/
    agent/
    security/
    storage/
  tests/
```

## 4. 数据模型

### Category

- `id`
- `name`
- `direction`
- `iconKey`
- `colorKey`
- `isPreset`
- `isEnabled`
- `sortOrder`
- `createdAt`
- `updatedAt`

### Account

- `id`
- `name`
- `nature`
- `type`
- `iconKey`
- `colorKey`
- `initialBalanceCent`
- `isEnabled`
- `createdAt`
- `updatedAt`

### Record

- `id`
- `type`
- `amountCent`
- `occurredAt`
- `categoryId`
- `fromAccountId`
- `toAccountId`
- `note`
- `createdAt`
- `updatedAt`

### ScheduledRecord

- `id`
- `type`
- `amountCent`
- `categoryId`
- `fromAccountId`
- `toAccountId`
- `frequency`
- `recurrenceMonth`
- `recurrenceDay`
- `recurrenceWeekday`
- `recurrenceValues`
- `recurrenceHour`
- `nextRunAt`
- `note`
- `isEnabled`
- `createdAt`
- `updatedAt`

## 5. 数据库

数据库名：

```text
keacs.db
```

数据表：

- `categories`
- `accounts`
- `records`
- `account_adjustments`：历史兼容表，当前功能不再新增账户变动记录。
- `app_meta`
- `scheduled_records`

偏好存储：

- `has_welcomed`：记录本设备是否已经点击过进入页的“开始记账”，只保存在本机。
- `agent_enabled`：是否启用在线助手。
- `agent_model_service_mode`：模型服务模式，支持官方免费服务和用户自定义服务。
- `agent_official_service_url`：官方 Agent 后端地址，正式版可使用内置默认值。
- `agent_custom_base_url`：用户自定义模型访问地址。
- `agent_custom_api_key`：用户自定义模型 API Key，仅保存在本机。
- `agent_custom_model_name`：用户自定义模型名称。
- `agent_device_id`：设备识别 ID，用于官方免费服务限流；上传前必须转换为不可逆摘要。
- `agent_data_scope`：在线助手数据使用范围，官方模式允许全量同步，自定义模式由用户配置的模型服务决定。
- `agent_personalization_enabled`：是否启用个性化记忆和习惯学习。
- `agent_feedback_enabled`：是否展示回答反馈入口。
- `agent_dynamic_suggestions_enabled`：是否展示动态引导词。
- `agent_conversation_snapshot`：助手最近对话快照，仅用于兼容旧实现；重构后对话和待确认任务迁移到本地结构化表。

Agent 本地表：

- `agent_messages`：保存最近助手消息，仅保存在本机。
- `agent_runs`：保存本机任务状态、阶段、请求时间、完成时间和失败原因。
- `agent_pending_actions`：保存待确认操作预览、一次性操作标识和确认状态。
- `agent_feedback_events`：保存点赞、点踩、重新生成和用户取消等反馈事件。
- `agent_local_profile`：保存本机可用的用户习惯摘要，用于离线展示和在线请求准备。

索引：

- `records.occurredAt`
- `records.type`
- `records.categoryId`
- `records.fromAccountId`
- `records.toAccountId`
- `scheduled_records.nextRunAt`

## 6. 核心服务

### RecordService

- 创建收入
- 创建支出
- 创建转账
- 编辑账目
- 删除账目

### AccountService

- 创建账户
- 编辑账户（支持直接修改余额）
- 停用账户
- 计算账户余额

### CategoryService

- 创建分类
- 编辑分类
- 停用分类
- 初始化预设分类

### StatsService

- 收支统计
- 分类统计
- 账户统计
- 总资产统计
- 总负债统计
- 净资产统计

### BackupService

- 导出 JSON 备份
- 导入 JSON 备份
- 备份版本校验
- 导入事务处理

### ExcelRecordImportService

- 读取 `.xlsx` 第一张工作表
- 校验日期、收支类型、金额
- 按名称匹配分类和账户
- 分类匹配不到时归到“其他”，并把原分类写入备注

### ScheduledRecordRepository

- 保存定时记账模板
- 应用启动时生成到期账目
- 生成后按周、月、年配置推进下次时间

### PreferencesManager

- 读取本设备首次进入状态
- 用户点击“开始记账”后写入本地标记
- 读取和保存在线助手启用状态、模型服务模式、自定义访问地址、API Key、模型名和数据范围

### AgentRepository

- 读取在线助手配置
- 调用 Agent 后端任务接口
- 消费 Agent 后端 SSE 事件
- 恢复未完成的待确认任务
- 调用 Agent 后端继续执行接口
- 调用 Agent 后端执行结果反馈接口
- 上报点赞、点踩和重新生成反馈
- 获取动态引导词
- 处理网络失败、API Key 无效、官方服务限流和结构化返回异常
- 模型失败时对常见记账和查账请求提供本地可读兜底

### AgentContextProvider

- 按需读取分类列表
- 按需读取账户列表
- 按需读取候选账目
- 按需读取统计摘要
- 按需读取定时记账摘要
- 按后端上下文请求返回结构化观察结果
- 支持官方模式下全量同步，同时为单次模型调用准备摘要、检索结果或必要明细

### AgentBackend

- 识别设备并执行限流
- 限制请求频率
- 通过状态图编排理解、上下文请求、模型调用、校验、确认和收尾
- 维护工具注册表、输入输出 Schema 和错误恢复规则
- 按服务端配置调用大模型供应商
- 校验模型结构化输出
- 通过 SSE 返回 App 可展示的阶段、回答、追问或操作预览
- 持久化不含账本明细的任务检查点
- 保存账本、对话、偏好、反馈和任务日志
- 生成用户个性化记忆
- 生成动态引导词
- 基于反馈和任务结果优化提示词、工具路由和上下文策略

## 7. 业务规则入口

所有写操作必须通过 UseCase：

- `CreateIncomeUseCase`
- `CreateExpenseUseCase`
- `CreateTransferUseCase`
- `UpdateRecordUseCase`
- `DeleteRecordUseCase`
- `ImportBackupUseCase`
- `ExportBackupUseCase`
- `GenerateDueScheduledRecordsUseCase`

Agent 助手不能直接调用 DAO 写入账本。所有新增、修改、删除、批量处理和定时记账写入，必须在用户确认后通过现有 UseCase 执行。

## 8. 余额计算

金额单位：

```text
Cent
```

账户余额：

```text
currentBalance = accounts.initialBalanceCent
historicalBalanceAt(time) = currentBalance - recordEffectsAfter(time)
```

`accounts.initialBalanceCent` 当前承担“账户当前余额”职责。统计历史节点时，按节点之后的账目影响从当前余额反推。账户余额采用有符号数：资产账户通常为正数，负债账户为负数。

账目影响规则：

```text
income: toAccount += amount
expense: fromAccount -= amount
transfer: fromAccount -= amount, toAccount += amount
```

上述规则不因账户性质反向处理。

资产负债：

```text
totalAsset = sum(asset account balances)
totalLiability = sum(liability account balances)
netAsset = totalAsset + totalLiability
```

`totalLiability` 本身为负数合计。

## 9. 备份结构

```json
{
  "backupVersion": 2,
  "exportedAt": 0,
  "appVersionName": "1.0.5",
  "appVersionCode": 5,
  "balanceSignPolicy": "asset_positive_liability_negative",
  "categories": [],
  "accounts": [],
  "records": []
}
```

导入策略：

- 仅合并导入
- 不覆盖现有数据
- 不做去重
- 导入时重新生成本地 ID
- 使用临时 ID 映射恢复关联关系
- 全流程数据库事务
- 支持读取版本 1 和版本 2 备份
- 导入版本 1 备份时，将负债账户余额转换为当前有符号规则

## 10. 数据迁移

- Room 数据库版本从 `1` 开始，当前版本为 `5`。
- 结构变更必须提供 Migration。
- 备份文件版本独立于数据库版本。

## 11. Agent 助手架构

Agent 助手采用“本地可信账本 + 后端状态图编排 + Harness 安全边界”的架构。

核心原则：

- 本地 Room 数据库仍是账本唯一可信来源。
- 官方在线助手允许保存用户完整账本、对话、偏好和反馈，用于个性化和持续优化。
- 大模型只负责理解用户意图、生成候选回答和结构化操作。
- Agent 后端负责状态图编排、工具边界、安全校验、流式事件和接口保护。
- App 展示操作预览，用户确认后通过现有 UseCase 执行写入。
- 每个工具输入输出都必须有明确 Schema，工具结果必须可校验、可解释、可恢复。
- 需要用户确认的步骤按中断处理，确认后从同一任务继续，不重新猜测用户意图。
- 后端可以长期保存数据，但单次模型调用必须通过摘要、检索和预算控制选择上下文。
- 未启用在线助手时，原离线功能不受影响。

### 11.1 总体结构

```text
Android App
  Compose 助手页面
  Agent ViewModel
  Agent Repository
  AgentRunStore
  AgentContextProvider
  AgentActionExecutor
  现有 UseCase
  Room 数据库

Agent Backend
  FastAPI
  设备识别
  限流
  Agent 状态图
  Harness 工具注册表
  安全护栏
  流式事件
  检查点和数据存储
  个性化记忆
  自我优化任务
  动态引导词
  大模型适配层
  输出结构校验
  质量评估日志

Model Provider API
  访问地址
  API Key
  模型名
  Tool Use 或结构化输出
```

数据流：

1. 用户在“助手”页输入问题。
2. App 创建本地任务，并带上近期对话、用户偏好摘要和可用账本摘要。
3. App 根据模型服务模式选择官方 Agent 后端或用户自定义模型服务。
4. 官方模式下，App 调用后端任务流接口，后端创建状态图运行实例。
5. 后端可保存用户账本、对话、反馈和偏好数据，用于任务恢复和后续优化。
6. 后端先做输入校验、意图路由和上下文规划。
7. 后端按上下文预算选择摘要、检索结果或明细数据，不把长期数据无控制地放进同一次模型调用。
8. 后端调用模型和工具生成回答、追问或操作预览。
9. App 渲染流式阶段、文本回复、候选选择或待确认操作。
10. 写入类操作进入等待确认状态，App 本地保存一次性操作标识。
11. 用户确认后，App 通过现有 UseCase 写入本地数据库。
12. App 将点赞、点踩、重新生成、确认、取消或执行失败结果发回后端。
13. 后端从中断点完成任务收尾，并把反馈进入评估和优化流程。
14. 自定义模式下，App 直接调用用户配置的模型服务，但仍复用本地 Harness 校验、操作预览和确认执行规则。

### 11.2 Android 端模块

建议新增目录：

```text
app/src/main/java/com/keacs/app/ui/agent
app/src/main/java/com/keacs/app/domain/agent
app/src/main/java/com/keacs/app/data/agent
```

职责：

- `ui/agent`：助手页面、消息列表、输入框、操作预览卡片、确认弹窗。
- `domain/agent`：Agent 操作模型、任务状态、确认执行规则、上下文读取用例。
- `data/agent`：网络请求、SSE 事件解析、DTO、连接配置读取、本地任务持久化。

Android 端新增组件：

- `AgentRunStore`：保存最近消息、任务阶段和待确认操作。
- `AgentEventReducer`：把后端事件转换为页面可展示状态。
- `AgentActionExecutor`：在用户确认后调用现有 UseCase，并返回执行结果。
- `AgentLocalHarness`：自定义模型服务模式下执行本地结构校验和安全拦截。
- `AgentFeedbackCollector`：收集点赞、点踩、重新生成、取消确认和执行失败事件。
- `AgentSuggestionProvider`：请求和缓存动态引导词。

导航调整：

- 底部导航保持 5 项：`首页 -> 统计 -> 新增 -> 助手 -> 我的`。
- `KeacsDestination.Discover` 替换为 `KeacsDestination.Agent`。
- 原发现页 UI 不作为底部一级页面展示。
- 原发现页数据和展示逻辑迁入“我的”相关页面或模块。

网络权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

正式环境只允许 HTTPS 服务器地址。

### 11.3 本地上下文读取

App 根据后端请求或用户问题读取必要数据。

首版支持：

- 分类列表
- 账户列表
- 指定日期、周、月、年、最近区间或全部账单范围内的候选账目
- 指定月份统计摘要
- 当前账户余额摘要
- 定时记账列表摘要

数据同步与上下文选择规则：

- 官方模式允许同步和保存全量账本、完整对话、偏好和反馈。
- 单次模型调用按任务选择上下文，优先使用统计摘要、用户偏好、最近对话和检索结果。
- 单日、单周、单月、单年问题优先使用对应范围。
- 最近 7 天、最近 30 天、最近 3 个月、最近半年、最近一年按滚动区间组织摘要。
- 用户明确提出全部、所有、历史、累计、从开始、总共、整体等问题时，可以使用全量账本分析。
- 改账、删账、大额和明细查询会使用候选账目明细。
- 上下文超过预算时，先摘要、分段或检索，再调用模型。

上下文观察结果统一结构：

```json
{
  "status": "success | warning | error",
  "summary": "string",
  "data": {},
  "nextActions": [],
  "artifacts": []
}
```

规则：

- `summary` 必须能让模型和日志理解结果，并用于控制上下文长度。
- `data` 只放本次任务需要的结构化数据。
- `nextActions` 用于告诉后端可继续、需要缩小范围或需要用户补充。
- `artifacts` 只放本地不可逆标识或候选项标识，不放文件路径和备份文件。

### 11.4 写入执行

App 只能通过现有 UseCase 执行写入：

- `CreateIncomeUseCase`
- `CreateExpenseUseCase`
- `CreateTransferUseCase`
- `UpdateRecordUseCase`
- `DeleteRecordUseCase`
- 定时记账相关 UseCase

禁止 Agent 模块直接调用 DAO 写账。

### 11.5 后端架构

后端使用：

- Python
- FastAPI
- Pydantic
- httpx
- SQLite
- Docker Compose
- Caddy 或 Nginx 提供 HTTPS 反向代理

后端目录建议：

```text
server/
  app/
    main.py
    config.py
    api/
      health.py
      agent.py
    agent/
      graph.py
      orchestrator.py
      nodes.py
      harness.py
      tools.py
      prompts.py
      schemas.py
      provider_client.py
      validators.py
      events.py
      checkpoints.py
      memory.py
      optimizer.py
      suggestions.py
    security/
      access_code.py
      rate_limit.py
    storage/
      audit_log.py
      run_store.py
      conversation_store.py
      ledger_store.py
      profile_store.py
      feedback_store.py
      evaluation_store.py
  tests/
  Dockerfile
  docker-compose.yml
```

后端负责：

- 识别设备并执行限流。
- 限制请求频率。
- 按状态图推进任务阶段。
- 通过 Harness 工具注册表约束可执行能力。
- 按服务端配置调用大模型供应商。
- 维护模型系统提示词。
- 校验模型结构化输出。
- 返回 App 可展示的流式阶段、回答、追问或操作预览。
- 保存任务检查点、完整对话、账本数据、用户偏好和反馈事件。
- 生成用户记忆、动态引导词和优化分析结果。

后端不负责：

- 直接写入用户账本。
- 判断用户最终是否执行写入。
- 自动发布未经验证的架构或提示词变更。

#### 11.5.1 状态图编排

官方 Agent 后端采用 LangGraph 风格状态图。每个节点只负责一个明确阶段，节点之间通过条件路由进入下一步。

建议节点：

- `input_guard`：校验输入长度、配置、限流和明显禁止内容。
- `intent_router`：识别记账、查账、改账、删账、定时记账或建议类请求。
- `context_planner`：决定本次模型调用需要使用哪些摘要、记忆、检索结果或明细。
- `context_interrupt`：向 App 发出上下文请求，并等待 App 返回观察结果。
- `memory_loader`：加载用户偏好、历史反馈和长期习惯。
- `reasoner`：结合用户问题、用户记忆和任务上下文生成候选回答或候选操作。
- `action_builder`：把候选操作转换为 Keacs 结构化操作。
- `validation_guard`：校验金额、日期、分类、账户、批量影响范围和禁止建议。
- `confirmation_interrupt`：对所有写入类操作中断等待用户确认。
- `execution_feedback`：接收 App 执行成功、取消或失败结果。
- `feedback_collector`：接收点赞、点踩和重新生成结果。
- `suggestion_builder`：生成动态引导词。
- `final_responder`：生成最终可读回复。
- `error_recovery`：把异常转换为可读原因和下一步。

任务状态只允许向前推进。失败后可以进入 `error_recovery`，不能绕过确认节点直接执行写入。

#### 11.5.2 Harness 工具边界

后端工具不直接读写用户账本，只能生成上下文请求、校验结构化结果、生成预览和处理反馈。

工具设计规则：

- 工具名稳定、含义单一，禁止使用万能工具。
- 工具输入必须使用 Pydantic Schema。
- 工具输出必须包含 `status`、`summary`、`nextActions` 和必要数据。
- 工具失败必须返回根因提示、可安全重试方式和停止条件。
- 高风险操作使用小粒度工具，写入执行只保留在 App 本地。

首版工具集合：

- `plan_context_request`
- `validate_context_observation`
- `build_record_action`
- `build_scheduled_action`
- `validate_agent_action`
- `build_action_preview`
- `build_final_answer`
- `classify_recoverable_error`
- `record_feedback_event`
- `update_user_memory`
- `generate_dynamic_suggestions`
- `score_run_quality`

#### 11.5.3 数据存储和资源管理

官方在线助手允许保存完整账本、完整对话、模型输入输出、用户偏好、反馈事件和任务检查点。保存这些数据的目的，是恢复任务、形成用户记忆、评估回答质量和持续优化 Agent。

允许持久化：

- `runId`
- `clientRequestId`
- 当前阶段
- 意图类型
- 完整账本数据
- 完整对话记录
- 模型输入输出
- 上下文请求和观察结果
- 操作预览
- 确认状态
- 用户反馈事件
- 用户偏好和习惯摘要
- 动态引导词候选
- 优化评估结果
- 错误类型
- 耗时和模型供应商元信息

仍禁止持久化：

- 用户自定义 API Key。
- 设备原始标识。

资源管理规则：

- 存储层可以保存完整数据，推理层必须按上下文预算取数。
- 长对话进入摘要层，摘要保留用户目标、偏好、未完成事项和关键纠错。
- 账本进入统计摘要层和检索层，常规问答优先使用摘要。
- 只有明细定位、长期复盘、用户明确全量分析等场景才使用大量明细。
- 后台优化任务需要限频，不能影响用户实时对话。
- 需要记录每次模型调用的 token、耗时、重试次数和失败原因。

### 11.6 大模型接入

Agent 不写死模型供应商。

接入策略：

- 默认官方免费服务由 Keacs 后端提供。
- 官方后端通过配置项管理模型访问地址、API Key 和模型名。
- 用户可在 App 中切换为自定义模型服务，并配置访问地址、API Key 和模型名。
- 首版优先适配 OpenAI 兼容接口。
- 后端封装 `ModelProviderClient`，避免业务编排绑定具体模型供应商。
- App 端自定义模型服务也通过统一 `ModelServiceClient` 调用。
- 模型输出必须是 Keacs 定义的结构化结果。
- 模型返回无法解析时，返回可读错误，不让 App 猜测执行。
- 模型响应慢或结构不稳定时，后端返回可读下一步；App 可对常见记账和查账请求使用本地兜底。

后续如模型供应商变化，只调整模型适配层，不修改记账业务协议。

### 11.7 后端接口

健康检查：

```http
GET /health
```

返回：

```json
{
  "status": "ok"
}
```

任务流接口：

```http
POST /api/agent/runs/stream
```

请求字段：

```json
{
  "clientRequestId": "uuid",
  "deviceIdHash": "string",
  "message": "string",
  "conversationHistory": [
    {
      "role": "user | assistant",
      "content": "string"
    }
  ],
  "timezone": "Asia/Shanghai",
  "appVersion": "1.2.2"
}
```

`conversationHistory` 可传近期对话和本机偏好摘要。官方后端可保存完整对话，但单次请求仍需要限制上下文长度。

该接口用于官方免费模型服务。自定义模型服务由 App 直接调用用户配置的模型访问地址，不经过 Keacs 后端。

SSE 事件类型：

```json
{"type":"run_started","runId":"uuid"}
{"type":"stage_changed","stage":"understanding | reading_context | reasoning | validating | awaiting_confirmation | finalizing"}
{"type":"context_requested","runId":"uuid","requests":[]}
{"type":"partial_message","content":"string"}
{"type":"action_preview","runId":"uuid","actions":[]}
{"type":"awaiting_confirmation","runId":"uuid","actionIds":[]}
{"type":"final_message","reply":"string","warnings":[]}
{"type":"run_failed","errorType":"string","message":"string","retryable":true}
```

上下文继续接口：

```http
POST /api/agent/runs/{runId}/context
```

请求字段：

```json
{
  "clientRequestId": "uuid",
  "deviceIdHash": "string",
  "observations": []
}
```

确认继续接口：

```http
POST /api/agent/runs/{runId}/resume
```

请求字段：

```json
{
  "clientRequestId": "uuid",
  "deviceIdHash": "string",
  "decision": "confirmed | cancelled | failed",
  "actionResults": [],
  "errorType": "string"
}
```

回答反馈接口：

```http
POST /api/agent/runs/{runId}/feedback
```

请求字段：

```json
{
  "clientRequestId": "uuid",
  "deviceIdHash": "string",
  "messageId": "string",
  "feedback": "like | dislike | regenerate",
  "reason": "string"
}
```

动态引导词接口：

```http
POST /api/agent/suggestions
```

请求字段：

```json
{
  "deviceIdHash": "string",
  "today": "2026-05-12",
  "timezone": "Asia/Shanghai",
  "recentConversation": [],
  "localSummary": {},
  "limit": 4
}
```

返回字段：

```json
{
  "suggestions": [
    {
      "text": "复盘本月餐饮支出",
      "reason": "month_end | recent_topic | spending_change | user_habit"
    }
  ]
}
```

兼容接口：

```http
POST /api/agent/chat
POST /api/agent/feedback
```

兼容接口仅用于旧版本 App。新架构优先使用任务流、上下文继续和确认继续接口。

### 11.8 核心数据类型

`AgentRun` 用于描述一次助手任务。

字段包括：

- 任务 ID
- 客户端请求 ID
- 当前阶段
- 当前状态
- 意图类型
- 创建时间
- 完成时间
- 失败原因

`AgentRunEvent` 用于 App 渲染流式进度。

类型包括：

- `run_started`
- `stage_changed`
- `context_requested`
- `partial_message`
- `action_preview`
- `awaiting_confirmation`
- `final_message`
- `run_failed`

`AgentContextRequest` 用于后端请求 App 补充本地上下文。

类型包括：

- `category_list`
- `account_list`
- `record_candidates`
- `month_stats`
- `date_range_stats`
- `account_summary`
- `scheduled_record_list`

`AgentContextObservation` 用于 App 返回上下文读取结果。

字段包括：

- 状态
- 摘要
- 结构化数据
- 下一步建议
- 不可逆标识

`AgentAction` 用于描述待确认操作。

类型包括：

- `create_record`
- `update_record`
- `delete_record`
- `batch_update_records`
- `create_scheduled_record`
- `update_scheduled_record`
- `disable_scheduled_record`
- `answer_only`
- `ask_user`

`AgentActionPreview` 用于 App 展示确认卡片。

字段包括：

- 一次性操作 ID
- 操作类型
- 操作标题
- 操作说明
- 影响条数
- 账目明细
- 定时记账明细
- 风险提示

`AgentExecutionResult` 用于 App 回传确认后的执行结果。

字段包括：

- 操作 ID
- 执行状态
- 本地错误类型
- 可读失败原因

`AgentFeedbackEvent` 用于记录用户对回答的反馈。

字段包括：

- 任务 ID
- 消息 ID
- 反馈类型
- 可选原因
- 触发时间

`AgentUserMemory` 用于记录用户习惯。

字段包括：

- 常用分类
- 常用账户
- 常用表达
- 回答偏好
- 复盘偏好
- 最近纠错

`AgentSuggestion` 用于动态引导词。

字段包括：

- 引导词文本
- 生成原因
- 关联日期
- 关联上下文
- 展示优先级

### 11.9 安全规则

模型不能直接调用数据库，也不能直接执行写入。

写入必须同时满足：

1. 后端结构校验通过。
2. App 本地业务校验通过。
3. 用户在 App 上确认。
4. App 通过现有 UseCase 执行。
5. 一次性操作 ID 未被执行、取消或失败。

以下情况必须追问或让用户选择：

- 候选账目不唯一。
- 分类无法确定。
- 账户无法确定。
- 金额缺失。
- 日期缺失且无法从上下文合理推断。
- 批量影响数量过大。

禁止路径：

- 模型直接返回“已保存”“已删除”等完成结果。
- 后端替 App 执行本地账本写入。
- 跳过确认节点执行写入类操作。
- 使用旧的待确认操作 ID 再次执行。

### 11.10 日志、数据和限流

官方后端允许记录：

- 请求时间
- 请求耗时
- 请求状态
- 错误类型
- 模型供应商
- 操作类型
- 任务阶段
- 上下文请求类型
- 操作数量
- 用户输入
- 模型回答
- 账本数据
- 反馈事件
- 用户偏好
- 模型调用 token 和成本
- 评估分数

禁止记录：

- API Key
- 用户自定义 API Key
- 设备原始标识

首版官方免费模型服务按设备限流。

建议默认限制：

- 每分钟请求数
- 每日请求数
- 单次请求最大上下文大小
- 单次模型最大输出长度
- 单用户长期存储体积
- 后台优化任务频率

超限时返回明确提示，不继续调用模型供应商。

### 11.11 反馈、自我优化和动态引导

自我优化不是让模型直接改代码，而是让系统基于真实使用数据优化配置、提示词、工具路由、上下文选择和个性化记忆。

数据来源：

- 点赞、点踩、重新生成。
- 用户追问、取消确认、确认执行和执行失败。
- 用户手动纠正分类、账户、日期或金额。
- 对话完成率、重试次数、耗时和错误类型。
- 用户长期账本和复盘习惯。

优化结果：

- 更新用户个人记忆，例如常用账户、分类偏好和回答风格。
- 调整动态引导词优先级。
- 调整上下文选择策略，例如优先使用摘要或明细。
- 调整工具路由和错误恢复策略。
- 生成提示词改进候选和评估报告。

安全边界：

- 自动优化可以更新用户记忆和低风险配置。
- 提示词、工具路由和状态图结构的全局变更需要经过离线评估或人工确认后发布。
- 自我优化不能跳过用户确认，不能直接改账，不能自动发布未经验证的架构变更。

动态引导词生成流程：

1. 读取当天日期、时区、节奏信息和用户最近对话。
2. 读取用户账本摘要、偏好记忆和未完成任务。
3. 生成 2 到 4 条短引导词。
4. 过滤重复、过长、无关和风险类建议。
5. 返回展示原因，方便后续根据点击率优化。

### 11.12 测试要求

后端必测：

- 健康检查。
- 设备限流。
- 官方模型服务配置缺失。
- 用户自定义 API Key 缺失和无效。
- 限流。
- 状态图路由。
- 上下文请求和继续执行。
- 确认中断和恢复。
- SSE 事件顺序。
- 模型供应商 mock 调用。
- 结构化输出校验。
- 数据存储和上下文预算。
- 用户反馈写入和质量评分。
- 个性化记忆更新。
- 动态引导词生成和过滤。
- 异常返回。

Android 必测：

- 助手未配置状态。
- 服务器连接失败。
- 任务进度展示。
- 上下文请求后按范围读取数据。
- 待确认任务重进页面后可恢复。
- 点赞、点踩和重新生成图标可用且不遮挡正文。
- 动态引导词可展示、点击和刷新。
- 新增账目确认后保存。
- 修改账目确认后保存。
- 删除账目确认后删除。
- 取消确认后不写入。
- 重复确认同一操作不重复写入。
- 统计问答不写入。
- 原离线记账路径不受影响。

UI 自检范围：

- 助手一级页面。
- 设置页在线助手配置。
- 我的页承接原发现内容。

  <br />

## 12. 兼容说明

- 当前账户余额保存在 `accounts.initialBalanceCent`，写入账目时同步应用账目影响。
- 统计历史余额通过当前余额减去节点之后的账目影响反推。
- `preset_version` 版本 5 会补齐当前预置分类和图标规则。
- `account_adjustments` 仅用于兼容旧数据库结构，不进入新增、编辑、统计和备份主流程。
