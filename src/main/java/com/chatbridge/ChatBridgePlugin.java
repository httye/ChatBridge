package com.chatbridge;

import com.chatbridge.commands.ChatBridgeCommand;
import com.chatbridge.commands.GlobalChatCommand;
import com.chatbridge.commands.ToggleChatCommand;
import com.chatbridge.config.ConfigManager;
import com.chatbridge.listener.ChatListener;
import com.chatbridge.redis.RedisManager;
import com.chatbridge.redis.RedisSubscriber;
import com.chatbridge.security.KeyProvider;
import com.chatbridge.security.SecureRedisClient;
import com.chatbridge.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * ChatBridge 主插件类
 * 实现多服务器聊天互通功能
 */
public class ChatBridgePlugin extends JavaPlugin {
    private static ChatBridgePlugin instance;
    private ConfigManager configManager;
    private RedisManager redisManager;
    private RedisSubscriber redisSubscriber;
    private SecureRedisClient secureRedisClient;
    private KeyProvider keyProvider;
    
    // 存储禁用全局聊天的玩家
    private final Map<UUID, Boolean> toggleChatStatus = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        
        // 显示版权信息
        displayCopyright();
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 强制验证所有缓存数据
        getLogger().info("§7  - 正在验证缓存数据完整性...");
        
        // 等待 MD5 列表加载完成（最多等待10秒）
        if (configManager.getMD5Validator() != null) {
            int retries = 20;
            while (retries > 0 && !configManager.getMD5Validator().isLoaded()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                retries--;
            }
            if (retries > 0) {
                getLogger().info("§7  - MD5 列表已加载");
            }
        }
        
        // 验证服务器名称是否在允许列表中
        String serverName = configManager.getServerName();
        if (configManager.getServerNameProvider() != null) {
            // 等待服务器名称列表加载完成（最多等待10秒）
            int retries = 20;
            boolean isValid = false;
            while (retries > 0) {
                if (configManager.getServerNameProvider().isValidServerName(serverName)) {
                    isValid = true;
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                retries--;
            }
            
            if (!isValid) {
                getLogger().severe("§c✘ 服务器名称 '" + serverName + "' 不在允许的列表中!");
                getLogger().severe("§c  请联系管理员将服务器名称添加到允许列表中!");
                getLogger().severe("§c  当前允许的服务器名称: " + 
                    configManager.getServerNameProvider().getValidServerNames());
                getServer().getPluginManager().disablePlugin(this);
                return;
            } else {
                getLogger().info("§a✔ §f服务器名称验证通过: " + serverName);
            }
        }
        
        // 初始化密钥提供者
        keyProvider = new KeyProvider(this);
        keyProvider.initialize(
            configManager.getKeysUrl(),
            configManager.getKeysRefreshInterval()
        );
        
        // 初始化Redis管理器
        try {
            getLogger().info("正在连接到中转服务器...");
            redisManager = new RedisManager(this);
            redisManager.initialize();
            
            // 初始化安全Redis客户端
            secureRedisClient = new SecureRedisClient(this);
            getLogger().info("§7  - 安全Redis客户端已初始化");
            
            // 初始化Redis订阅者
            redisSubscriber = new RedisSubscriber(this);
            redisSubscriber.start();
            
            getLogger().info("§a✔ §f成功连接到中转服务器!");
            getLogger().info("§b  - 服务器标识: §f" + configManager.getServerName());
            
            // 向所有在线玩家发送连接成功提示
            broadcastRedisConnected();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "§c✘ 连接中转服务器失败!", e);
            getLogger().severe("§c  请检查Redis配置并确保Redis服务正在运行!");
            
            // 向所有在线玩家发送连接失败提示
            broadcastRedisFailed();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 注册命令
        registerCommands();
        
        // 注册监听器
        registerListeners();
        
        // 发送服务器启动消息到其他服务器
        if (configManager.isSyncServerStatus()) {
            redisManager.publishServerStatus("start");
        }
        
        getLogger().info("§a✔ §fChatBridge 插件已启用!");
    }
    
    /**
     * 显示版权信息
     */
    private void displayCopyright() {
        getLogger().info("§e========================================");
        getLogger().info("§b   ____ _               _     ____ _           _   ");
        getLogger().info("§b  / ___| |__   ___ _ __| | __/ ___| |__   __ _| |_ ");
        getLogger().info("§b | |   | '_ \\ / _ \\ '__| |/ / |   | '_ \\ / _` | __|");
        getLogger().info("§b | |___| | | |  __/ |  |   <| |___| | | | (_| | |_ ");
        getLogger().info("§b  \\____|_| |_|\\___|_|  |_|\\_\\\\____|_| |_|\\__,_|\\__|");
        getLogger().info("§e========================================");
        getLogger().info("§f  版本: §a" + getDescription().getVersion());
        getLogger().info("§f  作者: §bhttye");
        getLogger().info("§f  描述: §7多服务器聊天互通插件");
        getLogger().info("§e========================================");
        getLogger().info("§6  Copyright (c) 2026 httye");
        getLogger().info("§6  本插件为开源软件，遵循 MIT 许可证");
        getLogger().info("§6  GitHub: https://github.com/httye/ChatBridge");
        getLogger().info("§e========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c正在关闭 ChatBridge...");
        
        // 发送服务器关闭消息到其他服务器
        if (configManager.isSyncServerStatus() && redisManager != null) {
            redisManager.publishServerStatus("stop");
        }
        
        // 停止Redis订阅者
        if (redisSubscriber != null) {
            redisSubscriber.stop();
            getLogger().info("§7  - 已停止消息订阅");
        }
        
        // 关闭密钥提供者

        // 关闭违禁词提供者
        if (configManager.getBanWordsProvider() != null) {
            configManager.getBanWordsProvider().shutdown();
            getLogger().info("§7  - 已停止违禁词提供者");
        }
        
        // 关闭服务器名称提供者
        if (configManager.getServerNameProvider() != null) {
            configManager.getServerNameProvider().shutdown();
            getLogger().info("§7  - 已停止服务器名称提供者");
        }
        
        // 关闭 MD5 校验提供者
        if (configManager.getMD5Validator() != null) {
            configManager.getMD5Validator().shutdown();
            getLogger().info("§7  - 已停止 MD5 校验提供者");
        }
        
        if (keyProvider != null) {
            keyProvider.shutdown();
            getLogger().info("§7  - 已停止密钥提供者");
        }
        
        // 关闭Redis连接
        if (redisManager != null) {
            redisManager.shutdown();
            getLogger().info("§7  - 已断开中转服务器连接");
        }
        
        getLogger().info("§c✘ §fChatBridge 插件已禁用!");
        getLogger().info("§e感谢使用 ChatBridge!");
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        getCommand("chatbridge").setExecutor(new ChatBridgeCommand(this));
        getCommand("globalchat").setExecutor(new GlobalChatCommand(this));
        getCommand("togglechat").setExecutor(new ToggleChatCommand(this));
    }

    /**
     * 注册监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    /**
     * 重载插件
     */
    public void reload() {
        getLogger().info("§7  - 正在重新验证缓存数据...");
        
        configManager.loadConfig();
        
        // 刷新 MD5 列表（优先获取最新的 MD5）
        if (configManager.getMD5Validator() != null) {
            configManager.getMD5Validator().refresh();
        }
        
        // 刷新密钥列表（强制验证）
        if (keyProvider != null) {
            keyProvider.refresh();
        }
        
        // 刷新服务器名称列表（强制验证）
        if (configManager.getServerNameProvider() != null) {
            configManager.getServerNameProvider().refresh();
        }
        
        getLogger().info("配置已重新加载!");
    }

    /**
     * 获取插件实例
     */
    public static ChatBridgePlugin getInstance() {
        return instance;
    }

    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取Redis管理器
     */
    public RedisManager getRedisManager() {
        return redisManager;
    }
    
    /**
     * 获取安全Redis客户端
     */
    public SecureRedisClient getSecureRedisClient() {
        return secureRedisClient;
    }
    
    /**
     * 获取密钥提供者
     */
    public KeyProvider getKeyProvider() {
        return keyProvider;
    }

    /**
     * 检查玩家是否禁用了全局聊天
     */
    public boolean isChatToggled(UUID playerId) {
        return toggleChatStatus.getOrDefault(playerId, true);
    }

    /**
     * 设置玩家的全局聊天状态
     */
    public void setChatToggled(UUID playerId, boolean enabled) {
        toggleChatStatus.put(playerId, enabled);
    }

    /**
     * 切换玩家的全局聊天状态
     */
    public boolean toggleChat(UUID playerId) {
        boolean currentState = isChatToggled(playerId);
        setChatToggled(playerId, !currentState);
        return !currentState;
    }

    /**
     * 向所有在线玩家广播Redis连接成功消息
     */
    private void broadcastRedisConnected() {
        Component message = MessageUtil.toComponent(
            "&a[" + configManager.getServerDisplayName() + "&a] &f已成功连接到服务器，跨服聊天已启用！"
        );
        for (var player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * 向所有在线玩家广播Redis连接失败消息
     */
    private void broadcastRedisFailed() {
        Component message = MessageUtil.toComponent(
            "&c[ChatBridge] &f连接中转服务器失败，跨服聊天暂时不可用！"
        );
        for (var player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}