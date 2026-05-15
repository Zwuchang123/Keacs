# 部署与发版

除非用户有明确要求，否则默认每次发版都需要同时执行APK发版和后端更新
真实 API Key、签名文件、服务器密码只放在 GitHub Secrets 或服务器环境变量中，不能写入仓库。

## 1. 版本规则

- 版本号只在发布 APK 时更新 `app\build.gradle.kts`。
- `versionName` 使用 `X.Y.Z`，Git 标签必须为 `vX.Y.Z`，二者必须一致。
- `versionCode` 每次发版加 1。
- 修复、小优化、应用内文案改动递增 `PATCH`。
- 新页面、新入口、新能力递增 `MINOR`。
- 数据不兼容或重大规则变化递增 `MAJOR`。

## 2. APK 发版

发版前准备：

1. 确认发布范围、版本号和验收结果。
2. 更新 `app\build.gradle.kts` 的 `versionCode` 和 `versionName`。
3. 汇总 `docs\releases\next.md` 和本次变化，写入 `docs\releases\vX.Y.Z.md`。
4. 清空 `docs\releases\next.md`，只保留待发版标题。
5. 按 `docs\testing.md` 完成发版检查和安装验证。

创建并推送标签：

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
git push gitee vX.Y.Z
```

标签推送后，`.github\workflows\release.yml` 会校验版本号、构建签名 APK，并发布 `keacs-vX.Y.Z.apk`。如果标签和 `versionName` 不一致，流水线必须失败，不能继续发版。

## 3. 后端环境

官方 Agent 后端部署在腾讯云轻量应用服务器。

固定约定：

- 连接别名：`keacs-prod`
- 登录用户：`deploy`
- 服务器 IP：`43.138.174.171`
- MacBook Air 私钥：`~/.ssh/macbookair_openssh`
- 部署目录：`/opt/keacs`
- 后端目录：`/opt/keacs/server`
- 容器入口：`server/docker-compose.yml`
- HTTPS 入口：`server/Caddyfile`
- 健康检查：`https://<KEACS_AGENT_DOMAIN>/health`

服务器只对外开放 `80` 和 `443`。FastAPI 的 `8000` 端口只在 Docker 内部暴露。域名必须先解析到服务器公网 IP，Caddy 才能申请 HTTPS 证书。

过渡阶段，App 默认官方助手地址可临时使用 `http://43.138.174.171`；正式域名配置完成后切换为 HTTPS 域名。

首次配置本机 SSH：

```bash
cp ~/Downloads/macbookair.pem ~/.ssh/macbookair.pem
chmod 600 ~/.ssh/macbookair.pem
cp ~/.ssh/macbookair.pem ~/.ssh/macbookair_openssh
ssh-keygen -p -o -f ~/.ssh/macbookair_openssh -P "" -N ""
cat >> ~/.ssh/config <<'EOF'
Host keacs-prod
  HostName 43.138.174.171
  User deploy
  IdentityFile ~/.ssh/macbookair_openssh
  IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config
ssh keacs-prod "whoami && hostname && pwd"
```

## 4. 后端更新与回滚

后端更新和 APK 发版可以分开执行；接口协议向后兼容时，可以先部署后端，再发布 App。

更新流程：

1. 本地完成后端相关检查。
2. 合并到 `master`，或确认要部署的标签。
3. 拉取目标版本并重建容器。
4. 检查容器、日志、健康检查和 App 官方服务主路径。

```bash
ssh keacs-prod "cd /opt/keacs && git fetch --all --tags && git checkout <分支或标签> && git pull --ff-only && cd server && docker compose up -d --build"
ssh keacs-prod "cd /opt/keacs/server && docker compose ps"
ssh keacs-prod "cd /opt/keacs/server && docker compose logs --tail 200"
ssh keacs-prod "curl -fsS http://127.0.0.1:8000/health"
curl -fsS http://43.138.174.171/health
```

如果更新后健康检查失败，或 App 主路径无法连接，先回滚后端，不发布新的 APK。

```bash
ssh keacs-prod "cd /opt/keacs && git checkout <上一个可用标签或提交> && cd server && docker compose up -d --build"
ssh keacs-prod "cd /opt/keacs/server && docker compose ps"
ssh keacs-prod "curl -fsS http://127.0.0.1:8000/health"
curl -fsS http://43.138.174.171/health
```

如果只是环境变量错误，修改服务器 `/opt/keacs/server/.env` 后重启：

```bash
ssh keacs-prod "cd /opt/keacs/server && docker compose up -d"
```

## 5. 部署检查

每次后端部署完成后确认：

- `keacs-agent` 和 `caddy` 容器正常运行。
- `/health` 返回正常。
- 后端日志没有 API Key、完整用户输入、完整模型回答和完整账本内容。
- 官方模型 API Key 只存在于服务器 `.env` 或服务器环境变量中。
- App 官方服务地址优先使用 HTTPS 正式域名。

## 6. 常用命令

```bash
ssh keacs-prod
ssh deploy@43.138.174.171
ssh keacs-prod "whoami && hostname && pwd"
ssh keacs-prod "top -bn1 | head -20 && free -h && df -h"
ssh keacs-prod "docker ps -a"
ssh keacs-prod "cd /opt/keacs/server && docker compose ps"
ssh keacs-prod "cd /opt/keacs/server && docker compose logs --tail 200"
ssh keacs-prod "cd /opt/keacs/server && docker compose logs --tail 200 keacs-agent"
ssh keacs-prod "cd /opt/keacs/server && docker compose logs --tail 200 caddy"
ssh keacs-prod "cd /opt/keacs/server && docker compose restart keacs-agent"
ssh keacs-prod "cd /opt/keacs/server && docker compose restart caddy"
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
