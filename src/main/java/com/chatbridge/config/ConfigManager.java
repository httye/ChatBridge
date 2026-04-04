package com.chatbridge.config;

import com.chatbridge.ChatBridgePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * 配置管理器
 * 负责加载和管理插件配置
 */
public class ConfigManager {

    private final ChatBridgePlugin plugin;

    // Redis配置
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private int redisDatabase;
    private int redisPoolMaxTotal;
    private int redisPoolMaxIdle;
    private int redisPoolMinIdle;

    // 服务器配置
    private String serverName;
    private String serverDisplayName;
    private boolean serverPrefixEnabled;
    private String serverPrefixFormat;
    private String serverPrefixPosition;

    // 聊天配置
    private String chatFormat;
    private boolean chatEnabled;
    private boolean syncJoinQuit;
    private boolean syncDeath;
    private boolean syncServerStatus;
    private String joinFormat;
    private String quitFormat;
    private String serverStartFormat;
    private String serverStopFormat;

    // 频道配置
    private String defaultChannel;
    private List<String> availableChannels;

    // 过滤器配置
    private boolean filterEnabled;
    private List<String> filterWords;
    private String filterReplacement;

    // 调试模式
    private boolean debug;

    public ConfigManager(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 加载Redis配置
        redisHost = config.getString("redis.host", "localhost");
        redisPort = config.getInt("redis.port", 6379);
        redisPassword = config.getString("redis.password", "");
        redisDatabase = config.getInt("redis.database", 0);
        redisPoolMaxTotal = config.getInt("redis.pool.max-total", 8);
        redisPoolMaxIdle = config.getInt("redis.pool.max-idle", 8);
        redisPoolMinIdle = config.getInt("redis.pool.min-idle", 0);

        // 加载服务器配置
        serverName = config.getString("server.name", "Server1");
        serverDisplayName = config.getString("server.display-name", "&a&l生存服");
        serverPrefixEnabled = config.getBoolean("server.prefix.enabled", true);
        serverPrefixFormat = config.getString("server.prefix.format", "&7[&b{server}&7]");
        serverPrefixPosition = config.getString("server.prefix.position", "before_name");

        // 加载聊天配置
        chatFormat = config.getString("chat.format", "&7[&b{server}&7] &r{prefix}{player}&r: &f{message}");
        chatEnabled = config.getBoolean("chat.enabled", true);
        syncJoinQuit = config.getBoolean("chat.sync-join-quit", true);
        syncDeath = config.getBoolean("chat.sync-death", false);
        syncServerStatus = config.getBoolean("chat.sync-server-status", true);
        joinFormat = config.getString("chat.join-format", "&7[&b{server}&7] &e{player} &a加入了游戏");
        quitFormat = config.getString("chat.quit-format", "&7[&b{server}&7] &e{player} &c离开了游戏");
        serverStartFormat = config.getString("chat.server-start-format", "&7[&b{server}&7] &a服务器已启动");
        serverStopFormat = config.getString("chat.server-stop-format", "&7[&b{server}&7] &c服务器已关闭");

        // 加载频道配置
        defaultChannel = config.getString("channels.default", "global");
        availableChannels = config.getStringList("channels.available");

        // 加载过滤器配置
        filterEnabled = config.getBoolean("filter.enabled", false);
        filterWords = config.getStringList("filter.words");
        filterReplacement = config.getString("filter.replacement", "***");

        // 加载调试模式
        debug = config.getBoolean("debug", false);
    }

    // Redis配置Getters
    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public int getRedisDatabase() {
        return redisDatabase;
    }

    public int getRedisPoolMaxTotal() {
        return redisPoolMaxTotal;
    }

    public int getRedisPoolMaxIdle() {
        return redisPoolMaxIdle;
    }

    public int getRedisPoolMinIdle() {
        return redisPoolMinIdle;
    }

    public boolean hasRedisPassword() {
        return redisPassword != null && !redisPassword.isEmpty();
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
        return serverPrefixFormat
            .replace("{server}", serverDisplayName)
            .replace("{server_name}", serverName);
    }

    // 聊天配置Getters
    public String getChatFormat() {
        return chatFormat;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public boolean isSyncJoinQuit() {
        return syncJoinQuit;
    }

    public boolean isSyncDeath() {
        return syncDeath;
    }

    public boolean isSyncServerStatus() {
        return syncServerStatus;
    }

    public String getJoinFormat() {
        return joinFormat;
    }

    public String getQuitFormat() {
        return quitFormat;
    }

    public String getServerStartFormat() {
        return serverStartFormat;
    }

    public String getServerStopFormat() {
        return serverStopFormat;
    }

    // 频道配置Getters
    public String getDefaultChannel() {
        return defaultChannel;
    }

    public List<String> getAvailableChannels() {
        return availableChannels;
    }

    // 过滤器配置Getters
    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public List<String> getFilterWords() {
        return filterWords;
    }

    public String getFilterReplacement() {
        return filterReplacement;
    }

    // 调试模式Getter
    public boolean isDebug() {
        return debug;
    }

    /**
     * 过滤消息中的敏感词
     */
    public String filterMessage(String message) {
        if (!filterEnabled) {
            return message;
        }

        String filtered = message;
        for (String word : filterWords) {
            if (word != null && !word.isEmpty()) {
                filtered = filtered.replace(word, filterReplacement);
            }
        }
        return filtered;
    }
}