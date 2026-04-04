package com.chatbridge.redis;

import com.chatbridge.ChatBridgePlugin;
import com.chatbridge.model.ChatMessage;
import com.chatbridge.model.PlayerJoinMessage;
import com.chatbridge.model.PlayerQuitMessage;
import com.chatbridge.model.ServerStatusMessage;
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
    public static final String CHANNEL_CHAT = "chatbridge:chat";
    public static final String CHANNEL_JOIN = "chatbridge:join";
    public static final String CHANNEL_QUIT = "chatbridge:quit";
    public static final String CHANNEL_STATUS = "chatbridge:status";

    public RedisManager(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 初始化Redis连接池
     */
    public void initialize() {
        var config = plugin.getConfigManager();

        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(config.getRedisPoolMaxTotal());
        poolConfig.setMaxIdle(config.getRedisPoolMaxIdle());
        poolConfig.setMinIdle(config.getRedisPoolMinIdle());
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        if (config.hasRedisPassword()) {
            jedisPool = new JedisPool(
                poolConfig,
                config.getRedisHost(),
                config.getRedisPort(),
                5000,
                config.getRedisPassword(),
                config.getRedisDatabase()
            );
        } else {
            jedisPool = new JedisPool(
                poolConfig,
                config.getRedisHost(),
                config.getRedisPort(),
                5000,
                null,
                config.getRedisDatabase()
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
     * 发布玩家加入消息
     */
    public void publishPlayerJoin(PlayerJoinMessage message) {
        String json = gson.toJson(message);
        publish(CHANNEL_JOIN, json);
    }

    /**
     * 发布玩家退出消息
     */
    public void publishPlayerQuit(PlayerQuitMessage message) {
        String json = gson.toJson(message);
        publish(CHANNEL_QUIT, json);
    }

    /**
     * 发布服务器状态消息
     */
    public void publishServerStatus(String status) {
        ServerStatusMessage message = new ServerStatusMessage(
            plugin.getConfigManager().getServerName(),
            status,
            System.currentTimeMillis()
        );
        String json = gson.toJson(message);
        publish(CHANNEL_STATUS, json);
    }

    /**
     * 发布消息到Redis频道
     */
    private void publish(String channel, String message) {
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