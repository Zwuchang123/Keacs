# Keacs 技术架构

## 1. 架构目标

Keacs 采用“本地可信账本 + 可选在线 AI 助手”的架构。

目标：

- 基础记账离线可用，本地数据稳定可靠。
- 账本写入路径简单、统一、可校验。
- AI 能力可替换，不能绑定单一模型供应商。
- AI 只能生成候选结果和操作预览，不能直接写入账本。
- 架构保持轻量，不为未确认的未来能力预留复杂框架。

## 2. 系统边界

Android App：

- 提供全部本地记账、统计、账户、分类、备份和设置能力。
- 保存本地账本和用户本机配置。
- 展示 AI 助手对话、任务阶段、候选选择和操作预览。
- 在用户确认后，通过本地业务规则执行写入。

Agent 后端：

- 仅在官方在线助手模式下使用。
- 负责设备限流、任务编排、模型调用、上下文规划、结构化输出校验、流式事件、反馈和个性化记忆。
- 可以保存官方模式下的账本、对话、反馈和偏好数据，用于回答、恢复任务和优化体验。
- 不直接写入用户本地账本。

用户自定义模型服务：

- 由用户在 App 中配置访问地址、API Key 和模型名。
- App 直接调用该服务。
- 用户自定义 API Key 只保存在本机，不上传到 Keacs 后端。

大模型：

- 负责理解自然语言、生成回答和候选操作。
- 不可信任为最终事实来源。
- 输出必须经过结构化校验和本地业务校验。

## 3. 技术栈

Android 端：

- 平台：Android 原生
- 语言：Kotlin
- UI：Jetpack Compose
- 架构：MVVM + Repository + UseCase
- 本地数据库：Room + SQLite
- 偏好存储：DataStore
- 异步：Kotlin Coroutines + Flow
- 导入导出：JSON 文件、XLSX 文件读取
- 网络：HTTPS API、SSE
- 构建：Gradle

Agent 后端：

- 语言：Python
- Web 框架：FastAPI
- 数据校验：Pydantic
- HTTP 客户端：httpx
- Agent 编排：LangGraph 状态图或等价轻量状态图实现
- 流式事件：SSE
- 存储：SQLite；多实例部署时可升级 PostgreSQL
- 部署：Docker Compose + Caddy 或 Nginx HTTPS 反向代理

大模型接入：

- 供应商不写死。
- 优先按 OpenAI 兼容的访问地址、API Key 和模型名接入。
- 支持对话生成、结构化输出和 Tool Use。

## 4. 分层结构

Android 分层：

- UI Layer：Compose 页面、ViewModel、UiState。
- Domain Layer：UseCase、Domain Model、Business Rule。
- Data Layer：Repository、Room DAO、Entity、DataStore、备份和导入实现。

AI 助手本地边界：

- Agent UI 只负责展示对话、进度、候选和确认卡片。
- Agent Repository 负责配置读取、网络调用、SSE 消费和错误转换。
- Agent Context Provider 按任务读取必要的本地分类、账户、账目、统计和定时记账摘要。
- Agent Action Executor 只在用户确认后调用现有 UseCase。
- Agent Run Store 保存最近消息、任务阶段和待确认操作。

后端分层：

- API 层负责健康检查、任务流、上下文继续、确认继续、反馈和动态引导词。
- Agent 层负责状态图、上下文规划、模型调用、结构化校验和错误恢复。
- Security 层负责设备识别、限流和访问保护。
- Storage 层负责任务检查点、对话、账本、反馈、用户记忆和评估数据。

## 5. 核心数据模型

Category：

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

Account：

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

Record：

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

ScheduledRecord：

- `id`
- `type`
- `amountCent`
- `categoryId`
- `fromAccountId`
- `toAccountId`
- `frequency`
- `recurrenceValues`
- `nextRunAt`
- `note`
- `isEnabled`
- `createdAt`
- `updatedAt`

Agent 本地数据：

- 消息：最近助手消息，仅本机保存。
- 任务：任务状态、阶段、请求时间、完成时间和失败原因。
- 待确认操作：操作预览、一次性操作标识和确认状态。
- 反馈事件：点赞、点踩、重新生成、确认卡片字段修改、取消和执行失败。
- 用户习惯摘要：常用账户、常用分类、表达习惯和回答偏好。

## 6. 存储原则

- Room 数据库是本地账本唯一可信来源。
- 数据库名为 `keacs.db`。
- 金额统一使用分，类型为 Cent。
- 结构变更必须提供 Room Migration。
- 备份文件版本独立于数据库版本。
- 偏好配置使用 DataStore。
- 用户自定义 API Key 仅本机保存，禁止写入日志。
- 设备原始标识上传前必须转换为不可逆摘要。

主要数据表：

- `categories`
- `accounts`
- `records`
- `scheduled_records`
- `app_meta`
- `account_adjustments`，仅用于兼容旧数据库结构。

关键索引：

- `records.occurredAt`
- `records.type`
- `records.categoryId`
- `records.fromAccountId`
- `records.toAccountId`
- `scheduled_records.nextRunAt`

## 7. 关键业务规则

写入入口：

- 所有账本写入必须通过 UseCase。
- Agent、Repository 和后端都不能直接绕过 UseCase 写入账本。
- 新增、修改、删除、批量处理和定时记账写入，必须在用户确认后执行。

余额计算：

```text
income: toAccount += amount
expense: fromAccount -= amount
transfer: fromAccount -= amount, toAccount += amount
```

- 账户余额采用有符号数。
- 资产账户通常为正数，负债账户为负数。
- 账户性质不改变收入、支出和转账的余额方向。
- 总资产为资产账户余额合计。
- 总负债为负债账户余额合计，数值本身为负。
- 净资产 = 总资产 + 总负债。

备份导入：

- 仅合并导入。
- 不覆盖现有数据。
- 不做去重。
- 导入时重新生成本地 ID。
- 使用临时 ID 映射恢复关联关系。
- 全流程数据库事务。

## 8. 关键数据流

手动记账：

1. UI 收集用户输入。
2. ViewModel 调用对应 UseCase。
3. UseCase 执行业务校验。
4. Repository 写入 Room。
5. Flow 推动首页、统计和账户余额刷新。

定时记账：

1. 应用启动或进入相关页面时检查到期模板。
2. 到期模板通过现有记账 UseCase 生成账目。
3. 成功后推进下次生成时间。
4. 分类或账户不可用时停用模板并记录失败原因。

备份导入：

1. 读取 JSON 备份。
2. 校验版本和结构。
3. 生成临时 ID 映射。
4. 在事务内写入分类、账户和记录。
5. 导入完成后刷新账本视图。

官方 AI 助手：

1. 用户发送消息。
2. App 创建本地任务并读取必要摘要。
3. App 调用后端任务流接口。
4. 后端规划上下文、调用模型并返回 SSE 事件。
5. App 展示阶段、回答、候选或操作预览。
6. 写入类操作等待用户确认。
7. 用户确认后，App 通过 UseCase 写入本地账本。
8. App 把确认、取消、失败或反馈结果回传后端。

自定义模型服务：

1. App 直接调用用户配置的模型服务。
2. App 使用本地结构校验和安全拦截。
3. 写入类操作仍走本地预览、确认和 UseCase 执行。

## 9. Agent 架构规则

状态图阶段：

- 输入校验
- 意图识别
- 上下文规划
- 上下文读取
- 用户记忆读取
- 模型推理
- 操作构建
- 结构校验
- 用户确认
- 执行结果反馈
- 回答反馈
- 错误恢复

上下文原则：

- 单次模型调用按任务选择上下文。
- 常规查账优先使用统计摘要、用户偏好和检索结果。
- 改账、删账、大额和明细查询可读取候选明细。
- 用户明确要求全部、历史、累计、整体等分析时，可以使用全量账本。
- 上下文超过预算时先摘要、分段或检索，再调用模型。

安全原则：

- 模型不能直接调用数据库。
- 后端不能替 App 执行本地账本写入。
- 所有模型输出必须结构化校验。
- 所有写入必须经过 App 本地业务校验和用户确认。
- 待确认操作必须有一次性操作 ID。
- 已确认、已取消或已失败的操作不能再次执行。

## 10. 后端接口边界

官方后端需要提供：

- 健康检查。
- 任务流接口，返回 SSE 事件。
- 上下文继续接口，用于接收 App 本地读取结果。
- 确认继续接口，用于接收用户确认、取消或执行失败结果。
- 回答反馈接口，用于接收点赞、点踩和重新生成。
- 动态引导词接口，用于生成 2 到 4 条短建议。

SSE 事件至少覆盖：

- 任务开始
- 阶段变化
- 上下文请求
- 部分回复
- 操作预览
- 等待确认
- 最终回复
- 任务失败

接口返回必须面向 App 可展示状态，不能暴露模型提示词、内部工具参数或原始账本 JSON。

## 11. 技术决策

- 使用 Room + SQLite 作为本地账本存储，保证离线可用。
- 使用 UseCase 作为唯一业务写入入口，避免多处规则分叉。
- 使用 DataStore 保存偏好配置，避免把轻量设置塞入业务表。
- 使用 Cent 存储金额，避免浮点误差。
- 使用有符号负债余额，保持资产负债统计规则简单。
- 使用 HTTPS 和 SSE 支持在线助手流式进度。
- 使用 OpenAI 兼容模型接口作为优先适配形态，降低供应商绑定。
- 官方后端可以保存完整数据，但推理层必须按上下文预算取数。
- 自定义模型服务不经过 Keacs 后端，降低用户 API Key 暴露面。

## 12. 质量约束

安全与隐私：

- 未启用在线助手时，不发起 Agent 网络请求。
- API Key、设备原始标识和敏感配置禁止写入日志。
- 写入类 AI 操作不能跳过确认。
- 错误提示使用可读文案，不以错误码作为主要展示。

可靠性：

- 本地账本写入必须事务化处理。
- 备份导入失败时不能留下半导入状态。
- 模型失败或网络失败不能影响本地记账。
- 待确认任务重进页面后应可恢复或明确失效。

性能与资源：

- 长对话需要摘要或分段检索。
- 大量账本分析优先使用统计摘要。
- 后端需要限制单次上下文大小、输出长度、请求频率、每日成本和长期存储体积。
- 后台优化任务不能影响实时对话。

兼容性：

- `account_adjustments` 仅用于兼容旧数据库结构，不进入新增、编辑、统计和备份主流程。
- 当前账户余额保存在 `accounts.initialBalanceCent`。
- 统计历史余额通过当前余额减去节点之后的账目影响反推。
- 旧备份导入时需要转换负债账户余额为当前负数规则。

## 13. 构建与运行机制

Android：

- 使用 Gradle 构建。
- 调试构建必须能完成 Kotlin 编译、Debug APK 构建、Lint 和相关单元测试。
- 涉及数据库结构变化时，必须同步 Migration 和备份兼容规则。

Agent 后端：

- 使用 Docker Compose 部署。
- HTTPS 由 Caddy 或 Nginx 反向代理提供。
- 官方服务需要配置模型访问地址、API Key、模型名、限流和存储位置。
- 后端配置变化不能要求修改 Android 记账业务规则。
