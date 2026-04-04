package com.chatbridge.listener;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.config.ConfigManager;
import com.chatbridge.model.ChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * 聊天事件监听器
 * 监听玩家聊天并同步到其他服务器
 */
public class ChatListener implements Listener {

    private final ChatBridgePlugin plugin;

    public ChatListener(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfigManager().isChatEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // 过滤敏感词
        ConfigManager config = plugin.getConfigManager();
        message = config.filterMessage(message);

        // 获取玩家前缀和后缀 (需要权限插件支持，这里使用空字符串作为默认值)
        String prefix = getPlayerPrefix(player);
        String suffix = getPlayerSuffix(player);
// 创建聊天消息
ChatMessage chatMessage = new ChatMessage(
    config.getServerName(),
    config.getServerDisplayName(),
    config.getFormattedServerPrefix(),
    config.isServerPrefixEnabled(),
    config.getServerPrefixPosition(),
    player.getName(),
    player.getDisplayName(),
    message,
    config.getDefaultChannel(),
    prefix,
    suffix
);
        );

        // 发布到Redis
        plugin.getRedisManager().publishChatMessage(chatMessage);
    }

    /**
     * 获取玩家前缀
     * 可以在这里集成权限插件如LuckPerms、Vault等
     */
    private String getPlayerPrefix(Player player) {
        // 尝试获取LuckPerms前缀
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                net.luckperms.api.LuckPerms luckPerms = plugin.getServer().getServicesManager()
                    .load(net.luckperms.api.LuckPerms.class);
                if (luckPerms != null) {
                    net.luckperms.api.model.user.User user = luckPerms.getUserManager()
                        .getUser(player.getUniqueId());
                    if (user != null) {
                        String prefix = user.getCachedData().getMetaData().getPrefix();
                        return prefix != null ? prefix : "";
                    }
                }
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
        }

        // 尝试获取Vault前缀
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                var chat = plugin.getServer().getServicesManager()
                    .load(net.milkbowl.vault.chat.Chat.class);
                if (chat != null) {
                    String prefix = chat.getPlayerPrefix(player);
                    return prefix != null ? prefix : "";
                }
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
        }

        return "";
    }

    /**
     * 获取玩家后缀
     * 可以在这里集成权限插件如LuckPerms、Vault等
     */
    private String getPlayerSuffix(Player player) {
        // 尝试获取LuckPerms后缀
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                net.luckperms.api.LuckPerms luckPerms = plugin.getServer().getServicesManager()
                    .load(net.luckperms.api.LuckPerms.class);
                if (luckPerms != null) {
                    net.luckperms.api.model.user.User user = luckPerms.getUserManager()
                        .getUser(player.getUniqueId());
                    if (user != null) {
                        String suffix = user.getCachedData().getMetaData().getSuffix();
                        return suffix != null ? suffix : "";
                    }
                }
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
        }

        // 尝试获取Vault后缀
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                var chat = plugin.getServer().getServicesManager()
                    .load(net.milkbowl.vault.chat.Chat.class);
                if (chat != null) {
                    String suffix = chat.getPlayerSuffix(player);
                    return suffix != null ? suffix : "";
                }
            } catch (Exception e) {
                // 忽略异常，使用默认值
            }
        }

        return "";
    }
}