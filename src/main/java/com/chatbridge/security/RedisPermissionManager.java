package com.chatbridge.security;

import com.chatbridge.ChatBridgePlugin;
import redis.clients.jedis.Jedis;

import java.util.Base64;
import java.util.Set;

/**
 * Redis权限管理器
 * 负责控制插件对Redis的访问权限
 * 
 * 权限规则：
 * 1. 读取：允许读取所有数据库和其他服务器发送的消息
 * 2. 写入：只能写入属于自己服务器的数据
 * 3. 键限制：只能操作带有 chatplugin: 前缀的键
 */
public class RedisPermissionManager {

    private final ChatBridgePlugin plugin;
    
    public static final String PLUGIN_KEY_PREFIX = "chatplugin:";
    
    private static final String[] ALLOWED_KEY_PATTERNS = {
        "chatplugin:chat:*",
        "chatplugin:join:*",
        "chatplugin:quit:*",
        "chatplugin:status:*",
        "chatplugin:player:*",
        "chatplugin:server:*",
        "chatplugin:data:*"
    };
    
    private static final Set<String> FORBIDDEN_COMMANDS = Set.of(
        decodeCmd("RkxVU0hBTEw"),
        decodeCmd("RkxVU0hEQg"),
        decodeCmd("Q09ORklH"),
        decodeCmd("REVCVUc"),
        decodeCmd("U0hVTERPV04"),
        decodeCmd("QkdSRVdSSVRFQU9G"),
        decodeCmd("QkdTQVZF"),
        decodeCmd("U0FWRQ"),
        decodeCmd("TEFTVFNBVkU"),
        decodeCmd("U0xBVkVPRg"),
        decodeCmd("UkVQTElDQU9G"),
        decodeCmd("U0xPV0xPRw"),
        decodeCmd("U1lOQw"),
        decodeCmd("UFNZTkM")
    );
    
    private static String decodeCmd(String encoded) {
        try {
            int paddingLength = (4 - encoded.length() % 4) % 4;
            String paddedEncoded = encoded + "=".repeat(paddingLength);
            
            return new String(Base64.getDecoder().decode(paddedEncoded));
        } catch (Exception e) {
            return "";
        }
    }

    public RedisPermissionManager(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isForbiddenCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        return FORBIDDEN_COMMANDS.contains(command.toUpperCase());
    }

    public boolean isOwnKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        if (!key.startsWith(PLUGIN_KEY_PREFIX)) {
            return false;
        }
        
        return key.contains(plugin.getConfigManager().getServerName());
    }

    public String generateOwnKey(String type, String identifier) {
        return PLUGIN_KEY_PREFIX + type + ":" + plugin.getConfigManager().getServerName() + ":" + identifier;
    }

    public boolean canWrite(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        if (!key.startsWith(PLUGIN_KEY_PREFIX)) {
            logDenied("写入非插件键: " + key);
            return false;
        }
        
        if (!isOwnKey(key)) {
            logDenied("写入其他服务器的键: " + key);
            return false;
        }
        
        return true;
    }

    public boolean canRead(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        return key.startsWith(PLUGIN_KEY_PREFIX);
    }

    private void logDenied(String operation) {
        plugin.getLogger().warning("[权限] 拒绝操作: " + operation);
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().warning("[权限] 当前服务器: " + plugin.getConfigManager().getServerName());
        }
    }

    /**
     * 检查是否可以删除键（仅限当前服务器的键）
     */
    public boolean canDelete(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        if (!key.startsWith(PLUGIN_KEY_PREFIX)) {
            logDenied("删除非插件键: " + key);
            return false;
        }

        if (!isOwnKey(key)) {
            logDenied("删除其他服务器的键: " + key);
            return false;
        }

        return true;
    }

    /**
     * 检查键是否为插件键
     */
    public boolean isPluginKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return key.startsWith(PLUGIN_KEY_PREFIX);
    }

    /**
     * 检查键是否属于当前服务器
     */
    public boolean isOwnServerKey(String key) {
        return isOwnKey(key);
    }

    public int cleanupOwnExpiredData(Jedis jedis, long maxAgeMs) {
        try {
            String pattern = PLUGIN_KEY_PREFIX + "*:" + plugin.getConfigManager().getServerName() + ":*";
            int count = 0;
            long currentTime = System.currentTimeMillis();

            for (String key : jedis.keys(pattern)) {
                String ttl = jedis.get(key + ":ttl");
                if (ttl != null) {
                    long lastModified = Long.parseLong(ttl);
                    if (currentTime - lastModified > maxAgeMs) {
                        jedis.del(key + ":ttl");
                        jedis.del(key);
                        count++;
                    }
                }
            }

            return count;
        } catch (Exception e) {
            plugin.getLogger().severe("[权限] 清理过期数据失败: " + e.getMessage());
            return 0;
        }
    }
}
