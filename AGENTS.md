## 项目概览

- 项目名称：Keacs。
- 产品方向：个人记账 App。
- 产品要求：简单、轻量、便捷、智能。
- 产品目标：一个拥有AI能力的便捷记账工具。

## 项目文档

- 需求文档：`docs\prd.md`。
- 技术路线：`docs\arc.md`。
- 设计规范：`docs\design.md`。
- 代码映射：`docs\code-map.md`。
- Keacs Agent 助手开发计划：`docs\agents-plan.md`。

## 工作流程

先判断任务类型，再只读取和修改必要内容；测试范围统一按“测试要求”执行，不要扩大范围。

行动前先制定计划，列清楚 task list，再按计划执行；执行中持续迭代分析、执行、验证闭环，直到对当前策略有 100% 把握。

不要假设用户一定是对的，运用第一性原理，主动考虑影响范围，思考更合理的方案并让用户确认。

| 任务类型               | 读取范围                                                               | 执行范围                                                                                                                                                         |
| ------------------ | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 非代码开发任务            | 只读任务相关文档                                                           | 不改代码                                                                                                                                                         |
| 小功能优化              | 先看 `docs\code-map.md`，再读相关代码                                       | 只改当前功能相关代码；有需求变化时同步 `docs\prd.md` 对应章节                                                                                                                       |
| BUG 修复             | 先看 `docs\code-map.md`，再读缺陷相关代码                                     | 先复现，再修复，不扩大范围                                                                                                                                                |
| 大功能迭代              | 读取 `docs\prd.md`、`docs\arc.md`、`docs\design.md`、`docs\code-map.md` | 进入计划模式，并调用 Codex 已安装的 Superpowers 插件。先使用Superpowers: Brainstorming技能对需求进行详细分析，与用户沟通不清晰的内容；然后确认方案；再更新文档和代码                                                    |
| Keacs Agent 助手开发任务 | 读取`docs\prd.md`、`docs\arc.md`对应章节，严格按照`docs\agents-plan.md`开发阶段执行。 | 在Keacs-agent分支中执行Keacs Agent 助手全部开发任务，每个阶段完成后需主动更新`docs\code-map.md`，并在分支内执行提交。前端须遵守现有组件风格和设计规范，前端开发需使用 `.agents\skills\impeccable`。每个阶段都要按“测试要求”识别改动范围执行自测。 |

<br />

<br />

## 交付与发版决策

核心规则：验收失败不合并、不发版；验收通过后合并到 `master` 并删除短分支；是否发版只看是否要发布 APK。

| 场景                 | 发版      | 合并 `master`                              | 删除短分支   | 必做记录                             |
| ------------------ | ------- | ---------------------------------------- | ------- | -------------------------------- |
| 非代码开发任务            | 不发版     | 验收通过后合并                                  | 合并后删除   | 按需更新相关文档                         |
| 小功能优化，暂不上线         | 不发版     | 验收通过后合并                                  | 合并后删除   | 写入 `docs\releases\next.md`       |
| BUG 修复，可等待发版       | 不发版     | 验收通过后合并                                  | 合并后删除   | 写入 `docs\releases\next.md`       |
| BUG 修复，需要立刻给用户更新   | 发补丁版本   | 发版准备完成后合并                                | 标签推送后删除 | 写入 `docs\releases\vX.Y.Z.md`     |
| 小功能优化，确认上线         | 发补丁或小版本 | 发版准备完成后合并                                | 标签推送后删除 | 写入 `docs\releases\vX.Y.Z.md`     |
| 大功能迭代              | 默认发版    | 发版准备完成后合并                                | 标签推送后删除 | 写入 `docs\releases\vX.Y.Z.md`     |
| Keacs Agent 助手开发任务 | 不发版     | 在分支Keacs-agent分支中完成全部开发任务，全部验收通过，等用户要求合并 | 需用户要求删除 | 每个阶段完成都需要更新`docs\agents-plan.md` |

不发版路径：自测通过 -> 业务验收通过 -> 更新必要文档和 `docs\releases\next.md` -> 提交 -> 合并到 `master` -> 删除短分支。

发版路径：自测通过 -> 安装应用并业务验收通过 -> 汇总 `docs\releases\next.md` 和本次变化 -> 更新版本号和正式版本说明 -> 提交 -> 合并到 `master` -> 打标签并推送 -> 删除短分支。

## 开发边界

- 只做当前需求，禁止为未来假设提前设计。
- 优先选择简单直接的方案，禁止把小问题扩大成新架构。
- 单次使用的逻辑不要抽象成框架、配置层或通用工具。
- 只修改与当前任务直接相关的文件，不顺手重构、不改无关格式。
- 保证当前改动后代码库干净、组织清晰，不留下临时文件、死代码、死文件或不必要的文件夹、子文件夹、文件。
- 发现无关问题可以简短提示，但不要借机扩改。
- 前端必须遵守现有组件风格；大功能迭代中的前端开发必须使用 `.agents\skills\impeccable`。

## 代码规范

- 保持 UI 样式、组件、交互和体验一致，禁止擅自扩展能力。
- 代码必须简单、清晰、可维护，先保证正确性，再考虑性能。
- 命名准确清晰，职责单一，边界明确，结构统一，并遵循现有风格。
- 控制复杂度，避免深层嵌套、重复逻辑和重复实现。
- 只清理自己改动引入的无用代码。
- 禁止硬编码；已有配置必须继续统一管理。
- 保持向后兼容，覆盖边界条件、异常输入和失败场景。
- 禁止静默失败、吞异常或暴露敏感信息。
- 代码注释使用中文，但不要写冗余注释。
- 单文件尽量控制在 300 行以内；超过400行必须评估是否拆分；超过600行一般视为需要重构。

## 测试要求

每轮任务按影响面自测，只测与当前改动直接相关的内容。

| 变更类型               | 必测项                                           | 可省略项                  |
| ------------------ | --------------------------------------------- | --------------------- |
| 非代码开发任务            | 内容准确性、链接有效性、是否影响现有规范                          | 代码测试                  |
| 小功能优化              | Lint、类型检查、受影响模块测试或最小可验证链路、1 条主路径              | 全量集成、UI、E2E、性能、安全测试   |
| BUG 修复             | Lint、类型检查、问题复现、修复后回归、相关单元测试或集成测试              | 与缺陷无关的 UI、E2E、性能、安全测试 |
| 大功能迭代              | Lint、类型检查、单元测试、集成测试、UI 测试、E2E 测试              | 无                     |
| Keacs Agent 助手开发任务 | 根据阶段开发内容，按需执行Lint、类型检查、单元测试、集成测试、UI 测试、E2E 测试 | <br />                |

按需补测：

- 改动影响输入、校验、计算、状态判断时，补测空值、最大值、最小值、非法输入和临界状态。
- 改动涉及网络、权限、存储、保存、超时、重试时，补测对应异常场景。
- 改动涉及页面切换、返回、重复进入、重复提交、多步骤流程时，补测状态流转。
- 改动影响布局、交互、系统能力、设备适配、字体展示时，补测兼容性。
- 有失败项时，先修复，再重新完成受影响范围内的自测。
- 只有涉及 UI 改动、布局适配、交互状态，或任务明确要求 UI 测试时，才执行 UI 自检。自检时确认不存在文案错误、样式不统一、间距失衡、按钮无反馈、组件状态异常、缺少必要状态提示、内容遮挡溢出、视觉噪音明显，且整体与已有组件样式一致。

## 文档、分支和版本记录

- 分支：非代码任务不拉取临时分支，每个任务从 `master` 建短分支，功能用 `feature/`，修复用 `fix/`；不使用长期 `develop`。
- 状态：`master` 表示已验收、可随时发版；`v*` 标签表示已正式发布。
- 待发版记录：未立即发版的用户可感知变化写入 `docs\releases\next.md`；文件不存在时先创建；正式发版后清空。
- 项目文档：`prd.md` 只写需求和产品规则变化；`arc.md` 只写技术路线、架构、存储、构建和发版机制变化；`code-map.md` 只写模块、入口或路径变化。
- 版本号：只在发布 APK 时更新 `app\build.gradle.kts`；`versionName` 为 `X.Y.Z`，标签必须一致；`versionCode` 每次发版加 1。
- 版本递增：修复、小优化、应用内文案改 `PATCH`；新页面、新入口、新能力改 `MINOR`；数据不兼容或重大规则变化改 `MAJOR`。
- 自动发版：在 `master` 推送 `vX.Y.Z` 标签后，由 `.github\workflows\release.yml` 构建和发布；APK 命名为 `keacs-v*.apk`，APK 不入库。

```powershell
git tag vX.Y.Z
git push origin vX.Y.Z
git push gitee vX.Y.Z
```

## 提交要求

- 提交说明使用中文，清楚说明本次变更。
- 提交前必须查看 `git status --short`，只提交与当前任务直接相关的文件。

## 完成标准

- 假设已明确说明，或已通过上下文和代码验证。
- 改动范围足够小，没有无关修改。
- 验收目标明确，且已完成对应检查。
- 测试结果只报告通过或失败数量，不逐条列出用例。
- 如果有检查无法执行，必须说明原因、影响和风险。

## 禁止事项

- 禁止未确认需求、假设或分歧就直接写代码。
- 禁止把小修复扩大成新架构、通用能力或大范围重构。
- 禁止修改相邻代码来“顺手优化”。
- 禁止没有完成验证就给出“应该可以”的结论。
- 禁止隐藏不确定性或未验证假设。

## 常用命令

### 腾讯云服务器

- PowerShell 读取中文前先执行：`chcp 65001 > $null; [Console]::OutputEncoding = [System.Text.Encoding]::UTF8`
- 连接腾讯云服务器：`ssh keacs-prod`
- 查看服务器身份信息：`ssh keacs-prod "whoami && hostname && pwd"`
- 查看 CPU、内存、磁盘：`ssh keacs-prod "top -bn1 | head -20 && free -h && df -h"`
- 查看 Docker 容器：`ssh keacs-prod "docker ps -a"`
- 查看 Docker 日志：`ssh keacs-prod "docker logs --tail 200 容器名"`
- 重启 Docker 容器：`ssh keacs-prod "docker restart 容器名"`
- 查看 systemd 服务状态：`ssh keacs-prod "systemctl status 服务名 --no-pager"`
- 重启 systemd 服务：`ssh keacs-prod "sudo systemctl restart 服务名"`
- 查看最近 200 行服务日志：`ssh keacs-prod "journalctl -u 服务名 -n 200 --no-pager"`
- 实时查看服务日志：`ssh keacs-prod "journalctl -u 服务名 -f"`
- 查看 Nginx 状态：`ssh keacs-prod "sudo systemctl status nginx --no-pager"`
- 测试 Nginx 配置并重载：`ssh keacs-prod "sudo nginx -t && sudo systemctl reload nginx"`
- 上传文件到服务器：`scp 本地文件 keacs-prod:/目标目录/`
- 从服务器下载文件：`scp keacs-prod:/远程文件 本地目录`

### Android 本地开发

- 完整自测：`.\gradlew.bat :app:compileDebugKotlin :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest`
- 查看设备：`adb devices`
- 设置 Android SDK 路径：`$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME`
- 查看模拟器：`$sdk=if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }; & "$sdk\emulator\emulator.exe" -list-avds`
- 启动模拟器：`$sdk=if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }; Start-Process -FilePath "$sdk\emulator\emulator.exe" -ArgumentList @('-avd','Pixel6_API34','-no-snapshot','-no-audio','-no-boot-anim') -WindowStyle Hidden`
- 等待模拟器开机：`$serial='emulator-5554'; do { Start-Sleep -Seconds 5; $state=adb -s $serial get-state 2>$null; $boot=(@(adb -s $serial shell getprop sys.boot_completed 2>$null) -join '').Trim(); "state=$state boot=$boot" } until ($state -eq 'device' -and $boot -eq '1')`
- 安装应用：`$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME; .\gradlew.bat :app:installDebug`
- 启动应用：`adb shell am start -n com.keacs.app/.MainActivity`
- 检查联网权限：`Select-String -Path "app\src\main\AndroidManifest.xml" -Pattern "INTERNET|ACCESS_NETWORK_STATE|uses-permission" -SimpleMatch`
- 提交前查看：`git status --short`

