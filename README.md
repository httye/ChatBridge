# ChatBridge - Minecraft多服务器聊天互通插件

一个基于Redis的Minecraft Paper服务器多服务器聊天互通插件，支持跨服务器聊天同步、玩家加入/退出消息同步等功能。

## 功能特性

- ✅ 跨服务器聊天消息同步
- ✅ 玩家加入/退出消息同步
- ✅ 服务器启动/关闭状态同步
- ✅ 支持LuckPerms/Vault前缀后缀
- ✅ 敏感词过滤
- ✅ 玩家可单独开关全局聊天显示
- ✅ 自定义消息格式
- ✅ 支持颜色代码

## 环境要求

- Minecraft服务器: Paper 1.20.4+
- Java: 17+
- Redis: 5.0+

## 安装方法

1. 确保你的服务器已安装Redis
2. 下载插件JAR文件
3. 将JAR文件放入服务器的`plugins`目录
4. 启动服务器，插件会自动生成配置文件
5. 编辑`plugins/ChatBridge/config.yml`配置Redis连接信息
6. 重启服务器或使用`/chatbridge reload`重载配置

## 配置说明

```yaml
# Redis配置
redis:
  host: localhost          # Redis服务器地址
  port: 6379              # Redis端口
  password: ""            # Redis密码（无密码留空）
  database: 0             # Redis数据库编号

# 服务器配置
server:
  name: "Server1"         # 服务器唯一标识
  display-name: "&a&l生存服"  # 服务器显示名称（支持颜色代码）
  prefix:
    enabled: true         # 是否在玩家名字前显示服务器前缀
    format: "&7[&b{server}&7]"  # 服务器前缀格式，{server}为显示名称
    position: "before_name"     # 前缀位置: before_name(玩家名前) 或 before_message(消息前)

# 聊天配置
chat:
  format: "&7[&b{server}&7] &r{prefix}{player}&r: &f{message}"
  enabled: true           # 是否启用聊天同步
  sync-join-quit: true    # 是否同步加入/退出消息
  sync-death: false       # 是否同步死亡消息
  sync-server-status: true # 是否同步服务器状态
```

### 可用变量

| 变量 | 说明 |
|------|------|
| `{server}` | 服务器显示名称 |
| `{player}` | 玩家名称 |
| `{display_name}` | 玩家显示名称 |
| `{message}` | 消息内容 |
| `{prefix}` | 玩家前缀（需要权限插件） |
| `{suffix}` | 玩家后缀（需要权限插件） |
| `{server_prefix}` | 服务器前缀（根据配置自动添加） |

### 服务器前缀配置说明

腐竹可以自定义服务器前缀的显示方式：

```yaml
server:
  prefix:
    enabled: true              # 是否启用服务器前缀
    format: "&7[&b{server}&7]" # 前缀格式，{server}会被替换为服务器显示名称
    position: "before_name"    # 前缀位置
```

**前缀位置选项：**
- `before_name` - 前缀显示在玩家名字前面，如：`[生存服] 玩家名: 消息内容`
- `before_message` - 前缀显示在消息前面，如：`玩家名: [生存服] 消息内容`

**示例效果：**

当配置为：
```yaml
server:
  display-name: "&a&l生存服"
  prefix:
    enabled: true
    format: "&7[&b{server}&7]"
    position: "before_name"
```

聊天消息显示效果：`[生存服] 玩家名: 大家好！`

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/chatbridge reload` | 重载配置文件 | chatbridge.admin |
| `/chatbridge status` | 查看插件状态 | chatbridge.admin |
| `/globalchat <消息>` | 发送全局消息 | chatbridge.global |
| `/togglechat` | 切换全局聊天显示 | chatbridge.toggle |

## 权限

| 权限 | 说明 | 默认 |
|------|------|------|
| chatbridge.admin | 管理员权限 | OP |
| chatbridge.global | 全局聊天权限 | 所有人 |
| chatbridge.toggle | 切换聊天显示权限 | 所有人 |

## 编译方法

### 方法一：GitHub Actions 自动编译（推荐）

1. Fork 或将此项目上传到你的 GitHub 仓库
2. 推送代码到 `main` 或 `master` 分支
3. GitHub Actions 会自动触发编译
4. 编译完成后，在仓库的 **Actions** → **Artifacts** 中下载编译好的 JAR 文件

**发布版本：**
- 创建一个新的 tag（如 `v1.0.0`）并推送
- GitHub Actions 会自动创建 Release 并附带编译好的 JAR 文件

```bash
git tag v1.0.0
git push origin v1.0.0
```

### 方法二：本地编译

需要安装Maven和JDK 17+

```bash
cd chat-bridge
mvn clean package
```

编译后的JAR文件位于`target/chat-bridge-1.0.0.jar`

## 多服务器部署

1. 在每个服务器上安装此插件
2. 配置每个服务器的`server.name`为唯一值
3. 配置每个服务器的`server.display-name`
4. 确保所有服务器连接到同一个Redis实例

示例配置：

**服务器1 (生存服)**
```yaml
server:
  name: "survival"
  display-name: "&a&l生存服"
```

**服务器2 (创造服)**
```yaml
server:
  name: "creative"
  display-name: "&b&l创造服"
```

**服务器3 (小游戏服)**
```yaml
server:
  name: "minigames"
  display-name: "&e&l小游戏服"
```

## 依赖插件（可选）

- **LuckPerms** - 提供玩家前缀/后缀
- **Vault** - 提供玩家前缀/后缀（备选）

## 常见问题

### Q: 消息没有同步到其他服务器？
A: 请检查：
1. Redis服务是否正常运行
2. 所有服务器是否连接到同一个Redis实例
3. 使用`/chatbridge status`检查Redis连接状态

### Q: 玩家前缀/后缀不显示？
A: 请确保已安装LuckPerms或Vault插件，并正确配置了玩家权限组的前缀/后缀。

### Q: 如何禁用某些消息同步？
A: 在`config.yml`中设置相应的选项为`false`：
- `chat.enabled` - 禁用聊天同步
- `chat.sync-join-quit` - 禁用加入/退出消息同步
- `chat.sync-server-status` - 禁用服务器状态同步

## 开发信息

- 作者: ChatBridge Team
- 版本: 1.0.0
- API: Paper API 1.20.4

## 许可证

MIT License