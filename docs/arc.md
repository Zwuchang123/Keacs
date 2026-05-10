# 技术架构

<br />


## 1. 技术栈

- 平台：Android 原生
- 语言：Kotlin
- UI：Jetpack Compose
- 架构：MVVM + Repository + UseCase
- 本地数据库：Room + SQLite
- 偏好存储：DataStore
- 异步：Kotlin Coroutines + Flow
- 导入导出：JSON 文件
- 构建工具：Gradle

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
    account/
    category/
    stats/
    settings/
    backup/
  domain/
    model/
    usecase/
    rule/
  data/
    local/
      dao/
      entity/
      database/
      PreferencesManager
    repository/
    backup/
  common/
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

偏好存储：

- `has_welcomed`：记录本设备是否已经点击过进入页的“开始记账”，只保存在本机。

索引：

- `records.occurredAt`
- `records.type`
- `records.categoryId`
- `records.fromAccountId`
- `records.toAccountId`

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

### PreferencesManager

- 读取本设备首次进入状态
- 用户点击“开始记账”后写入本地标记

## 7. 业务规则入口

所有写操作必须通过 UseCase：

- `CreateIncomeUseCase`
- `CreateExpenseUseCase`
- `CreateTransferUseCase`
- `UpdateRecordUseCase`
- `DeleteRecordUseCase`
- `ImportBackupUseCase`
- `ExportBackupUseCase`

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

- Room 数据库版本从 `1` 开始，当前版本为 `2`。
- 结构变更必须提供 Migration。
- 备份文件版本独立于数据库版本。

## 11. 兼容说明

- 当前账户余额保存在 `accounts.initialBalanceCent`，写入账目时同步应用账目影响。
- 统计历史余额通过当前余额减去节点之后的账目影响反推。
- `preset_version` 版本 4 会把旧负债账户余额迁移为负数。
- `account_adjustments` 仅用于兼容旧数据库结构，不进入新增、编辑、统计和备份主流程。
