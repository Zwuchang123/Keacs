## 项目概览

- 项目名称：Keacs。
- 产品方向：个人记账 App。
- 产品要求：简单、轻量、便捷、智能。
- 产品目标：一个拥有AI能力的便捷记账工具。

## 项目文档

- 需求文档：`docs\prd.md`。
- 技术路线：`docs\arc.md`。
- Agent 架构速览：`docs\agent-arc.md`。
- 设计规范：`docs\design.md`。
- 代码映射：`docs\code-map.md`。
- 测试要求：`docs\testing.md`。
- 部署与发版：`docs\deploy.md`。

## 工作流程

1. 必须先判断任务类型，分析用户需求与影响范围;
2. 新增功能或原有功能优化时，必须主动借鉴行业优秀案例，主动思考更好的实现方案；
3. 仅获取必要信息，利用文档目录和代码映射等找到必要信息，避免每次都读取全量文档；
4. 编码前先思考，不要假设，不要隐藏困惑。要暴露权衡
5. 先计划再执行，将任务转化为可验证的步骤；
6. 开发完成后按 `docs\testing.md` 进行检查；
7. 经用户确认验收通过后，更新必要文档，对于代码开发任务，须将改进内容简短更新至 `docs\releases\next.md`；再主动执行提交。

## 目标驱动执行 

**定义成功标准。循环验证直到完成。**

将任务转化为可验证的目标：

| 不要说...   | 转化为...                |
| -------- | --------------------- |
| "添加验证"   | "为无效输入编写测试，然后让它们通过"   |
| "修复 bug" | "编写一个能复现它的测试，然后让测试通过" |
| "重构 X"   | "确保重构前后测试都能通过"        |

对于多步骤任务，简述计划：

```
1. [步骤] → 验证：[检查项]
2. [步骤] → 验证：[检查项]
3. [步骤] → 验证：[检查项]
```

## 代码开发要求

- 必须遵守`docs\arc.md`中的约定
- 只做当前需求，禁止为未来假设提前设计、顺手重构或扩大范围。
- 优先复用现有组件、模式、库和工具；只有能实际降低复杂度时才新增抽象。
- 只修改与当前任务直接相关的文件，清理自己引入的临时文件、死代码和无用文件。
- 发现无关问题可以简短提示，但不要借机扩改。
- 前端必须遵守现有组件风格和设计规范：`docs\design.md`；涉及前端重构、新界面或大功能迭代时，使用 `.agents\skills\impeccable`。
- 代码命名、分层、职责和错误处理必须遵循现有风格，禁止硬编码、吞异常或暴露敏感信息。
- 注释使用中文，只写必要说明，不写冗余注释。
- 单文件尽量控制在 300 行以内；超过 400 行必须评估是否拆分；超过 600 行一般视为需要重构。
- 修复bug前请先复现。

## 部署与发版索引

版本号、发版记录、APK 发布、后端部署和服务器运维流程统一查看 `docs\deploy.md`，不要在其他文档重复维护同一套流程。

## 文档管理

- `prd.md` 必须精简、可验收：说明问题、目标、用户场景、功能需求、非目标、验收标准和成功指标。
- `arc.md` 必须精简、可执行：说明架构目标、系统边界、核心模块、关键数据流、数据/存储原则、技术决策和质量约束。
- `code-map.md` 只写模块、入口或路径变化。
- 禁止把实现计划、发版流程或代码路径混入 `prd.md` / `arc.md`。

## 提交要求

- 提交说明使用中文，清楚说明本次变更。
- 提交前必须查看 `git status --short`，只提交与当前任务直接相关的文件。

## 常用命令

### 服务器部署

腾讯云服务器部署、发版和运维命令统一查看 `docs\deploy.md`。

### Android 本地开发

- 查看设备：`adb devices`
- 设置 Android SDK 路径：`export ANDROID_HOME="$HOME/Library/Android/sdk"; export ANDROID_SDK_ROOT="$ANDROID_HOME"`
- 查看模拟器：`"$ANDROID_HOME/emulator/emulator" -list-avds`
- 启动模拟器：`nohup "$ANDROID_HOME/emulator/emulator" -avd Pixel6_API34 -no-snapshot -no-audio -no-boot-anim >/tmp/keacs-emulator.log 2>&1 &`
- 等待模拟器开机：`serial='emulator-5554'; until [ "$(adb -s "$serial" get-state 2>/dev/null)" = "device" ] && [ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 5; done`
- 安装应用：`./gradlew :app:installDebug`
- 启动应用：`adb shell am start -n com.keacs.app/.MainActivity`
- 检查联网权限：`rg -n "INTERNET|ACCESS_NETWORK_STATE|uses-permission" app/src/main/AndroidManifest.xml`
- 提交前查看：`git status --short`

