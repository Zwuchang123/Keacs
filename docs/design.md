# Keacs 前端设计规范

## 1. 风格定位

| 项目 | 规范 |
| --- | --- |
| 产品气质 | 简单、轻量、清爽、可信赖 |
| 图片风格 | 浅色背景、蓝色主操作、近白圆角卡片、彩色圆形分类图标、底部导航、轻阴影、低干扰图表 |
| 设计关键词 | Clean Mobile、Soft Cards、Finance Minimal、Fast Entry |
| 首屏目标 | 看余额、看最近记录、快速记账 |
| 禁止出现 | 登录、云同步、会员、营销横幅、复杂理财入口、装饰性插画堆叠 |

## 2. 颜色

| 角色 | 色值 | 用途 |
| --- | --- | --- |
| Primary | `#3F82F6` | 主按钮、选中态、重点图表线 |
| Primary Light | `#EAF2FF` | 选中背景、浅蓝提示 |
| Background | `#F6F8FC` | 页面背景 |
| Surface | `#FCFDFF` | 卡片、输入框、列表 |
| Surface Subtle | `#F1F4F8` | 分段控件、键盘块、搜索框 |
| Text Primary | `#1F2937` | 主文字、金额 |
| Text Secondary | `#6B7280` | 副标题、说明值 |
| Text Tertiary | `#A0A7B3` | 占位、弱信息 |
| Border | `#E6EBF2` | 分割线、输入框边 |
| Focus | `#2563EB` | 焦点环、键盘选中态 |
| Income | `#35C785` | 收入、正向金额 |
| Expense | `#FF5A5F` | 支出、负向金额 |
| Warning | `#FFB020` | 导入确认、风险提示 |
| Error | `#E5484D` | 删除、失败 |

### 分类色

| 分类 | 色值 |
| --- | --- |
| 餐饮 | `#FFB35C` |
| 交通 | `#6EA8FF` |
| 购物 | `#49C58F` |
| 住房 | `#FFB35C` |
| 娱乐 | `#B984F6` |
| 医疗 | `#FF7A7A` |
| 教育 | `#56C8A5` |
| 通讯 | `#73A8FF` |
| 日用 | `#64B5F6` |
| 工资 | `#4A8DFF` |
| 其他 | `#C8CDD6` |

## 3. 字体

| 场景 | 字号 | 字重 | 行高 |
| --- | --- | --- | --- |
| 页面标题 | `18sp` | `600` | `26sp` |
| 模块标题 | `15sp` | `600` | `22sp` |
| 正文 | `14sp` | `400` | `20sp` |
| 辅助文字 | `12sp` | `400` | `17sp` |
| 大金额 | `30sp` | `700` | `38sp` |
| 卡片金额 | `24sp` | `700` | `32sp` |
| 底部导航 | `11sp` | `500` | `14sp` |

字体使用 Android 系统默认字体。金额使用等宽数字。禁止负字距。

## 4. 间距与形状

| 项目 | 规范 |
| --- | --- |
| 基础单位 | `4dp` |
| 页面左右边距 | `16dp` |
| 卡片内边距 | `16dp` |
| 模块间距 | `16dp` |
| 列表项间距 | `8dp` |
| 卡片圆角 | `12dp` |
| 输入框圆角 | `8dp` |
| 主按钮圆角 | `10dp` |
| 分类图标容器 | `40dp` 圆形 |
| 底部中间按钮 | `56dp` 圆形 |
| 最小点击区域 | `48dp x 48dp` |
| 卡片阴影 | `1dp-2dp`，不使用重阴影 |

## 5. 页面结构

### 通用页面

- 使用 `KeacsScaffold`。
- 顶部保留系统状态栏安全距离。
- 内容区底部避开底部导航和键盘。
- 页面背景统一使用 `Background`。
- 主要内容使用近白卡片或近白列表区。
- 页面标题居中或左对齐保持同页一致。

### 首页

- 顶部：本月结余卡片。
- 中部：记账、账户、图表、更多四个快捷入口。
- 下部：最近记录。
- 底部：五栏导航，中间为新增按钮。
- 首页不放低频设置项。

### 账单页

- 顶部：月份选择、搜索、更多。
- 记录按日期分组。
- 每组显示收入合计、支出合计。
- 列表项包含分类图标、标题、备注、时间、账户、金额。
- 收入金额绿色，支出金额红色，转账金额使用正文色。

### 新增记录页

- 顶部：返回、标题、保存。
- 类型使用分段控件。
- 分类使用横向圆形图标。
- 金额输入优先显示。
- 金额键盘固定在底部。
- 保存按钮只在金额合法时可用。
- 备注为空时不占用醒目位置。

### 账户页

- 顶部：净资产卡片。
- 账户按资产、负债分组。
- 列表项显示账户图标、名称、类型、余额。
- 停用账户降低透明度，不参与新建记录选择。

### 分类页

- 收入分类、支出分类使用分段控件。
- 分类项显示图标、名称、进入箭头。
- 新增分类按钮固定在列表底部或页面底部。

### 统计页

- 顶部：支出、收入、资产分段控件。
- 时间维度：日、月、年。
- 趋势图使用折线或面积图。
- 分类占比使用环形图。
- 排行只展示前 5 项，其余合并为其他。

### 设置页

- 使用分组列表。
- 仅放默认账户、默认分类、金额显示、导入导出、缓存等必要项。
- 危险操作使用二次确认。

## 6. 统一组件

| 组件 | 规范 |
| --- | --- |
| `KeacsScaffold` | 页面外壳、背景、状态栏、底部安全区 |
| `KeacsTopBar` | 返回、标题、右侧操作 |
| `KeacsBottomBar` | 首页、账单、新增、图表、我的 |
| `KeacsCard` | 近白底、`12dp` 圆角、轻阴影 |
| `OverviewCard` | 蓝底、余额、收入、支出 |
| `QuickActionGrid` | 2-4 个快捷入口，不超过 4 个 |
| `RecordListItem` | 固定高度不小于 `64dp` |
| `CategoryIcon` | 圆形底色、白色线性图标 |
| `AccountListItem` | 账户名、账户类型、余额、箭头 |
| `SegmentedTabs` | 白底容器、蓝色选中态 |
| `AmountInput` | 大金额、人民币符号、等宽数字 |
| `NumberPad` | 3 列数字键、删除键、确认态 |
| `FormFieldRow` | 图标、标题、值、箭头 |
| `ChartCard` | 标题、主指标、图表、简短趋势 |
| `EmptyState` | 空图标、短文案、一个主操作 |
| `ConfirmDialog` | 标题、正文、取消、确认 |
| `KeacsSnackbar` | 保存失败、导入失败、删除完成 |

### 组件状态

- 所有可点击组件必须包含默认、按下、焦点、禁用、加载、错误状态；禁用状态仍需可读。
- 同类操作只使用一种组件样式，焦点颜色使用 `Focus`。

## 7. 图标

| 场景 | 规范 |
| --- | --- |
| 图标来源 | Compose Material Icons Rounded |
| 图标风格 | 圆角、统一线宽，全局只使用同一套 |
| 常规尺寸 | `24dp` |
| 分类图标 | `20dp`，白色，放入 `40dp` 圆形底 |
| 底部导航 | `24dp`，选中蓝色，未选中灰色 |
| 禁止 | Emoji、混用多套图标、位图图标、无含义装饰图标 |

### 业务图标映射

- 导航：首页 `home`、账单 `receipt_long`、新增 `add`、图表 `monitoring`、我的 `person`。
- 操作：搜索 `search`、更多 `more_horiz`、返回 `AutoMirrored.Rounded.ArrowBack`、保存使用文本按钮。
- 分类：餐饮 `restaurant`、交通 `directions_bus`、购物 `shopping_bag`、住房 `home`、娱乐 `sports_esports`、医疗 `local_hospital`、教育 `school`、工资 `work`、其他 `category`。
- 账户：现金 `payments`、银行卡 `credit_card`、钱包 `account_balance_wallet`。

## 8. 动效

| 场景 | 时长 | 动效 |
| --- | --- | --- |
| 页面进入 | `220ms` | `fadeIn + slideInHorizontally` |
| 页面退出 | `180ms` | `fadeOut + slideOutHorizontally` |
| 卡片出现 | `180ms` | `fadeIn + slideInVertically(8dp)` |
| 列表增删 | `180ms` | 使用 Compose 官方 LazyList 项位移动画 |
| 分段切换 | `160ms` | 背景滑动、文字变色 |
| 按钮按下 | `80ms` | 透明度或涟漪反馈 |
| 金额变化 | `180ms` | 数字淡入，不滚动跳字 |
| 图表加载 | `300ms` | 线条绘制或透明度渐入 |
| 弹窗 | `180ms` | 淡入、轻微缩放 |

### 动效限制

- 只使用 Compose Animation。
- Lottie 不默认引入；仅在不明显增加体积时用于导入导出结果等一次性反馈。
- 禁止循环装饰动画。
- 禁止缩放导致布局跳动。
- 只动画透明度、位移和颜色，避免动画改变布局尺寸。
- 遵循系统“移除动画”和动画缩放设置。

## 9. 图表

| 图表 | 用途 | 规范 |
| --- | --- | --- |
| 折线图 | 收入、支出、净收支趋势 | 蓝色主线，填充透明度 `12%-20%` |
| 面积图 | 月度趋势 | 仅单指标使用 |
| 环形图 | 分类占比 | 中心留白，不做 3D |
| 横向条形图 | 分类排行、账户排行 | 颜色与分类一致 |
| 指标卡 | 总资产、总负债、净资产 | 大数字 + 小趋势 |

图表坐标、标签和图例必须可读。颜色不能作为唯一含义。

## 10. 表单

- 金额必须优先输入。
- 金额键盘使用数字键盘。
- 金额默认保留两位小数。
- 金额为 `0`、空值、非法字符时禁止保存。
- 日期默认当前时间。
- 分类和账户缺失时显示明确错误。
- 保存中禁用重复点击。
- 保存失败保留已输入内容。
- 删除、导入、清空缓存使用确认弹窗。

## 11. 状态

| 状态 | 规范 |
| --- | --- |
| 加载 | 骨架或小型进度，不遮挡已存在内容 |
| 空账单 | 显示空图标、短文案、记一笔按钮 |
| 空分类 | 显示新增分类入口 |
| 空账户 | 显示新增账户入口 |
| 保存失败 | Snackbar + 保留表单 |
| 导入失败 | 弹窗显示失败原因 |
| 删除成功 | Snackbar |
| 无权限 | 明确提示文件读写权限 |
| 大字体 | 不截断金额，不挤压按钮 |

## 12. 无障碍

- 文字对比度不低于 `4.5:1`。
- 点击区域不小于 `48dp x 48dp`。
- 图标按钮必须有 `contentDescription`。
- 金额颜色必须配合正负号或文案。
- 支持系统大字体。
- 键盘弹出时输入框不可被遮挡。
- TalkBack 顺序与视觉顺序一致。
- 搜索、筛选、删除、保存可被键盘和辅助功能触发。

## 13. Compose 约束

- 使用 Jetpack Compose + Material 3。
- 主题入口统一为 `KeacsTheme`。
- 颜色、字体、圆角、间距统一从主题或设计 token 获取。
- 业务页面不得硬编码颜色和尺寸。
- 页面状态使用不可变 `UiState`。
- 列表使用稳定 `key`。
- 导航使用类型安全路由。
- ViewModel 发出一次性导航事件。
- 预览覆盖浅色、大字体、空状态、错误状态。

## 14. 参考库

| 类型 | 名称 | 链接 |
| --- | --- | --- |
| 组件 | Jetpack Compose Material 3 | https://developer.android.com/develop/ui/compose/designsystems/material3 |
| 组件规范 | Material Design 3 Components | https://m3.material.io/components |
| 图标 | Compose Material Icons | https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons |
| 圆角图标 | Icons.Rounded | https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons.Rounded |
| 动画 | Compose Animation | https://developer.android.com/develop/ui/compose/animation/introduction |
| 动画素材 | Lottie Android | https://github.com/airbnb/lottie-android |
| 图表 | Vico Compose Charts | https://guide.vico.patrykandpatrick.com/android/compose/overview |
| 无障碍 | Jetpack Compose Accessibility | https://developer.android.com/codelabs/jetpack-compose-accessibility |
| 导航 | Navigation with Compose | https://developer.android.com/jetpack/compose/navigation |

## 15. 验收清单

- 页面背景、卡片、图标、按钮风格一致。
- 底部导航、顶部栏、卡片、列表项使用统一组件。
- 没有 Emoji 图标。
- 没有营销、登录、联网、云同步入口。
- 没有文字重叠、按钮文字溢出、横向滚动。
- 金额、日期、分类、账户输入路径清晰。
- 保存失败、删除确认、导入失败有明确反馈。
- 图表简单可读，不使用 3D 或复杂金融图。
- 支持 `360dp` 宽度、小屏、大字体。
- 动效不影响记账速度。
- 所有点击项有可见反馈。
- 所有图标按钮有无障碍描述。
