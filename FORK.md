# WhiteFox Minecraft Launcher（非官方分支）

本仓库是 [HMCL](https://github.com/HMCL-dev/HMCL) 的非官方 fork，用于在 2026-10-20 中国大陆地区第三方启动器集体关停下载功能之后，保留一份可自主维护的分支。

- 仓库：`https://github.com/Skywindfox/WhiteFoxMinecraftLauncher`（默认分支 `Master`）
- **本分支不与上游通报、不向上游提交 PR、不向上游反馈 Issue、不改动上游仓库。**

## 上游基线

- 基线提交：`583e004d7 在中国大陆地区禁用游戏下载功能 (#7017)`
- 该提交之后上游还有若干提交（含 `8827fd06d 优化地区检测逻辑 (#7020)`），未合入本分支。

## 相对上游的改动

| 项目 | 状态 |
| --- | --- |
| 中国大陆下载限制 | **已移除**，下载功能恢复 |
| 区域判定 | 只保留本机时区/区域判定，删掉了 `ipwho.is` 网络探测 |
| Terracotta 多人联机 | **已移除**（含依赖、资源、i18n、许可证声明） |
| 启动器自动更新 | **已彻底关闭** |
| 启动验证锁 | **已启用**（见下） |
| 服务端点 / 凭据 | 可通过 `config/fork.properties` 换成本分支自己的 |
| 名称与标识 | 已改为 WhiteFox，Windows AppUserModelID 独立 |

## 启动验证锁

首次启动会弹窗要求输入验证码；输入正确后写入 `<配置目录>/access.lock`，之后启动不再询问（"单次即可"）。校验失败或取消会直接退出，不加载任何界面。

- 验证码常量：`HMCL/src/main/java/org/jackhuang/hmcl/fork/ForkAccessLock.java` 中的 `ACCESS_CODE`（当前值 `WF-7Q2M-9K3P`，可按需修改）。
- 轮换验证码：改这个常量即可。marker 文件里存的是验证码的 SHA-256，换了常量后所有已解锁的安装都会重新要求输入。
- 清除解锁状态：删除 `access.lock`。

**这是"软锁"，不是安全边界**：本仓库源码公开，任何人都能重新构建并把校验删掉；它只能挡住"直接转发成品"的扩散方式，不要指望它防住有意绕过的人。另外按 GPL-3.0，分发二进制时仍必须提供源码，锁不改变这一点。

## 更新流程

所有改动**不直接进 `Master`**，一律走：

```
分支 → 提交 → 推送分支 → 向 Master 开 PR → 维护者审批合并
```

## 服务端点与凭据（换成本分支自己的）

所有可替换的外部服务集中放在 `config/fork.properties`，构建时写入 jar 内的 `assets/hmcl.properties`，运行时由 `Metadata` / `DownloadProviders` / `Constants` / `CleanroomRemoteVersion` / `DiscoFetchJavaListTask` 读取。

规则：**留空 = 沿用上游默认值**；每项都可用同名大写环境变量覆盖（CI Secret 优先于文件）。

| 键 | 环境变量 | 作用 | 留空后果 |
| --- | --- | --- | --- |
| `website` | `FORK_WEBSITE` | 官网/仓库链接，关于页与错误提示 | 指向本仓库 |
| `docs` | `FORK_DOCS` | 文档站，派生 `CHANGELOG/CONTACT/EULA` | 指向 `docs.hmcl.net` |
| `manual_update_url` | `FORK_MANUAL_UPDATE_URL` | 手动更新入口 | 指向本仓库 Releases |
| `update_url` | `FORK_UPDATE_URL` | 自动更新源（完整 URL） | `website` + `/api/update_link` |
| `microsoft_auth_id` | `MICROSOFT_AUTH_ID` | 微软 OAuth 应用 ID | **正版登录不可用** |
| `curseforge_apikey` | `CURSEFORGE_API_KEY` | CurseForge API Key | 自动隐藏 CurseForge 源 |
| `bmclapi_root` | `FORK_BMCLAPI_ROOT` | 大陆游戏文件镜像根 | `bmclapi2.bangbang93.com` |
| `metadata_mirror` | `FORK_METADATA_MIRROR` | 自有元数据镜像（Cleanroom/FML 库） | `hmcl.glavo.site` |
| `addon_mirror` | `FORK_ADDON_MIRROR` | 模组站镜像（Modrinth/CurseForge/ForgeCDN） | `mod.mcimirror.top` |
| `discoapi_root` | `FORK_DISCOAPI_ROOT` | foojay Disco API（下载 Java） | `api.foojay.io` |

**尚未抽成配置的第三方依赖**（可选加速源，留待决定）：

- `alist.8mi.tech`、`zkitefly.github.io`：元数据/未列出游戏版本的中国大陆镜像；
- `mirrors.cloud.tencent.com`：Maven 公共镜像；
- `littleskin.cn`：离线皮肤服务。

## 待办（维护者）

1. **微软 OAuth 应用**：当前阻塞。Linux 上登录 Azure 门户报"所选用户帐户在租户 Microsoft Services 中不存在"（应用 ID `c44b4083-...`）。这是个人 Microsoft 账号的租户归属问题，建议改用 Entra 管理中心（`entra.microsoft.com`）或新建一个目录后再注册应用。应用需为公共客户端、重定向 URI `http://localhost`、启用设备代码流；拿到 `Application (client) ID` 后填入 `microsoft_auth_id` 或 Secret `MICROSOFT_AUTH_ID`。
2. **CurseForge API Key**：审批中。拿到后填 `curseforge_apikey` 或 Secret `CURSEFORGE_API_KEY`。

## 构建

```shell
./gradlew clean makeExecutables
```

产物在 `HMCL/build/libs`。

- 版本号由 `HMCL/build.gradle.kts` 决定：非官方仓库/CI 上 `versionType` 为 `unofficial`，版本形如 `3.17.unofficial-<short-sha>`，未提供 `GITHUB_SHA` 时为 `3.17.SNAPSHOT`。
- JDK 17 及以上；本机验证使用 JDK 25 + Gradle 9.8.0。
- 若 `~/.gradle` 不可写（沙箱、只读 HOME 等），用 `GRADLE_USER_HOME` 指向可写目录后重试。
- CI（`.github/workflows/gradle.yml`）在 fork 仓库上可直接用，凭据通过仓库 Secret 注入。

## 许可证

本项目遵循 GPL-3.0。分发二进制时必须同时提供对应源码，保留原始版权声明，并标注本分支的修改。Terracotta（AGPL-3.0）已被移除，`assets/about/deps.json` 中对应条目同步删除。
