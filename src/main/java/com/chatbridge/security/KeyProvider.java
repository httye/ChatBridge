package com.chatbridge.security;

import com.chatbridge.ChatBridgePlugin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 密钥提供者
 * 从远程URL获取有效的密钥列表
 * 插件只接收来自这些密钥对应频道的消息
 * 使用 MD5 校验本地缓存，每次启动和 reload 时强制校验
 */
public class KeyProvider {

    private final ChatBridgePlugin plugin;
    private final Gson gson;
    private final Set<String> validKeys;
    private ScheduledExecutorService scheduler;
    
    // 远程密钥列表URL
    private String keyListUrl;
    // 刷新间隔（分钟）
    private int refreshInterval;
    
    // 本地缓存文件
    private File cacheFile;
    private File md5File;

    public KeyProvider(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.validKeys = new HashSet<>();
        
        // 初始化本地缓存文件
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        cacheFile = new File(dataFolder, "keys.cache");
        md5File = new File(dataFolder, "keys.md5");
    }

    /**
     * 初始化密钥提供者
     * @param url 密钥列表URL
     * @param refreshIntervalMinutes 刷新间隔（分钟）
     */
    public void initialize(String url, int refreshIntervalMinutes) {
        this.keyListUrl = url;
        this.refreshInterval = refreshIntervalMinutes;
        
        // 立即加载一次（会校验 MD5）
        loadKeys();
        
        // 设置定时刷新
        if (refreshInterval > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                this::loadKeys,
                refreshInterval,
                refreshInterval,
                TimeUnit.MINUTES
            );
        }
    }

    /**
     * 从远程URL加载密钥列表
     * 每次都会校验 MD5，如果不匹配则重新下载
     */
    private void loadKeys() {
        if (keyListUrl == null || keyListUrl.isEmpty()) {
            plugin.getLogger().warning("[KeyProvider] 密钥列表URL未配置");
            return;
        }
        
        try {
            URL url = new URL(keyListUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("[KeyProvider] 获取密钥列表失败，HTTP响应码: " + responseCode);
                return;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String content = response.toString();
            
            // 计算新的 MD5
            String newMd5 = calculateMD5(content);
            
            // 检查是否需要更新
            if (needUpdate(newMd5)) {
                // 保存到本地文件
                saveToCache(content, newMd5);
                
                // 解析密钥列表
                Set<String> newKeys = parseKeys(content);
                
                if (!newKeys.isEmpty()) {
                    validKeys.clear();
                    validKeys.addAll(newKeys);
                    plugin.getLogger().info("[KeyProvider] 成功加载 " + validKeys.size() + " 个密钥");
                    
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("[KeyProvider] 密钥列表: " + validKeys);
                    }
                }
            } else {
                // MD5 匹配，从本地缓存加载
                if (!validKeys.isEmpty()) {
                    plugin.getLogger().info("[KeyProvider] 使用缓存密钥列表（" + validKeys.size() + " 个）");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[KeyProvider] 加载密钥列表失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否需要更新
     * @param newMd5 新的MD5值
     * @return 是否需要更新
     */
    private boolean needUpdate(String newMd5) {
        try {
            // 如果本地没有文件，需要下载
            if (!md5File.exists() || !cacheFile.exists()) {
                return true;
            }
            
            // 读取本地 MD5
            String localMd5 = new String(Files.readAllBytes(md5File.toPath()));
            
            // MD5 不匹配，需要更新
            return !newMd5.equals(localMd5);
        } catch (Exception e) {
            // 发生错误，重新下载
            return true;
        }
    }

    /**
     * 保存到本地缓存
     * @param content 内容
     * @param md5 MD5值
     */
    private void saveToCache(String content, String md5) {
        try {
            // 保存内容
            try (FileWriter writer = new FileWriter(cacheFile)) {
                writer.write(content);
            }
            
            // 保存 MD5
            try (FileWriter writer = new FileWriter(md5File)) {
                writer.write(md5);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[KeyProvider] 保存本地缓存失败: " + e.getMessage());
        }
    }

    /**
     * 计算 MD5
     * @param content 内容
     * @return MD5值
     */
    private String calculateMD5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(content.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析密钥列表JSON
     * 支持两种格式：
     * 1. JSON数组: ["key1", "key2", "key3"]
     * 2. JSON对象: {"keys": ["key1", "key2"], "channels": {...}}
     */
    private Set<String> parseKeys(String json) {
        Set<String> keys = new HashSet<>();
        
        try {
            // 尝试解析为数组
            if (json.trim().startsWith("[")) {
                Set<String> arrayKeys = gson.fromJson(json, new TypeToken<Set<String>>(){}.getType());
                if (arrayKeys != null) {
                    keys.addAll(arrayKeys);
                }
            } else {
                // 尝试解析为对象，提取keys字段
                KeyListResponse response = gson.fromJson(json, KeyListResponse.class);
                if (response != null && response.keys != null) {
                    keys.addAll(response.keys);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[KeyProvider] 解析密钥列表失败: " + e.getMessage());
        }
        
        return keys;
    }

    /**
     * 验证密钥是否有效
     * @param key 要验证的密钥
     * @return 是否有效
     */
    public boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return validKeys.contains(key);
    }

    /**
     * 获取所有有效密钥
     * @return 密钥集合
     */
    public Set<String> getValidKeys() {
        return new HashSet<>(validKeys);
    }

    /**
     * 手动刷新密钥列表（强制重新下载）
     */
    public void refresh() {
        loadKeys();
    }

    /**
     * 关闭密钥提供者
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * 密钥列表响应结构
     */
    private static class KeyListResponse {
        Set<String> keys;
    }
}
