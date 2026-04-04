package com.chatbridge.commands;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.config.ConfigManager;
import com.chatbridge.model.ChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 全局聊天命令处理器
 */
public class GlobalChatCommand implements CommandExecutor {

    private final ChatBridgePlugin plugin;

    public GlobalChatCommand(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbridge.global")) {
            sender.sendMessage(Component.text("你没有权限执行此命令!")
                .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("用法: /globalchat <消息内容>")
                .color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getConfigManager().isChatEnabled()) {
            sender.sendMessage(Component.text("全局聊天功能已禁用!")
                .color(NamedTextColor.RED));
            return true;
        }

        // 构建消息
        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            if (messageBuilder.length() > 0) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(arg);
        }

        String message = messageBuilder.toString();
        ConfigManager config = plugin.getConfigManager();

        // 过滤敏感词
        message = config.filterMessage(message);

        // 获取玩家信息
        String playerName = sender.getName();
        String playerDisplayName = sender instanceof Player 
            ? ((Player) sender).getDisplayName() 
            : sender.getName();
        String prefix = "";
        String suffix = "";

        // 如果是玩家，获取前缀和后缀
        if (sender instanceof Player) {
            Player player = (Player) sender;
            prefix = getPlayerPrefix(player);
            suffix = getPlayerSuffix(player);
        }

        // 创建聊天消息
        ChatMessage chatMessage = new ChatMessage(
            config.getServerName(),
            config.getServerDisplayName(),
            config.getFormattedServerPrefix(),
            config.isServerPrefixEnabled(),
            config.getServerPrefixPosition(),
            playerName,
            playerDisplayName,
            message,
            config.getDefaultChannel(),
            prefix,
            suffix
        );

        // 发布到Redis
        plugin.getRedisManager().publishChatMessage(chatMessage);

        // 发送确认消息
        sender.sendMessage(Component.text("[全局] ")
            .color(NamedTextColor.GOLD)
            .append(Component.text(message).color(NamedTextColor.WHITE)));

        return true;
    }

    /**
     * 获取玩家前缀
     * 使用反射来支持可选的权限插件
     */
    private String getPlayerPrefix(Player player) {
        // 尝试获取LuckPerms前缀
        try {
            Object luckPerms = plugin.getServer().getServicesManager()
                .load(Class.forName("net.luckperms.api.LuckPerms"));
            if (luckPerms != null) {
                Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
                Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String prefix = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);
                    return prefix != null ? prefix : "";
                }
            }
        } catch (Exception e) {
            // 忽略异常，尝试其他方式
        }

        return "";
    }

    /**
     * 获取玩家后缀
     * 使用反射来支持可选的权限插件
     */
    private String getPlayerSuffix(Player player) {
        // 尝试获取LuckPerms后缀
        try {
            Object luckPerms = plugin.getServer().getServicesManager()
                .load(Class.forName("net.luckperms.api.LuckPerms"));
            if (luckPerms != null) {
                Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
                Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String suffix = (String) metaData.getClass().getMethod("getSuffix").invoke(metaData);
                    return suffix != null ? suffix : "";
                }
            }
        } catch (Exception e) {
            // 忽略异常，尝试其他方式
        }

        return "";
    }
}