# 技术架构

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
balance = initialBalance + recordEffects
```

其中 recordEffects 为该账户关联的收入、支出和转账记录的影响总和。

资产负债：

```text
totalAsset = sum(asset account balances)
totalLiability = sum(liability account balances)
netAsset = totalAsset - totalLiability
```

## 9. 备份结构

```json
{
  "backupVersion": 1,
  "exportedAt": 0,
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

## 10. 数据迁移

- Room 数据库版本从 `1` 开始，当前版本为 `2`。
- 结构变更必须提供 Migration。
- 备份文件版本独立于数据库版本。

##
