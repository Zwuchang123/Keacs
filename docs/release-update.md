# 版本更新发布方案

## 目标

- 应用内只提供“获取更新”入口，不在本机检查版本。
- 点击后打开外部发布页，用户自己查看更新内容并下载安装包。
- 不给应用增加联网权限，保持核心功能完全离线。

## 推荐方案

采用“双发布入口”：

1. 国内主入口：Gitee Release。
2. 备用入口：GitHub Releases。

原因：GitHub 更适合作为代码和海外下载入口，但中国大陆用户访问可能不稳定；Gitee Release 支持上传安装包附件，更适合作为国内用户的下载入口。

## 当前应用入口

应用内“关于 -> 获取更新”读取 `BuildConfig.UPDATE_URL`。

当前默认地址：

```text
https://gitee.com/zwuc/keacs/releases
```

如果暂时还没有 Gitee 仓库，可临时改为：

```text
https://github.com/Zwuchang123/Keacs/releases
```

说明：当前项目实际 Gitee 远程仓库为 `https://gitee.com/zwuc/keacs.git`，应用内更新地址必须与之保持一致，否则“获取更新”会打开错误页面。

## 每次发布需要做什么

1. 配置发布签名后，执行 `.\gradlew.bat :app:packageReleaseForPublish` 构建正式 APK。
2. 在 GitHub Releases 创建版本，例如 `v1.0.1`。
3. 上传 APK，并在发布说明里写清楚更新内容。
4. 在 Gitee Release 创建同版本，上传同一个 APK。
5. 在两个发布说明里互相放备用地址。
6. 保留 APK 文件名中的版本号，例如 `keacs-v1.0.1.apk`。
7. 务必确保用户更新不会导致设备数据丢失。

## 自动发布链路

仓库已补充 GitHub Actions 工作流：`.github/workflows/release.yml`

触发方式：

1. 本地确认版本号已更新。
2. 创建并推送版本标签，例如 `v1.0.1`。
3. GitHub Actions 自动构建已签名 release APK。
4. 自动在 GitHub Releases 创建同名版本并上传 `keacs-v1.0.1.apk`。
5. 如果已配置 `GITEE_TOKEN`，则自动在 Gitee Releases 创建同名版本并上传同一个 APK。

需要配置的 GitHub Secrets：

- `KEACS_RELEASE_STORE_BASE64`：发布证书 `.jks` 文件的 Base64 内容。
- `KEACS_RELEASE_STORE_PASSWORD`：发布证书密码。
- `KEACS_RELEASE_KEY_ALIAS`：发布证书别名。
- `KEACS_RELEASE_KEY_PASSWORD`：发布证书别名密码。
- `GITEE_TOKEN`：Gitee 个人访问令牌，用于创建 Gitee Release 并上传 APK。

本地构建正式 APK 前，也需要先配置同名环境变量：

```powershell
$env:KEACS_RELEASE_STORE_FILE="D:\keys\keacs-release.jks"
$env:KEACS_RELEASE_STORE_PASSWORD="证书密码"
$env:KEACS_RELEASE_KEY_ALIAS="证书别名"
$env:KEACS_RELEASE_KEY_PASSWORD="别名密码"
.\gradlew.bat :app:packageReleaseForPublish
```

未配置发布签名时，`packageReleaseForPublish` 会直接失败，避免把未签名 APK 当作正式包发布。

建议的发布命令：

```bash
.\gradlew.bat :app:packageReleaseForPublish
git tag v1.0.1
git push origin v1.0.1
```

如果也需要把标签同步到 Gitee，可再执行：

```bash
git push gitee v1.0.1
```

注意：

- GitHub Actions 只会在 GitHub 侧执行，所以必须至少把版本标签推送到 `origin`。
- Gitee Release 的自动上传依赖 GitHub Actions 里的 `GITEE_TOKEN`，未配置时只会跳过 Gitee 发布，不影响 GitHub 发布。
- 严禁上传未签名 APK，用户设备无法按正式安装包正常使用。

## 更新内容模板

```text
版本：v1.0.1
发布日期：2026-04-28

更新内容：
- 修复……
- 优化……

安装说明：
- 下载 APK 后按系统提示安装。
- 更新前建议先在“我的 -> 导出备份”保存一份备份文件。

备用下载：
- GitHub：https://github.com/Zwuchang123/Keacs/releases
- Gitee：https://gitee.com/zwuc/keacs/releases
```

## 注意

不要把 APK 提交进 Git 仓库正文，只放在 Release 附件里。
