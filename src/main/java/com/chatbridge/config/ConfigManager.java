package com.chatbridge.config;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.security.BanWordsProvider;
import com.chatbridge.util.CacheConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * 配置管理器
 * 负责加载和管理插件配置
 */
public class ConfigManager {

    private final ChatBridgePlugin plugin;
    private BanWordsProvider banWordsProvider;
    private boolean banWordsProviderInitialized = false;

    // 安全配置
    private String serverKey;
    private String keysUrl;
    private int keysRefreshInterval;

    // 服务器配置
    private String serverName;
    private String serverDisplayName;
    private boolean serverPrefixEnabled;
    private String serverPrefixFormat;
    private String serverPrefixPosition;

    // 聊天配置
    private boolean chatEnabled;
    private boolean syncServerStatus;

    // 过滤器配置
    private boolean filterEnabled = true;
    private String filterReplacement = "***";

    // 调试模式
    private boolean debug;

    public ConfigManager(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.banWordsProvider = new BanWordsProvider(plugin);
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 加载安全配置
        serverKey = config.getString("security.server-key", "your-secret-key-here");
        keysUrl = CacheConfig.getKeysUrl();
        keysRefreshInterval = CacheConfig.getKeysRefreshInterval();

        // 加载服务器配置
        serverName = config.getString("server.name", "Server1");
        serverDisplayName = config.getString("server.display-name", "&a&l生存服");
        serverPrefixEnabled = config.getBoolean("server.prefix.enabled", true);
        serverPrefixFormat = config.getString("server.prefix.format", "&7[&b{server}&7]");
        serverPrefixPosition = config.getString("server.prefix.position", "before_name");

        // 加载聊天配置
        chatEnabled = config.getBoolean("chat.enabled", true);
        syncServerStatus = config.getBoolean("chat.sync-server-status", true);

        // 加载过滤器配置
        
        // 初始化违禁词提供者（已启用）
        if (!banWordsProviderInitialized) {
            banWordsProvider.initialize(
                CacheConfig.getBanWordsUrl(),
                1440 // 1天刷新一次
            );
            banWordsProviderInitialized = true;
        } else {
            banWordsProvider.refresh();
        }

        // 加载调试模式
        
        // 校验密钥和违禁词缓存（每次启动和 reload 时）
        plugin.getLogger().info("§7  - 校验密钥和违禁词缓存...");
        debug = config.getBoolean("debug", false);
    }

    // 安全配置Getters
    public String getServerKey() {
        return serverKey;
    }
    
    public String getKeysUrl() {
        return keysUrl;
    }
    
    public int getKeysRefreshInterval() {
        return keysRefreshInterval;
    }

    // 服务器配置Getters
    public String getServerName() {
        return serverName;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public boolean isServerPrefixEnabled() {
        return serverPrefixEnabled;
    }

    public String getServerPrefixFormat() {
        return serverPrefixFormat;
    }

    public String getServerPrefixPosition() {
        return serverPrefixPosition;
    }

    /**
     * 获取格式化后的服务器前缀
     */
    public String getFormattedServerPrefix() {
        if (!serverPrefixEnabled) {
            return "";
        }
        return serverPrefixFormat.replace("{server}", serverDisplayName);
    }

    // 聊天配置Getters
    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public boolean isSyncServerStatus() {
        return syncServerStatus;
    }

    // 过滤器配置Getters
    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public String getFilterReplacement() {
        return filterReplacement;
    }

    // 调试模式Getter
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * 获取违禁词提供者
     */
    public BanWordsProvider getBanWordsProvider() {
        return banWordsProvider;
    }
    
    /**
     * 刷新违禁词列表
     */
    /**
     * 刷新密钥列表
     */
    public void refreshKeys() {
        // 密钥列表由 KeyProvider 自动管理，初始化时已校验 MD5
    }

    /**
     * 刷新所有缓存（密钥和违禁词）
     */
    public void refreshAllCaches() {
        refreshKeys();
        refreshBanWords();
    }


    public void refreshBanWords() {
        if (banWordsProvider != null) {
            banWordsProvider.refresh();
        }
    }

    /**
     * 过滤消息中的敏感词
     */
    public String filterMessage(String message) {
        if (!filterEnabled) {
            return message;
        }

        return banWordsProvider.filterMessage(message, filterReplacement);
    }
}