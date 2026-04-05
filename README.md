# 🌟✨ ChatBridge - 跨服聊天连接器 ✨🌟

<div align="center">

# 💫 让你的多个服务器连接起来，畅聊无阻！ 💫

[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](https://github.com/httye/ChatBridge)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Paper](https://img.shields.io/badge/Paper-1.20.4+-orange.svg)](https://papermc.io)

**🎊 一个超可爱的跨服聊天插件！🎊**
**💝 让玩家感觉服务器是同一个大家庭！💝**

</div>

---

## ✨🌸 核心功能 🌸✨

### 🎮💬 跨服务器聊天同步
- 📤 实时同步多个服务器之间的聊天消息 💬
- 🌈 支持表情、颜色代码等丰富格式 🎨
- 🚀 低延迟、高性能的消息传递 ⚡
- 💫 让聊天更加有趣和生动！

### 👋🎉 玩家加入/退出通知
- 🎊 跨服务器显示玩家加入消息 🎉
- 👋 跨服务器显示玩家离开消息 👋
- 💝 让玩家感觉服务器是同一个大家庭 🏠
- 🌟 永远不会错过好友的动态！

### 🔔📢 服务器状态通知
- 📢 服务器上线时自动通知所有玩家 🎊
- 🔕 服务器关闭时自动通知所有玩家 😴
- 🌈 实时了解服务器状态 📊
- 💤 知道什么时候可以一起玩啦！

### 🏷️🎨 自定义服务器前缀
- 🎨 支持配置服务器前缀格式和颜色 🌈
- 🔤 选择前缀显示位置（玩家名前或消息前）
- ✨ 让每个服务器都有独特的标识 🎭
- 💎 个性化你的服务器风格！

### 👑🎭 权限插件支持
- 🎭 自动读取 LuckPerms 玩家前缀后缀 👑
- 🛡️ 自动读取 Vault 玩家前缀后缀 🛡️
- 📛 完美支持现有的权限系统 📚
- ✨ 让每个玩家都有独特的标识！

### 🚫🌿 敏感词过滤
- 🔇 内置敏感词过滤功能 🚫
- 🌿 自动过滤不当言论 🍃
- 🛡️ 维护聊天环境健康 ❤️
- 😊 让社区更加友好！

### 🔇🎚️ 玩家聊天开关
- 🎚️ 玩家可自行开关全局聊天显示 🎛️
- 🔕 想安静时可以静音 🤫
- 🎨 灵活控制聊天体验 🎯
- 💝 给玩家更多选择权！

### 🌈🎨 颜色代码支持
- 🌈 完整支持 Minecraft 颜色代码 🎨
- ✨ 支持 `&` 符号和十六进制颜色 💎
- 💖 让消息更加丰富多彩 ✨
- 🎨 打造独特的聊天风格！

---

## 📋💻 系统要求 💻📋

| 组件 | 版本要求 |
|------|----------|
| Minecraft 服务器 | 🎮 Paper 1.20.4+ |
| Java | ☕ 17+ |
| Redis | 🔴 7.0+ |

---

## 🚀🎉 快速开始 🎉🚀

### 📦📥 安装步骤

1. 🔽 下载最新版本的 ChatBridge JAR 文件 📥
2. 📂 将 JAR 文件放入服务器的 `plugins` 目录 📁
3. ⚡ 启动服务器，插件会自动生成配置文件 🚀
4. ⚙️ 编辑 `plugins/ChatBridge/config.yml` 配置你的服务器 🔧
5. ✅ 使用 `/chatbridge reload` 重载配置 🔄
6. 🎉 享受跨服聊天体验！🎊

### 🎯🌟 首次启动

插件启动时会显示欢迎信息：

```
========================================
   ____ _               _     ____ _           _   
  / ___| |__   ___ _ __| | __/ ___| |__   __ _| |_ 
 | |   | '_ \ / _ \ '__| |/ / |   | '_ \ / _` | __|
 | |___| | | |  __/ |  |   <| |___| | | | (_| | |_ 
  \____|_| |_|\___|_|  |_|\_\____|_| |_|\__,_|\__|
========================================
  版本: 1.0.2
  作者: httye
  描述: 多服务器聊天互通插件
========================================
  Copyright 2026 httye
  GitHub: https://github.com/httye/ChatBridge
========================================
```

连接成功后，所有在线玩家会收到提示：
> `🌟✅ 已成功连接到中转服务器，跨服聊天已启用！✨`

---

## ⚙️🔧 配置说明 🔧⚙️

### 📝📋 完整配置示例

```yaml
# 服务器配置 🏠
server:
  name: "Server1"         # 服务器唯一标识 🔑
  display-name: "&a&l生存服"  # 服务器显示名称（支持颜色代码）🎨
  prefix:
    enabled: true         # 是否显示服务器前缀 ✅
    format: "&7[&b{server}&7]"  # 服务器前缀格式 🏷️
    position: "before_name"     # 前缀位置 📍

# 聊天配置 💬
chat:
  enabled: true           # 是否启用聊天同步 ✅
  sync-server-status: true # 是否同步服务器状态 📢

# 敏感词过滤已内置并启用 🚫
```

### 🏷️🎨 服务器前缀配置

**配置项说明：**

| 配置项 | 说明 | 说明 |
|--------|------|------|
| `enabled` | 是否启用服务器前缀 | ✅/❌ |
| `format` | 前缀格式，`{server}` 会替换为服务器显示名称 | 🏷️ |
| `position` | 前缀位置 | 📍 |

**位置效果：**

| 位置选项 | 效果 |
|----------|------|
| `before_name` | 🏷️ `[生存服] 玩家名: 消息内容` |
| `before_message` | 🎨 `玩家名: [生存服] 消息内容` |

**配置示例：**

```yaml
server:
  display-name: "&a&l生存服"
  prefix:
    enabled: true
    format: "&7[&b{server}&7]"
    position: "before_name"
```

**消息格式：**
- 💬 聊天：`🌟[生存服] 玩家名: 消息内容`
- 👋 加入：`🎉[生存服] 玩家 加入了游戏`
- 👋 退出：`👋[生存服] 玩家 离开了游戏`
- 📢 启动：`🚀[生存服] 服务器已启动`
- 🔕 关闭：`😴[生存服] 服务器已关闭`

---

## 💻🎮 命令列表 🎮💻

| 命令 | 说明 | 权限 |
|------|------|------|
| `/chatbridge reload` | 🔄 重载配置文件 | OP 👑 |
| `/chatbridge status` | 📊 查看插件状态 | OP 👑 |
| `/globalchat <消息>` | 💬 发送全局消息 | 所有人 👥 |
| `/togglechat` | 🔇 切换聊天显示 | 所有人 👥 |

**命令别名：**
- `/chatbridge` → `/cb` 🎯
- `/globalchat` → `/gc` 🌍
- `/togglechat` → `/tc` 🎚️

**权限列表：**

| 权限 | 说明 | 默认 |
|------|------|------|
| chatbridge.admin | 👑 管理员权限 | OP 👑 |
| chatbridge.global | 🌍 全局聊天权限 | 所有人 👥 |
| chatbridge.toggle | 🎚️ 切换聊天显示 | 所有人 👥 |

---

## 🌐🏰 多服务器部署 🏰🌐

### 🎯📋 部署步骤

1. 📥 在每个服务器上安装此插件 📥
2. 🔑 为每个服务器配置唯一的 `server.name` 🔑
3. 🎨 配置各服务器的 `server.display-name` 🎨
4. 🔗 确保所有服务器连接到同一个 Redis 实例 🔗

### 📌🏷️ 配置示例

**服务器 1 - 生存服 🌳🏡**
```yaml
server:
  name: "survival"
  display-name: "&a&l🌳生存服"
```

**服务器 2 - 创造服 🎨🎭**
```yaml
server:
  name: "creative"
  display-name: "&b&l🎨创造服"
```

**服务器 3 - 小游戏服 🎮🎲**
```yaml
server:
  name: "minigames"
  display-name: "&e&l🎮小游戏服"
```

---

## 🔧🔨 编译方法 🔨🔧

### 🤖🔄 GitHub Actions 自动编译

1. 🔄 Fork 或将项目上传到 GitHub 仓库 🔄
2. 📤 推送代码到 `main` 或 `master` 分支 📤
3. 📥 在 **Actions** → **Artifacts** 中下载编译好的 JAR 文件 📥

**发布版本：**
```bash
git tag v1.0.2
git push origin v1.0.2
```

### 💻🔨 本地编译

```bash
cd chat-bridge
mvn clean package
```

编译后的 JAR 文件位于 `target/chat-bridge-1.0.2.jar` 📦

---

## ❓💡 常见问题 💡❓

### 🤔🤷‍♂️ 消息没有同步到其他服务器？

请检查：
1. ✅ Redis 服务是否正常运行 🔴
2. 🔗 所有服务器是否连接到同一个 Redis 实例 🔗
3. 📊 使用 `/chatbridge status` 检查连接状态 📊
4. 🔑 确认服务器密钥是否正确配置 🔑

### 🎭👑 玩家前缀/后缀不显示？

请确保已安装 LuckPerms 或 Vault 插件，并正确配置了权限组的前缀/后缀 👑

### 🚫🔇 如何禁用某些消息同步？

在 `config.yml` 中设置相应选项为 `false`：
- `chat.enabled` - 禁用聊天同步 💬
- `chat.sync-server-status` - 禁用服务器状态同步 📢

### 🏷️🎨 如何自定义服务器前缀显示？

在 `server.prefix` 配置中：
- 设置 `enabled: true/false` 控制是否显示 ✅
- 设置 `format` 自定义前缀格式，使用 `{server}` 代表服务器名称 🏷️
- 设置 `position` 控制前缀位置 📍

---

## 📦📚 依赖插件（可选）📚📦

| 插件 | 用途 |
|------|------|
| [LuckPerms](https://luckperms.net/) | 👑 提供玩家前缀/后缀 👑 |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | 🛡️ 提供玩家前缀/后缀（备选）🛡️ |

---

## 📄🎉 项目信息 🎉📄

- **版本:** 1.0.2 🎊
- **作者:** httye 👤
- **API:** Paper API 1.20.4 📚
- **许可证:** MIT License 📜

---

## 📜💝 版权声明 💝📜

```
Copyright 2026 httye

本插件为开源软件，遵循 MIT 许可证
GitHub: https://github.com/httye/ChatBridge
```

---

<div align="center">

# 💖 如果这个项目对你有帮助，请给一个 ⭐ Star 支持一下！ 💖

## 🎊 感谢使用 ChatBridge！祝你玩得开心！🎊

### 💕 有问题随时来提问哦！💕

</div>