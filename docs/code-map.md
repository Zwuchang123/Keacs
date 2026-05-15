# Code Map

| 模块       | 路径                                                                       | 作用                       |
| -------- | ------------------------------------------------------------------------ | ------------------------ |
| 应用启动     | `app/src/main/java/com/keacs/app/MainActivity.kt`                        | 启动入口，处理欢迎页、本地初始化、进入主应用   |
| 主导航      | `app/src/main/java/com/keacs/app/ui/KeacsApp.kt`                         | 串起首页、统计、新增、助手、我的及二级页面    |
| UI 层     | `app/src/main/java/com/keacs/app/ui`                                     | Compose 页面、组件、导航、主题      |
| 首页       | `app/src/main/java/com/keacs/app/ui/home`                                | 首页与首页状态                  |
| 记录       | `app/src/main/java/com/keacs/app/ui/record`                              | 新增/编辑记录、账目详情、选择器         |
| 统计       | `app/src/main/java/com/keacs/app/ui/stats`                               | 收入、支出、结余统计与图表            |
| 助手       | `app/src/main/java/com/keacs/app/ui/agent`                               | 在线助手一级页面、消息列表、确认卡片、确认卡片分页与底部编辑弹窗、输入框、会话保留和清空 |
| 管理       | `app/src/main/java/com/keacs/app/ui/management`                          | 分类管理、账户管理、图标与颜色映射        |
| 定时记账     | `app/src/main/java/com/keacs/app/ui/scheduled`                           | 定时记账列表、编辑和启停              |
| 设置       | `app/src/main/java/com/keacs/app/ui/settings`                            | 我的、设置、关于     |
| 通用组件     | `app/src/main/java/com/keacs/app/ui/components`                          | 卡片、底栏、数字键盘、弹窗等通用 UI      |
| Domain 层 | `app/src/main/java/com/keacs/app/domain`                                 | 业务模型、UseCase、规则          |
| 余额规则     | `app/src/main/java/com/keacs/app/domain/rule/RecordCalculations.kt`      | 余额、历史余额、收支汇总规则           |
| Data 层   | `app/src/main/java/com/keacs/app/data`                                   | 本地存储、仓库、偏好、备份            |
| Agent App 数据 | `app/src/main/java/com/keacs/app/data/agent`                             | 助手配置读取、网络请求、任务模型、事件归并、本地待确认操作拦截、可编辑确认动作、动态引导、默认账户上下文、按问题范围选择账单上下文、模型异常本地兜底和确认后操作执行 |
| 仓库入口     | `app/src/main/java/com/keacs/app/data/repository/LocalDataRepository.kt` | 分类、账户、记录、初始化、备份导入的核心入口   |
| 定时记账仓库   | `app/src/main/java/com/keacs/app/data/repository/ScheduledRecordRepository.kt` | 定时记账模板保存和到期账目生成       |
| 本地数据     | `app/src/main/java/com/keacs/app/data/local`                             | 数据库、DAO、Entity、预置数据、偏好设置 |
| 预置数据     | `app/src/main/java/com/keacs/app/data/local/database/PresetSeedData.kt`  | 预置分类、预置账户、默认图标颜色         |
| 偏好设置     | `app/src/main/java/com/keacs/app/data/local/PreferencesManager.kt`       | 欢迎页状态、默认记账账户、默认记账类型      |
| 备份       | `app/src/main/java/com/keacs/app/data/backup/BackupService.kt`           | JSON 备份导出与导入             |
| Excel 添加  | `app/src/main/java/com/keacs/app/data/importer/ExcelRecordImportService.kt` | 读取 XLSX 并添加收入、支出账目      |
| 单元测试     | `app/src/test`                                                           | 单元测试                     |
| 设备测试     | `app/src/androidTest`                                                    | Android 设备测试             |
| Agent 后端  | `server/app`                                                             | FastAPI 服务、健康检查、官方助手接口、任务流接口、上下文继续、确认继续、反馈、动态引导、限流、日志、模型通路和安全提示词 |
| Agent 后端状态 | `server/app/storage/agent_run_store.py`                                  | 保存任务阶段、待确认动作、上下文观察和反馈的轻量运行状态 |
| Agent 部署  | `server/Dockerfile`、`server/docker-compose.yml`、`server/Caddyfile`       | 官方助手后端容器化部署和 HTTPS 反向代理配置 |
| Agent 后端测试 | `server/tests`                                                           | 后端健康检查、mock 模型、限流和日志安全测试 |
| 平台公共组件文档 | `docs/components.md`                                                     | Markdown、语音转文字、Agent 卡片、确认卡片、权限提示等公共组件契约 |
| 模块文档框架 | `docs/modules`                                                           | 记账、待办、搜索/攻略、资产观察等 Capability Modules 的轻量 PRD |
| 当前 Markdown 实现 | `app/src/main/java/com/keacs/app/ui/agent/AgentMessages.kt`              | 当前 Android 助手 Markdown 渲染仍在 Agent 消息内部，后续平台化时应提取为公共文本组件 |
| 语音转文字规划 | `docs/components.md`                                                     | 当前仅作为公共输入组件规划，不改变 Android 首版只支持文字输入的范围 |
