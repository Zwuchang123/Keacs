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
https://gitee.com/Zwuchang123/Keacs/releases
```

如果暂时还没有 Gitee 仓库，可临时改为：

```text
https://github.com/Zwuchang123/Keacs/releases
```

## 每次发布需要做什么

1. 构建正式 APK。
2. 在 GitHub Releases 创建版本，例如 `v1.0.1`。
3. 上传 APK，并在发布说明里写清楚更新内容。
4. 在 Gitee Release 创建同版本，上传同一个 APK。
5. 在两个发布说明里互相放备用地址。
6. 保留 APK 文件名中的版本号，例如 `keacs-v1.0.1.apk`。
7. 务必确保用户更新不会导致设备数据丢失。

## 更新内容模板

```text
版本：v1.0.1
发布日期：2026-04-27

更新内容：
- 修复……
- 优化……

安装说明：
- 下载 APK 后按系统提示安装。
- 更新前建议先在“我的 -> 导出备份”保存一份备份文件。

备用下载：
- GitHub：https://github.com/Zwuchang123/Keacs/releases
- Gitee：https://gitee.com/Zwuchang123/Keacs/releases
```

## 注意

不要把 APK 提交进 Git 仓库正文，只放在 Release 附件里。
