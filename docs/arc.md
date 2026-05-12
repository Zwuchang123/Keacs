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

### 0.2 Agent 助手开发定位索引

| 开发内容       | 对应章节                      | 说明                                        |
| ---------- | ------------------------- | ----------------------------------------- |
| 技术栈变化      | `1. 技术栈`                  | 增加 Agent 后端、通用大模型接入和 HTTPS API 要求           |
| App 目录调整   | `3. 目录结构`                 | 增加 `ui/agent`、`domain/agent`、`data/agent` |
| 在线助手配置     | `5. 数据库`                  | 增加 DataStore 配置项                          |
| Agent 服务边界 | `6. 核心服务`                 | 明确 App 端、后端和模型各自职责                        |
| 写入入口       | `7. 业务规则入口`               | Agent 写入必须复用现有 UseCase                    |
| Agent 总体架构 | `11. Agent 助手架构`          | Agent 独立开发的主要架构集中在本章节                     |
| 后端接口       | `11.7 后端接口`               | 定义健康检查、对话、执行反馈接口                          |
| 安全规则       | `11.9 安全规则`、`11.10 日志和限流` | 明确确认执行、日志、限流和权限边界                         |

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
- 轻量存储：SQLite
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
- `agent_data_scope`：在线助手数据上传范围，首版固定为最小必要。
- `agent_conversation_snapshot`：助手最近对话快照，仅保存在本机，用于退出后恢复对话和继续追问。

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
- 调用 Agent 后端对话接口
- 调用 Agent 后端执行结果反馈接口
- 处理网络失败、API Key 无效、官方服务限流和结构化返回异常
- 模型失败时对常见记账和查账请求提供本地可读兜底

### AgentContextProvider

- 按需读取分类列表
- 按需读取账户列表
- 按需读取候选账目
- 按需读取统计摘要
- 按需读取定时记账摘要
- 禁止默认读取和上传全量账本

### AgentBackend

- 识别设备并执行限流
- 限制请求频率
- 按服务端配置调用大模型供应商
- 校验模型结构化输出
- 返回 App 可展示的回答、追问或操作预览
- 保存不含账本内容的元信息日志

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

Agent 助手采用“本地账本 + 在线理解”的架构。

核心原则：

- 本地 Room 数据库仍是账本唯一可信来源。
- 大模型只负责理解用户意图和生成结构化操作。
- Agent 后端只负责模型调用、安全校验和接口保护。
- App 展示操作预览，用户确认后通过现有 UseCase 执行写入。
- 未启用在线助手时，原离线功能不受影响。

### 11.1 总体结构

```text
Android App
  Compose 助手页面
  Agent ViewModel
  Agent Repository
  AgentContextProvider
  现有 UseCase
  Room 数据库

Agent Backend
  FastAPI
  设备识别
  限流
  Agent 编排
  大模型适配层
  输出结构校验
  元信息日志

Model Provider API
  访问地址
  API Key
  模型名
  Tool Use 或结构化输出
```

数据流：

1. 用户在“助手”页输入问题。
2. App 根据本次问题准备最小必要上下文。
3. App 带上必要的近期对话和最小必要上下文。
4. App 根据模型服务模式选择调用官方 Agent 后端或用户自定义模型服务。
5. 官方模式下，App 把用户输入、近期对话、上下文和设备限流标识发给 Agent 后端。
6. 官方后端校验请求格式，按设备限流，并调用服务端配置的大模型供应商。
7. 自定义模式下，App 直接调用用户配置的访问地址，并使用用户本机保存的 API Key。
8. App 或后端校验模型返回的结构化结果。
9. App 展示回答、追问或待确认操作，并展示响应耗时。
10. 用户确认后，App 通过现有 UseCase 写入本地数据库。
11. 官方模式下，App 把执行成功或失败的元信息反馈给后端；自定义模式不向 Keacs 后端反馈会话内容。

### 11.2 Android 端模块

建议新增目录：

```text
app/src/main/java/com/keacs/app/ui/agent
app/src/main/java/com/keacs/app/domain/agent
app/src/main/java/com/keacs/app/data/agent
```

职责：

- `ui/agent`：助手页面、消息列表、输入框、操作预览卡片、确认弹窗。
- `domain/agent`：Agent 操作模型、确认执行规则、上下文读取用例。
- `data/agent`：网络请求、DTO、连接配置读取。

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

上下文选择规则：

- 单日、单周、单月、单年问题只读取对应范围。
- 最近 7 天、最近 30 天、最近 3 个月、最近半年、最近一年按滚动区间读取。
- 用户明确提出全部、所有、历史、累计、从开始、总共、整体等问题时，允许读取并上传全部账单明细。
- 用户没有明确要求明细分析时，优先上传统计摘要；改账、删账、大额和明细查询会上传候选账目。

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
      orchestrator.py
      prompts.py
      schemas.py
      minimax_client.py
      validators.py
    security/
      access_code.py
      rate_limit.py
    storage/
      audit_log.py
  tests/
  Dockerfile
  docker-compose.yml
```

后端负责：

- 识别设备并执行限流。
- 限制请求频率。
- 按服务端配置调用大模型供应商。
- 维护模型系统提示词。
- 校验模型结构化输出。
- 返回 App 可展示的回答、追问或操作预览。
- 保存不含账本内容的元信息日志。

后端不负责：

- 保存完整账本。
- 保存完整聊天内容。
- 直接写入用户账本。
- 判断用户最终是否执行写入。

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

对话接口：

```http
POST /api/agent/chat
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
  "localContext": {
    "timeContext": {},
    "categories": [],
    "accounts": [],
    "records": [],
    "stats": {},
    "scheduledRecords": []
  },
  "timezone": "Asia/Shanghai",
  "appVersion": "1.2.2"
}
```

`conversationHistory` 只传必要的近期对话，App 需要限制条数和总长度，避免上传过长历史。

`localContext.records` 由 App 按问题范围选择，可能是单日、单周、单月、单年、最近区间或全部账单明细。用户明确要求全量分析时，允许传全部账单；其他场景不上传无关账单明细。

该接口用于官方免费模型服务。自定义模型服务由 App 直接调用用户配置的模型访问地址，不经过 Keacs 后端。

返回字段：

```json
{
  "reply": "string",
  "needsMoreContext": false,
  "contextRequests": [],
  "actions": [],
  "warnings": []
}
```

执行结果反馈：

```http
POST /api/agent/feedback
```

请求字段：

```json
{
  "clientRequestId": "uuid",
  "deviceIdHash": "string",
  "result": "confirmed | cancelled | failed",
  "actionTypes": [],
  "errorType": "string"
}
```

反馈接口只用于官方免费模型服务，不上传完整账本、完整会话和用户自定义 API Key。

### 11.8 核心数据类型

`AgentContextRequest` 用于后端请求 App 补充本地上下文。

类型包括：

- `category_list`
- `account_list`
- `record_candidates`
- `month_stats`
- `account_summary`
- `scheduled_record_list`

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

- 操作类型
- 操作标题
- 操作说明
- 影响条数
- 账目明细
- 定时记账明细
- 风险提示

`AgentChatResult` 用于 App 渲染助手返回。

字段包括：

- 回复文本
- 是否需要补充上下文
- 上下文请求
- 待确认操作
- 警告信息

### 11.9 安全规则

模型不能直接调用数据库，也不能直接执行写入。

写入必须同时满足：

1. 后端结构校验通过。
2. App 本地业务校验通过。
3. 用户在 App 上确认。
4. App 通过现有 UseCase 执行。

以下情况必须追问或让用户选择：

- 候选账目不唯一。
- 分类无法确定。
- 账户无法确定。
- 金额缺失。
- 日期缺失且无法从上下文合理推断。
- 批量影响数量过大。

### 11.10 日志和限流

后端只记录元信息：

- 请求时间
- 请求耗时
- 请求状态
- 错误类型
- 模型供应商
- 操作类型

禁止记录：

- 用户完整输入
- 模型完整回答
- 完整账本内容
- API Key
- 用户自定义 API Key
- 设备原始标识

首版官方免费模型服务按设备限流。

建议默认限制：

- 每分钟请求数
- 每日请求数
- 单次请求最大上下文大小
- 单次模型最大输出长度

超限时返回明确提示，不继续调用模型供应商。

### 11.11 测试要求

后端必测：

- 健康检查。
- 设备限流。
- 官方模型服务配置缺失。
- 用户自定义 API Key 缺失和无效。
- 限流。
- 模型供应商 mock 调用。
- 结构化输出校验。
- 异常返回。

Android 必测：

- 助手未配置状态。
- 服务器连接失败。
- 新增账目确认后保存。
- 修改账目确认后保存。
- 删除账目确认后删除。
- 取消确认后不写入。
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
