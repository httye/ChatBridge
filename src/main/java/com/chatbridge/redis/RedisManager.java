package com.chatbridge.redis;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.model.ChatMessage;
import com.chatbridge.model.ServerStatusMessage;
import com.chatbridge.security.RedisPermissionManager;
import com.chatbridge.util.CacheConfig;
import com.google.gson.Gson;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Redis管理器
 * 负责Redis连接和消息发布
 */
public class RedisManager {

    private final ChatBridgePlugin plugin;
    private final Gson gson;
    private JedisPool jedisPool;
    private ExecutorService executorService;

    // Redis频道名称
    public static final String CHANNEL_CHAT = "chatplugin:chat";
    public static final String CHANNEL_STATUS = "chatplugin:status";

    public RedisManager(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 初始化Redis连接池
     */
    public void initialize() {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(CacheConfig.getPoolMaxTotal());
        poolConfig.setMaxIdle(CacheConfig.getPoolMaxIdle());
        poolConfig.setMinIdle(CacheConfig.getPoolMinIdle());
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        // 从缓存配置读取连接参数
        String username = CacheConfig.getUsername();
        String password = CacheConfig.getPassword();

        if (password != null && !password.isEmpty()) {
            if (username != null && !username.isEmpty()) {
                // Redis 7.0 ACL认证模式 (需要用户名和密码)
                jedisPool = new JedisPool(
                    poolConfig,
                    CacheConfig.getHost(),
                    CacheConfig.getPort(),
                    5000,
                    username,
                    password,
                    CacheConfig.getDatabase(),
                    false
                );
            } else {
                // 传统密码认证模式
                jedisPool = new JedisPool(
                    poolConfig,
                    CacheConfig.getHost(),
                    CacheConfig.getPort(),
                    5000,
                    password,
                    CacheConfig.getDatabase()
                );
            }
        } else {
            // 无密码连接
            jedisPool = new JedisPool(
                poolConfig,
                CacheConfig.getHost(),
                CacheConfig.getPort(),
                5000,
                null,
                CacheConfig.getDatabase()
            );
        }

        // 测试连接
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
        }
    }

    /**
     * 发布聊天消息
     */
    public void publishChatMessage(ChatMessage message) {
        String json = gson.toJson(message);
        publish(CHANNEL_CHAT, json);
    }

    /**
     * 发布服务器状态消息
     */
    public void publishServerStatus(String status) {
        ServerStatusMessage message = new ServerStatusMessage(
            plugin.getConfigManager().getServerKey(),
            plugin.getConfigManager().getServerName(),
            status,
            System.currentTimeMillis()
        );
        String json = gson.toJson(message);
        publish(CHANNEL_STATUS, json);
    }

    /**
     * 发布消息到Redis频道
     * 所有发布操作都会验证频道是否为插件频道
     */
    private void publish(String channel, String message) {
        // 验证频道是否为插件频道
        if (!channel.startsWith(RedisPermissionManager.PLUGIN_KEY_PREFIX)) {
            plugin.getLogger().warning("[Redis] 拒绝发布到非插件频道: " + channel);
            return;
        }
        
        executorService.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, message);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("[Redis] 发布消息到 " + channel + ": " + message);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[Redis] 发布消息失败: " + e.getMessage());
            }
        });
    }

    /**
     * 获取Jedis连接
     */
    public Jedis getResource() {
        return jedisPool.getResource();
    }

    /**
     * 关闭Redis连接池
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    /**
     * 检查连接是否正常
     */
    public boolean isConnected() {
        if (jedisPool == null || jedisPool.isClosed()) {
            return false;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
