package com.chatbridge.security;

import com.chatbridge.ChatBridgePlugin;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 校验提供者
 * 从远程 URL 获取文件的校验信息
 * 用于验证本地缓存的文件完整性
 */
public class MD5Validator {

    private final ChatBridgePlugin plugin;
    private final Gson gson;
    private Map<String, String> md5Map;
    private ScheduledExecutorService scheduler;
    
    // 远程 MD5 列表 URL
    private String md5ListUrl;
    // 刷新间隔（分钟）
    private int refreshInterval;
    
    // 文件名到缓存文件名的映射
    private static final Map<String, String> FILE_MAP = Map.of(
        "keys.cache", "密钥列表",
        "servernames.cache", "服务器名称列表"
    );

    public MD5Validator(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.md5Map = new HashMap<>();
    }

    /**
     * 初始化 MD5 校验提供者
     * @param url MD5 列表 URL
     * @param refreshIntervalMinutes 刷新间隔（分钟）
     */
    public void initialize(String url, int refreshIntervalMinutes) {
        this.md5ListUrl = url;
        this.refreshInterval = refreshIntervalMinutes;
        
        // 立即加载一次
        loadMD5List();
        
        // 设置定时刷新
        if (refreshInterval > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                this::loadMD5List,
                refreshInterval,
                refreshInterval,
                TimeUnit.MINUTES
            );
        }
    }

    /**
     * 从远程 URL 加载 MD5 列表
     */
    private void loadMD5List() {
        if (md5ListUrl == null || md5ListUrl.isEmpty()) {
            plugin.getLogger().warning("[MD5Validator] 校验列表 URL 未配置");
            return;
        }
        
        try {
            URL url = new URL(md5ListUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("[MD5Validator] 获取校验列表失败，HTTP 响应码: " + responseCode);
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
            
            // 解析 MD5 列表
            MD5ListResponse md5List = gson.fromJson(content, MD5ListResponse.class);
            
            if (md5List != null && md5List.md5 != null) {
                md5Map.clear();
                for (MD5Item item : md5List.md5) {
                    if (item.docname != null && item.md5 != null) {
                        md5Map.put(item.docname, item.md5);
                    }
                }
                
                plugin.getLogger().info("[MD5Validator] 成功加载 " + md5Map.size() + " 个校验信息");
                
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("[MD5Validator] 校验列表: " + md5Map);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[MD5Validator] 加载 MD5 列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的校验值
     * @param filename 文件名
     * @return 校验值，如果不存在则返回 null
     */
    public String getMD5(String filename) {
        return md5Map.get(filename);
    }
    
    /**
     * 检查 MD5 列表是否已加载
     * @return 是否已加载
     */
    public boolean isLoaded() {
        return !md5Map.isEmpty();
    }

    /**
     * 检查文件是否在 MD5 列表中
     * @param filename 文件名
     * @return 是否存在
     */
    public boolean hasMD5(String filename) {
        return md5Map.containsKey(filename);
    }

    /**
     * 验证文件的校验值是否匹配
     * @param filename 文件名
     * @param expectedMD5 期望的校验值
     * @return 是否匹配
     */
    public boolean validateMD5(String filename, String expectedMD5) {
        String remoteMD5 = getMD5(filename);
        if (remoteMD5 == null) {
            plugin.getLogger().warning("[MD5Validator] 文件 " + filename + " 不在远程校验列表中");
            return false;
        }
        
        boolean matches = remoteMD5.equals(expectedMD5);
        if (!matches) {
            plugin.getLogger().warning("[MD5Validator] 文件 " + filename + " 的校验不匹配");
        }
        
        return matches;
    }

    /**
     * 手动刷新校验列表
     */
    public void refresh() {
        loadMD5List();
    }

    /**
     * 关闭 MD5 校验提供者
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * 校验列表响应结构
     */
    private static class MD5ListResponse {
        Set<MD5Item> md5;
    }

    /**
     * 校验项结构
     */
    private static class MD5Item {
        String docname;
        String md5;
    }
}