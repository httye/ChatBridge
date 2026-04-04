package com.chatbridge.listener;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.config.ConfigManager;
import com.chatbridge.model.PlayerJoinMessage;
import com.chatbridge.model.PlayerQuitMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家事件监听器
 * 监听玩家加入和退出事件
 */
public class PlayerListener implements Listener {

    private final ChatBridgePlugin plugin;

    public PlayerListener(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().isSyncJoinQuit()) {
            return;
        }

        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();

        // 创建加入消息
        PlayerJoinMessage joinMessage = new PlayerJoinMessage(
            config.getServerName(),
            config.getServerDisplayName(),
            player.getName(),
            player.getDisplayName()
        );

        // 发布到Redis
        plugin.getRedisManager().publishPlayerJoin(joinMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfigManager().isSyncJoinQuit()) {
            return;
        }

        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();

        // 创建退出消息
        PlayerQuitMessage quitMessage = new PlayerQuitMessage(
            config.getServerName(),
            config.getServerDisplayName(),
            player.getName(),
            player.getDisplayName(),
            ""
        );

        // 发布到Redis
        plugin.getRedisManager().publishPlayerQuit(quitMessage);
    }
}