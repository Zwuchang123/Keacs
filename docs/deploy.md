# 部署与发版

本文档统一管理 Keacs 的交付、发版、版本记录、APK 发布和官方 Agent 后端部署流程。真实 API Key、签名文件和服务器密码只放在 GitHub Secrets 或服务器环境变量中，不能写入仓库。

## 1. 交付与发版决策

核心规则：验收失败不合并、不发版；验收通过后合并到 `master` 并删除短分支；是否发版只看是否要发布 APK。

| 场景               | 发版      | 合并 `master` | 删除短分支   | 必做记录                         |
| ---------------- | ------- | ----------- | ------- | ---------------------------- |
| 非代码开发任务          | 不发版     | 验收通过后合并     | 合并后删除   | 按需更新相关文档                     |
| 小功能优化，暂不上线       | 不发版     | 验收通过后合并     | 合并后删除   | 写入 `docs\releases\next.md`   |
| BUG 修复，可等待发版     | 不发版     | 验收通过后合并     | 合并后删除   | 写入 `docs\releases\next.md`   |
| BUG 修复，需要立刻给用户更新 | 发补丁版本   | 发版准备完成后合并   | 标签推送后删除 | 写入 `docs\releases\vX.Y.Z.md` |
| 小功能优化，确认上线       | 发补丁或小版本 | 发版准备完成后合并   | 标签推送后删除 | 写入 `docs\releases\vX.Y.Z.md` |
| 大功能迭代            | 默认发版    | 发版准备完成后合并   | 标签推送后删除 | 写入 `docs\releases\vX.Y.Z.md` |

## 2. 分支、版本和记录

- 分支：非代码任务不拉取临时分支，每个任务从 `master` 建短分支，功能用 `feature/`，修复用 `fix/`；不使用长期 `develop`。
- 状态：`master` 表示已验收、可随时发版；`v*` 标签表示已正式发布。
- 待发版记录：未立即发版的用户可感知变化写入 `docs\releases\next.md`；文件不存在时先创建；正式发版后清空。
- 版本号：只在发布 APK 时更新 `app\build.gradle.kts`；`versionName` 为 `X.Y.Z`，标签必须一致；`versionCode` 每次发版加 1。
- 版本递增：修复、小优化、应用内文案改 `PATCH`；新页面、新入口、新能力改 `MINOR`；数据不兼容或重大规则变化改 `MAJOR`。

## 3. 不发版流程

1. 在短分支完成开发和自测。
2. 用户验收通过后，更新必要文档和 `docs\releases\next.md`。
3. 提交本次变更，合并到 `master`。
4. 删除短分支。

## 4. APK 发版流程

1. 确认本次发布内容和版本号。
2. 更新 `app\build.gradle.kts` 中的 `versionCode` 和 `versionName`。
3. 汇总 `docs\releases\next.md` 和本次变化，写入 `docs\releases\vX.Y.Z.md`。
4. 清空 `docs\releases\next.md`，只保留待发版标题。
5. 完成自测、安装验证和业务验收。
6. 提交发版准备内容，合并到 `master`。
7. 在 `master` 创建并推送标签：

```powershell
git tag vX.Y.Z
git push origin vX.Y.Z
git push gitee vX.Y.Z
```

标签推送后，`.github\workflows\release.yml` 会自动校验版本号、构建签名 APK，并发布 `keacs-vX.Y.Z.apk`。如果标签和 `versionName` 不一致，流水线必须失败，不能继续发版。

GitHub Release 需要以下 Secrets：

- `KEACS_RELEASE_STORE_BASE64`
- `KEACS_RELEASE_STORE_PASSWORD`
- `KEACS_RELEASE_KEY_ALIAS`
- `KEACS_RELEASE_KEY_PASSWORD`
- `GITEE_TOKEN`：可选，用于同步创建 Gitee Release。

## 5. 后端部署目标

官方 Agent 后端部署在腾讯云轻量应用服务器。

服务器固定约定：

- 连接别名：`keacs-prod`。
- 部署目录：`/opt/keacs`。
- 后端运行目录：`/opt/keacs/server`。
- 容器入口：`server/docker-compose.yml`。
- HTTPS 入口：`server/Caddyfile`。
- 健康检查：`https://<KEACS_AGENT_DOMAIN>/health`。

服务器只对外开放 `80` 和 `443`，FastAPI 的 `8000` 端口只在 Docker 内部暴露。域名必须先解析到服务器公网 IP，Caddy 才能自动申请 HTTPS 证书。

当前过渡阶段，App 默认官方助手地址暂时使用 `http://43.138.174.171`。正式域名配置完成后，再切换为 HTTPS 域名。

## 6. 首次部署后端

首次部署前确认服务器已安装 Docker 和 Docker Compose 插件。

1. 创建部署目录：

```powershell
ssh keacs-prod 'sudo mkdir -p /opt/keacs && sudo chown $USER:$USER /opt/keacs'
```

1. 在服务器拉取仓库代码：

```powershell
ssh keacs-prod "cd /opt/keacs && git clone <仓库地址> ."
```

如果服务器不能直接访问仓库，可以只上传 `server` 目录，但仍以 `/opt/keacs/server` 作为运行目录。

1. 在服务器创建 `/opt/keacs/server/.env`，字段参考 `server/.env.example`。必须配置：

```text
KEACS_AGENT_DOMAIN=<正式域名>
KEACS_MODEL_PROVIDER=openai_compatible
KEACS_MODEL_BASE_URL=https://api.minimax.io/v1
KEACS_MODEL_NAME=MiniMax-M2.5-highspeed
KEACS_MODEL_REASONING_SPLIT=true
KEACS_MODEL_API_KEY=<服务器上的真实模型 API Key>
```

1. 启动服务：

```powershell
ssh keacs-prod "cd /opt/keacs/server && docker compose up -d --build"
```

1. 验证容器和健康检查：

```powershell
ssh keacs-prod "cd /opt/keacs/server && docker compose ps"
ssh keacs-prod "curl -fsS https://<正式域名>/health"
```

健康检查返回 `{"status":"ok"}` 后，才允许把 App 的官方服务地址指向该域名。

## 7. 后端更新部署

后端更新和 App 发版可以分开执行。只要接口协议向后兼容，可以先部署后端，再发布 App。

标准更新流程：

1. 本地完成后端相关自测。
2. 合并到 `master` 或确认要部署的标签。
3. 服务器拉取目标版本并重建容器：

```powershell
ssh keacs-prod "cd /opt/keacs && git fetch --all --tags && git checkout <分支或标签> && cd server && docker compose up -d --build"
```

1. 检查运行状态和最近日志：

```powershell
ssh keacs-prod "cd /opt/keacs/server && docker compose ps"
ssh keacs-prod "cd /opt/keacs/server && docker compose logs --tail 200"
```

1. 再次访问健康检查，并用 App 官方服务主路径做一次连接验证。

## 8. 后端回滚

如果更新后健康检查失败，或 App 主路径无法连接，先回滚后端，不发布新的 APK。

回滚流程：

1. 切回上一个可用标签或提交：

```powershell
ssh keacs-prod "cd /opt/keacs && git checkout <上一个可用标签或提交> && cd server && docker compose up -d --build"
```

1. 检查容器和健康检查。
2. 如果是环境变量错误，只修改服务器 `/opt/keacs/server/.env` 后重启：

```powershell
ssh keacs-prod "cd /opt/keacs/server && docker compose up -d"
```

## 9. 部署检查清单

每次后端部署完成后，至少确认：

- `docker compose ps` 中 `keacs-agent` 和 `caddy` 正常运行。
- `/health` 返回正常。
- 后端日志没有 API Key、完整用户输入、完整模型回答和完整账本内容。
- 官方模型 API Key 只存在于服务器 `.env` 或服务器环境变量中。
- App 官方服务地址优先使用 HTTPS 正式域名；过渡阶段允许临时使用 `http://43.138.174.171`。

## 10. 腾讯云服务器常用命令

PowerShell 读取中文前先执行：

```powershell
chcp 65001 > $null; [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

常用命令：

```powershell
ssh keacs-prod
ssh keacs-prod "whoami && hostname && pwd"
ssh keacs-prod "top -bn1 | head -20 && free -h && df -h"
ssh keacs-prod "docker ps -a"
ssh keacs-prod "docker logs --tail 200 容器名"
ssh keacs-prod "docker restart 容器名"
ssh keacs-prod "systemctl status 服务名 --no-pager"
ssh keacs-prod "sudo systemctl restart 服务名"
ssh keacs-prod "journalctl -u 服务名 -n 200 --no-pager"
ssh keacs-prod "journalctl -u 服务名 -f"
ssh keacs-prod "sudo systemctl status nginx --no-pager"
ssh keacs-prod "sudo nginx -t && sudo systemctl reload nginx"
scp 本地文件 keacs-prod:/目标目录/
scp keacs-prod:/远程文件 本地目录
```

