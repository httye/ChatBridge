package com.chatbridge;

import com.chatbridge.commands.ChatBridgeCommand;
import com.chatbridge.commands.GlobalChatCommand;
import com.chatbridge.commands.ToggleChatCommand;
import com.chatbridge.config.ConfigManager;
import com.chatbridge.listener.ChatListener;
import com.chatbridge.listener.PlayerListener;
import com.chatbridge.redis.RedisManager;
import com.chatbridge.redis.RedisSubscriber;
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
    
    // 存储禁用全局聊天的玩家
    private final Map<UUID, Boolean> toggleChatStatus = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化Redis管理器
        try {
            redisManager = new RedisManager(this);
            redisManager.initialize();
            
            // 初始化Redis订阅者
            redisSubscriber = new RedisSubscriber(this);
            redisSubscriber.start();
            
            getLogger().info("Redis连接成功!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Redis连接失败!", e);
            getLogger().severe("请检查Redis配置并确保Redis服务正在运行!");
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
        
        getLogger().info("ChatBridge 插件已启用!");
        getLogger().info("服务器名称: " + configManager.getServerName());
    }

    @Override
    public void onDisable() {
        // 发送服务器关闭消息到其他服务器
        if (configManager.isSyncServerStatus() && redisManager != null) {
            redisManager.publishServerStatus("stop");
        }
        
        // 停止Redis订阅者
        if (redisSubscriber != null) {
            redisSubscriber.stop();
        }
        
        // 关闭Redis连接
        if (redisManager != null) {
            redisManager.shutdown();
        }
        
        getLogger().info("ChatBridge 插件已禁用!");
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
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    /**
     * 重载插件
     */
    public void reload() {
        configManager.loadConfig();
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
}