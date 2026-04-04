package com.chatbridge.redis;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.model.ChatMessage;
import com.chatbridge.model.PlayerJoinMessage;
import com.chatbridge.model.PlayerQuitMessage;
import com.chatbridge.model.ServerStatusMessage;
import com.chatbridge.util.MessageUtil;
import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis订阅者
 * 负责接收其他服务器的消息
 */
public class RedisSubscriber {

    private final ChatBridgePlugin plugin;
    private final Gson gson;
    private final AtomicBoolean running;
    private ExecutorService executorService;
    private JedisPubSub pubSub;

    public RedisSubscriber(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.running = new AtomicBoolean(false);
    }

    /**
     * 启动订阅者
     */
    public void start() {
        running.set(true);
        executorService = Executors.newSingleThreadExecutor();
        
        executorService.submit(() -> {
            while (running.get()) {
                try (Jedis jedis = plugin.getRedisManager().getResource()) {
                    pubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            handleMessage(channel, message);
                        }
                    };
                    
                    // 订阅所有频道
                    jedis.subscribe(pubSub, 
                        RedisManager.CHANNEL_CHAT,
                        RedisManager.CHANNEL_JOIN,
                        RedisManager.CHANNEL_QUIT,
                        RedisManager.CHANNEL_STATUS
                    );
                } catch (Exception e) {
                    if (running.get()) {
                        plugin.getLogger().severe("[Redis] 订阅连接断开，正在重连: " + e.getMessage());
                        try {
                            Thread.sleep(5000); // 5秒后重试
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * 停止订阅者
     */
    public void stop() {
        running.set(false);
        if (pubSub != null) {
            pubSub.unsubscribe();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(String channel, String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[Redis] 收到消息 from " + channel + ": " + message);
        }

        switch (channel) {
            case RedisManager.CHANNEL_CHAT:
                handleChatMessage(message);
                break;
            case RedisManager.CHANNEL_JOIN:
                handleJoinMessage(message);
                break;
            case RedisManager.CHANNEL_QUIT:
                handleQuitMessage(message);
                break;
            case RedisManager.CHANNEL_STATUS:
                handleStatusMessage(message);
                break;
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String message) {
        try {
            ChatMessage chatMessage = gson.fromJson(message, ChatMessage.class);
            
            // 忽略来自本服务器的消息
            if (chatMessage.getServerName().equals(plugin.getConfigManager().getServerName())) {
                return;
            }

            // 构建消息组件
            Component component = MessageUtil.formatChatMessage(chatMessage, plugin.getConfigManager());
            
            // 在主线程中广播消息
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var player : Bukkit.getOnlinePlayers()) {
                    // 检查玩家是否启用了全局聊天
                    if (plugin.isChatToggled(player.getUniqueId())) {
                        player.sendMessage(component);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("[Redis] 处理聊天消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理玩家加入消息
     */
    private void handleJoinMessage(String message) {
        if (!plugin.getConfigManager().isSyncJoinQuit()) {
            return;
        }

        try {
            PlayerJoinMessage joinMessage = gson.fromJson(message, PlayerJoinMessage.class);
            
            // 忽略来自本服务器的消息
            if (joinMessage.getServerName().equals(plugin.getConfigManager().getServerName())) {
                return;
            }

            Component component = MessageUtil.formatJoinMessage(joinMessage, plugin.getConfigManager());
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isChatToggled(player.getUniqueId())) {
                        player.sendMessage(component);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("[Redis] 处理加入消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理玩家退出消息
     */
    private void handleQuitMessage(String message) {
        if (!plugin.getConfigManager().isSyncJoinQuit()) {
            return;
        }

        try {
            PlayerQuitMessage quitMessage = gson.fromJson(message, PlayerQuitMessage.class);
            
            // 忽略来自本服务器的消息
            if (quitMessage.getServerName().equals(plugin.getConfigManager().getServerName())) {
                return;
            }

            Component component = MessageUtil.formatQuitMessage(quitMessage, plugin.getConfigManager());
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isChatToggled(player.getUniqueId())) {
                        player.sendMessage(component);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("[Redis] 处理退出消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理服务器状态消息
     */
    private void handleStatusMessage(String message) {
        if (!plugin.getConfigManager().isSyncServerStatus()) {
            return;
        }

        try {
            ServerStatusMessage statusMessage = gson.fromJson(message, ServerStatusMessage.class);
            
            // 忽略来自本服务器的消息
            if (statusMessage.getServerName().equals(plugin.getConfigManager().getServerName())) {
                return;
            }

            Component component = MessageUtil.formatStatusMessage(statusMessage, plugin.getConfigManager());
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isChatToggled(player.getUniqueId())) {
                        player.sendMessage(component);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("[Redis] 处理状态消息失败: " + e.getMessage());
        }
    }
}