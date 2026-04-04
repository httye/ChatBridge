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

        // 发布到Redis
        plugin.getRedisManager().publishChatMessage(chatMessage);
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

        // 尝试获取Vault前缀
        try {
            Object chat = plugin.getServer().getServicesManager()
                .load(Class.forName("net.milkbowl.vault.chat.Chat"));
            if (chat != null) {
                String prefix = (String) chat.getClass().getMethod("getPlayerPrefix", Player.class)
                    .invoke(chat, player);
                return prefix != null ? prefix : "";
            }
        } catch (Exception e) {
            // 忽略异常，使用默认值
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

        // 尝试获取Vault后缀
        try {
            Object chat = plugin.getServer().getServicesManager()
                .load(Class.forName("net.milkbowl.vault.chat.Chat"));
            if (chat != null) {
                String suffix = (String) chat.getClass().getMethod("getPlayerSuffix", Player.class)
                    .invoke(chat, player);
                return suffix != null ? suffix : "";
            }
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }

        return "";
    }
}