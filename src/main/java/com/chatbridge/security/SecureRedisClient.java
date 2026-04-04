package com.chatbridge.security;

import com.chatbridge.ChatBridgePlugin;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 安全的Redis客户端包装器
 * 所有操作都会经过权限检查
 * 
 * 权限规则：
 * - 读取：允许读取所有插件键（包括其他服务器的数据）
 * - 写入：只能写入当前服务器的键
 * - 删除：只能删除当前服务器的键
 * - 禁止危险命令
 */
public class SecureRedisClient {

    private final ChatBridgePlugin plugin;
    private final RedisPermissionManager permissionManager;
    
    public SecureRedisClient(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.permissionManager = new RedisPermissionManager(plugin);
    }
    
    /**
     * 获取权限管理器
     */
    public RedisPermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    /**
     * 在安全上下文中执行读取操作
     * @param action 要执行的操作
     * @return 操作结果
     */
    public <T> T executeRead(Function<Jedis, T> action) {
        try (Jedis jedis = plugin.getRedisManager().getResource()) {
            return action.apply(jedis);
        } catch (Exception e) {
            plugin.getLogger().severe("[SecureRedis] 读取操作失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 安全地执行写入操作
     * @param key 要写入的键
     * @param action 要执行的操作
     * @return 操作是否成功
     */
    public boolean executeWrite(String key, Function<Jedis, Boolean> action) {
        if (!permissionManager.canWrite(key)) {
            return false;
        }
        
        try (Jedis jedis = plugin.getRedisManager().getResource()) {
            return action.apply(jedis);
        } catch (Exception e) {
            plugin.getLogger().severe("[SecureRedis] 写入操作失败: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== 字符串操作 ====================
    
    /**
     * 安全设置键值（仅限当前服务器的键）
     */
    public boolean set(String key, String value) {
        return executeWrite(key, jedis -> {
            jedis.set(key, value);
            return true;
        });
    }
    
    /**
     * 安全设置键值（带过期时间，仅限当前服务器的键）
     */
    public boolean setex(String key, int seconds, String value) {
        return executeWrite(key, jedis -> {
            jedis.setex(key, seconds, value);
            return true;
        });
    }
    
    /**
     * 读取键值（允许读取所有插件键）
     */
    public String get(String key) {
        if (!permissionManager.canRead(key)) {
            return null;
        }
        return executeRead(jedis -> jedis.get(key));
    }
    
    /**
     * 安全删除键（仅限当前服务器的键）
     */
    public boolean del(String key) {
        if (!permissionManager.canDelete(key)) {
            return false;
        }
        return executeWrite(key, jedis -> jedis.del(key) > 0);
    }
    
    /**
     * 检查键是否存在（允许检查所有插件键）
     */
    public boolean exists(String key) {
        if (!permissionManager.isPluginKey(key)) {
            return false;
        }
        return executeRead(jedis -> jedis.exists(key));
    }
    
    // ==================== 哈希操作 ====================
    
    /**
     * 安全设置哈希字段（仅限当前服务器的键）
     */
    public boolean hset(String key, String field, String value) {
        return executeWrite(key, jedis -> {
            jedis.hset(key, field, value);
            return true;
        });
    }
    
    /**
     * 获取哈希字段（允许读取所有插件键）
     */
    public String hget(String key, String field) {
        if (!permissionManager.canRead(key)) {
            return null;
        }
        return executeRead(jedis -> jedis.hget(key, field));
    }
    
    /**
     * 获取所有哈希字段（允许读取所有插件键）
     */
    public Map<String, String> hgetAll(String key) {
        if (!permissionManager.canRead(key)) {
            return null;
        }
        return executeRead(jedis -> jedis.hgetAll(key));
    }
    
    /**
     * 安全删除哈希字段（仅限当前服务器的键）
     */
    public boolean hdel(String key, String... fields) {
        if (!permissionManager.canDelete(key)) {
            return false;
        }
        return executeWrite(key, jedis -> jedis.hdel(key, fields) > 0);
    }
    
    // ==================== 列表操作 ====================
    
    /**
     * 安全推入列表左侧（仅限当前服务器的键）
     */
    public boolean lpush(String key, String... values) {
        return executeWrite(key, jedis -> {
            jedis.lpush(key, values);
            return true;
        });
    }
    
    /**
     * 安全推入列表右侧（仅限当前服务器的键）
     */
    public boolean rpush(String key, String... values) {
        return executeWrite(key, jedis -> {
            jedis.rpush(key, values);
            return true;
        });
    }
    
    /**
     * 获取列表范围（允许读取所有插件键）
     */
    public List<String> lrange(String key, long start, long stop) {
        if (!permissionManager.canRead(key)) {
            return null;
        }
        return executeRead(jedis -> jedis.lrange(key, start, stop));
    }
    
    // ==================== 集合操作 ====================
    
    /**
     * 安全添加集合成员（仅限当前服务器的键）
     */
    public boolean sadd(String key, String... members) {
        return executeWrite(key, jedis -> {
            jedis.sadd(key, members);
            return true;
        });
    }
    
    /**
     * 获取集合所有成员（允许读取所有插件键）
     */
    public Set<String> smembers(String key) {
        if (!permissionManager.canRead(key)) {
            return null;
        }
        return executeRead(jedis -> jedis.smembers(key));
    }
    
    /**
     * 安全移除集合成员（仅限当前服务器的键）
     */
    public boolean srem(String key, String... members) {
        if (!permissionManager.canDelete(key)) {
            return false;
        }
        return executeWrite(key, jedis -> jedis.srem(key, members) > 0);
    }
    
    // ==================== 有序集合操作 ====================
    
    /**
     * 安全添加有序集合成员（仅限当前服务器的键）
     */
    public boolean zadd(String key, double score, String member) {
        return executeWrite(key, jedis -> {
            jedis.zadd(key, score, member);
            return true;
        });
    }
    
    /**
     * 获取有序集合范围（允许读取所有插件键）
     */
    public Set<String> zrange(String key, long start, long stop) {
        if (!permissionManager.canRead(key)) {
            return null;
        }
        return this.<Set<String>>executeRead(jedis -> jedis.zrange(key, start, stop));
    }
    
    // ==================== 键扫描操作 ====================
    
    /**
     * 扫描插件键（只返回插件命名空间的键）
     */
    public Set<String> scanPluginKeys(String pattern) {
        Function<Jedis, Set<String>> action = jedis -> {
            Set<String> result = new java.util.HashSet<>();
            String fullPattern = RedisPermissionManager.PLUGIN_KEY_PREFIX + pattern;
            result.addAll(jedis.keys(fullPattern));
            return result;
        };
        return executeRead(action);
    }
    
    /**
     * 扫描当前服务器的键
     */
    public Set<String> scanOwnKeys(String pattern) {
        String serverName = plugin.getConfigManager().getServerName();
        String fullPattern = RedisPermissionManager.PLUGIN_KEY_PREFIX + pattern + ":" + serverName;
        Function<Jedis, Set<String>> action = jedis -> jedis.keys(fullPattern);
        return executeRead(action);
    }
    
    // ==================== 过期时间操作 ====================
    
    /**
     * 安全设置过期时间（仅限当前服务器的键）
     */
    public boolean expire(String key, int seconds) {
        if (!permissionManager.canWrite(key)) {
            return false;
        }
        return executeWrite(key, jedis -> jedis.expire(key, seconds) > 0);
    }
    
    /**
     * 获取剩余过期时间（允许查询所有插件键）
     */
    public long ttl(String key) {
        if (!permissionManager.isPluginKey(key)) {
            return -2; // 键不存在
        }
        return executeRead(jedis -> jedis.ttl(key));
    }
    
    // ==================== 发布订阅操作 ====================
    
    /**
     * 发布消息到频道（消息会带有服务器标识）
     */
    public boolean publish(String channel, String message) {
        // 发布操作需要验证频道是否为插件频道
        if (!channel.startsWith(RedisPermissionManager.PLUGIN_KEY_PREFIX)) {
            plugin.getLogger().warning("[SecureRedis] 拒绝发布到非插件频道: " + channel);
            return false;
        }
        
        try (Jedis jedis = plugin.getRedisManager().getResource()) {
            jedis.publish(channel, message);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[SecureRedis] 发布消息失败: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 生成当前服务器的数据键
     */
    public String generateOwnKey(String type) {
        return permissionManager.generateOwnKey(type, "");
    }
    
    /**
     * 生成当前服务器的数据键（带标识符）
     */
    public String generateOwnKey(String type, String identifier) {
        return permissionManager.generateOwnKey(type, identifier);
    }
    
    /**
     * 检查键是否属于当前服务器
     */
    public boolean isOwnKey(String key) {
        return permissionManager.isOwnServerKey(key);
    }
    
    /**
     * 检查键是否为插件键
     */
    public boolean isPluginKey(String key) {
        return permissionManager.isPluginKey(key);
    }
}