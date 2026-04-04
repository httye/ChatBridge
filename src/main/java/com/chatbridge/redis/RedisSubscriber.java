package com.chatbridge.redis;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.model.ChatMessage;
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
            
            // 验证服务器密钥
            if (!validateServerKey(chatMessage.getServerKey())) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("[Redis] 收到未授权的聊天消息，密钥不匹配");
                }
                return;
            }
            
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
     * 处理服务器状态消息
     */
    private void handleStatusMessage(String message) {
        if (!plugin.getConfigManager().isSyncServerStatus()) {
            return;
        }

        try {
            ServerStatusMessage statusMessage = gson.fromJson(message, ServerStatusMessage.class);
            
            // 验证服务器密钥
            if (!validateServerKey(statusMessage.getServerKey())) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("[Redis] 收到未授权的状态消息，密钥不匹配");
                }
                return;
            }
            
            // 忽略来自本服务器的消息
            if (statusMessage.getServerName().equals(plugin.getConfigManager().getServerName())) {
                return;
            }

            Component component = MessageUtil.formatStatusMessage(statusMessage, plugin.getConfigManager());
            
            // 向所有在线玩家广播服务器状态消息（不受玩家聊天开关影响）
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(component);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("[Redis] 处理状态消息失败: " + e.getMessage());
        }
    }

    /**
     * 验证服务器密钥
     * 使用KeyProvider验证密钥是否在有效密钥列表中
     */
    private boolean validateServerKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        // 使用KeyProvider验证密钥
        return plugin.getKeyProvider().isValidKey(key);
    }
}
