package com.chatbridge.storage;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.security.SecureRedisClient;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据存储管理器
 * 提供安全的数据存储和读取功能
 *
 * 权限规则：
 * - 读取：可以读取所有服务器的数据
 * - 写入：只能写入当前服务器的数据
 * - 键格式：chatplugin:{type}:{serverName}:{identifier}
 */
public class DataStorageManager {

    private final ChatBridgePlugin plugin;
    private final SecureRedisClient redisClient;
    private final Gson gson;
    
    // 本地缓存
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();
    
    // 数据类型
    public static final String TYPE_PLAYER_DATA = "player";
    public static final String TYPE_SERVER_DATA = "server";
    public static final String TYPE_CHAT_HISTORY = "chat";
    public static final String TYPE_STATS = "stats";
    public static final String TYPE_TEMP = "temp";

    public DataStorageManager(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.redisClient = plugin.getSecureRedisClient();
        this.gson = new Gson();
    }
    
    // ==================== 玩家数据存储 ====================
    
    /**
     * 保存玩家数据（只能保存到当前服务器）
     */
    public boolean savePlayerData(String playerUuid, String key, Object value) {
        String redisKey = redisClient.generateOwnKey(TYPE_PLAYER_DATA, playerUuid + ":" + key);
        String jsonValue = gson.toJson(value);
        return redisClient.hset(redisKey, "value", jsonValue);
    }
    
    /**
     * 读取玩家数据（可以读取任何服务器的数据）
     */
    public <T> T loadPlayerData(String serverName, String playerUuid, String key, Class<T> type) {
        // 首先检查本地缓存
        String cacheKey = serverName + ":" + playerUuid + ":" + key;
        if (localCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            T cached = (T) localCache.get(cacheKey);
            return cached;
        }
        
        // 从Redis读取
        String redisKey = "chatplugin:" + TYPE_PLAYER_DATA + ":" + serverName + ":" + playerUuid + ":" + key;
        String jsonValue = redisClient.hget(redisKey, "value");
        
        if (jsonValue != null) {
            T value = gson.fromJson(jsonValue, type);
            localCache.put(cacheKey, value);
            return value;
        }
        return null;
    }
    
    /**
     * 读取当前服务器的玩家数据
     */
    public <T> T loadOwnPlayerData(String playerUuid, String key, Class<T> type) {
        return loadPlayerData(plugin.getConfigManager().getServerName(), playerUuid, key, type);
    }
    
    // ==================== 服务器数据存储 ====================
    
    /**
     * 保存服务器数据（只能保存到当前服务器）
     */
    public boolean saveServerData(String key, Object value) {
        String redisKey = redisClient.generateOwnKey(TYPE_SERVER_DATA, key);
        String jsonValue = gson.toJson(value);
        return redisClient.set(redisKey, jsonValue);
    }
    
    /**
     * 读取服务器数据（可以读取任何服务器的数据）
     */
    public <T> T loadServerData(String serverName, String key, Class<T> type) {
        String redisKey = "chatplugin:" + TYPE_SERVER_DATA + ":" + serverName + ":" + key;
        String jsonValue = redisClient.get(redisKey);
        
        if (jsonValue != null) {
            return gson.fromJson(jsonValue, type);
        }
        return null;
    }
    
    /**
     * 读取当前服务器的数据
     */
    public <T> T loadOwnServerData(String key, Class<T> type) {
        return loadServerData(plugin.getConfigManager().getServerName(), key, type);
    }
    
    // ==================== 聊天历史存储 ====================
    
    /**
     * 添加聊天记录（只能添加到当前服务器）
     */
    public boolean addChatHistory(String message, String playerName, long timestamp) {
        String redisKey = redisClient.generateOwnKey(TYPE_CHAT_HISTORY);
        Map<String, String> chatRecord = new HashMap<>();
        chatRecord.put("message", message);
        chatRecord.put("player", playerName);
        chatRecord.put("timestamp", String.valueOf(timestamp));
        chatRecord.put("server", plugin.getConfigManager().getServerName());
        
        return redisClient.lpush(redisKey, gson.toJson(chatRecord));
    }
    
    /**
     * 获取聊天历史（可以获取任何服务器的聊天历史）
     */
    public Set<String> getChatHistory(String serverName, long count) {
        String redisKey = "chatplugin:" + TYPE_CHAT_HISTORY + ":" + serverName;
        return redisClient.zrange(redisKey, 0, count - 1);
    }
    
    // ==================== 统计数据存储 ====================
    
    /**
     * 更新统计数据（只能更新当前服务器的统计）
     */
    public boolean incrementStats(String key, long amount) {
        String redisKey = redisClient.generateOwnKey(TYPE_STATS);
        String currentValue = redisClient.hget(redisKey, key);
        
        long newValue = (currentValue != null ? Long.parseLong(currentValue) : 0) + amount;
        return redisClient.hset(redisKey, key, String.valueOf(newValue));
    }
    
    /**
     * 获取统计数据（可以获取任何服务器的统计）
     */
    public long getStats(String serverName, String key) {
        String redisKey = "chatplugin:" + TYPE_STATS + ":" + serverName;
        String value = redisClient.hget(redisKey, key);
        return value != null ? Long.parseLong(value) : 0;
    }
    
    /**
     * 获取当前服务器的统计数据
     */
    public long getOwnStats(String key) {
        return getStats(plugin.getConfigManager().getServerName(), key);
    }
    
    // ==================== 临时数据存储 ====================
    
    /**
     * 设置临时数据（带过期时间，只能设置当前服务器的数据）
     */
    public boolean setTempData(String key, Object value, int expireSeconds) {
        String redisKey = redisClient.generateOwnKey(TYPE_TEMP, key);
        String jsonValue = gson.toJson(value);
        return redisClient.setex(redisKey, expireSeconds, jsonValue);
    }
    
    /**
     * 获取临时数据（可以获取任何服务器的数据）
     */
    public <T> T getTempData(String serverName, String key, Class<T> type) {
        String redisKey = "chatplugin:" + TYPE_TEMP + ":" + serverName + ":" + key;
        String jsonValue = redisClient.get(redisKey);
        
        if (jsonValue != null) {
            return gson.fromJson(jsonValue, type);
        }
        return null;
    }
    
    /**
     * 获取当前服务器的临时数据
     */
    public <T> T getOwnTempData(String key, Class<T> type) {
        return getTempData(plugin.getConfigManager().getServerName(), key, type);
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清除本地缓存
     */
    public void clearLocalCache() {
        localCache.clear();
    }
    
    /**
     * 清除指定键的本地缓存
     */
    public void clearLocalCache(String key) {
        localCache.remove(key);
    }
    
    // ==================== 数据清理 ====================
    
    /**
     * 清理当前服务器的过期数据
     */
    public int cleanupExpiredData(long maxAgeMs) {
        return redisClient.getPermissionManager().cleanupOwnExpiredData(
            plugin.getRedisManager().getResource(),
            maxAgeMs
        );
    }
    
    // ==================== 数据迁移辅助 ====================
    
    /**
     * 检查数据是否属于当前服务器
     */
    public boolean isOwnData(String key) {
        return redisClient.isOwnKey(key);
    }
    
    /**
     * 获取所有服务器的数据键（只读）
     */
    public Set<String> getAllServerDataKeys() {
        return redisClient.scanPluginKeys(TYPE_SERVER_DATA + ":*");
    }
    
    /**
     * 获取当前服务器的数据键
     */
    public Set<String> getOwnDataKeys() {
        return redisClient.scanOwnKeys("*");
    }
}