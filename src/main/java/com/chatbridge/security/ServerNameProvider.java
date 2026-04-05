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
 * 服务器名称提供者
 * 从远程URL获取允许的服务器名称列表
 * 插件只允许配置的服务器名称接入服务
 * 使用校验机制确保数据完整性，每次启动和 reload 时强制校验
 */
public class ServerNameProvider {

    private final ChatBridgePlugin plugin;
    private final Gson gson;
    private final Set<String> validServerNames;
    private ScheduledExecutorService scheduler;
    
    // 远程服务器名称列表URL
    private String serverNameListUrl;
    // 刷新间隔（分钟）
    private int refreshInterval;
    
    // 本地缓存文件
    private File cacheFile;
    private File md5File;
    
    // 文件名（用于 MD5 校验）
    private static final String CACHE_FILENAME = "servernames.cache";
    
    // 强制验证标志
    private boolean forceValidation = true;

    public ServerNameProvider(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.validServerNames = new HashSet<>();
        
        // 初始化本地缓存文件
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        cacheFile = new File(dataFolder, CACHE_FILENAME);
        md5File = new File(dataFolder, "servernames.md5");
    }

    /**
     * 初始化服务器名称提供者
     * @param url 服务器名称列表URL
     * @param refreshIntervalMinutes 刷新间隔（分钟）
     */
    public void initialize(String url, int refreshIntervalMinutes) {
        this.serverNameListUrl = url;
        this.refreshInterval = refreshIntervalMinutes;
        
        // 立即加载一次（会校验 MD5）
        loadServerNames();
        
        // 设置定时刷新
        if (refreshInterval > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                this::loadServerNames,
                refreshInterval,
                refreshInterval,
                TimeUnit.MINUTES
            );
        }
    }

    /**
     * 从远程URL加载服务器名称列表
     * 每次都会校验 MD5，如果不匹配则重新下载
     */
    private void loadServerNames() {
        if (serverNameListUrl == null || serverNameListUrl.isEmpty()) {
            plugin.getLogger().warning("[ServerNameProvider] 服务器名称列表URL未配置");
            return;
        }
        
        try {
            URL url = new URL(serverNameListUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("[ServerNameProvider] 获取服务器名称列表失败，HTTP响应码: " + responseCode);
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
            boolean shouldUpdate = needUpdate(newMd5);
            
            // 如果是强制验证模式，总是重新验证
            if (forceValidation) {
                shouldUpdate = true;
                forceValidation = false;
            }
            
            if (shouldUpdate) {
                // 保存到本地文件
                saveToCache(content, newMd5);
                
                // 解析服务器名称列表
                Set<String> newNames = parseServerNames(content);
                
                if (!newNames.isEmpty()) {
                    validServerNames.clear();
                    validServerNames.addAll(newNames);
                    plugin.getLogger().info("[ServerNameProvider] 成功加载 " + validServerNames.size() + " 个允许的服务器名称");
                    
                    // 仅在 debug 模式下显示详细信息
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("[ServerNameProvider] 服务器名称列表已验证");
                    }
                }
            } else {
                // 校验通过，从本地缓存加载
                if (!validServerNames.isEmpty()) {
                    plugin.getLogger().info("[ServerNameProvider] 使用缓存服务器名称列表（" + validServerNames.size() + " 个）");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[ServerNameProvider] 加载服务器名称列表失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否需要更新
     * 使用远程 MD5 校验器验证本地缓存
     * @param contentMd5 计算的内容MD5值
     * @return 是否需要更新
     */
    private boolean needUpdate(String contentMd5) {
        try {
            // 如果本地没有文件，需要下载
            if (!md5File.exists() || !cacheFile.exists()) {
                return true;
            }
            
            // 使用远程校验器验证
            MD5Validator md5Validator = plugin.getConfigManager().getMD5Validator();
            if (md5Validator != null && md5Validator.hasMD5(CACHE_FILENAME)) {
                // 使用远程校验进行验证
                boolean matches = md5Validator.validateMD5(CACHE_FILENAME, contentMd5);
                if (!matches && plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("[ServerNameProvider] 校验失败，将重新下载");
                }
                return !matches;
            }
            
            // 如果没有远程 MD5，使用本地 MD5 文件
            String localMd5 = new String(Files.readAllBytes(md5File.toPath()));
            return !contentMd5.equals(localMd5);
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
            plugin.getLogger().warning("[ServerNameProvider] 保存本地缓存失败: " + e.getMessage());
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
     * 解析服务器名称列表JSON
     * 支持三种格式：
     * 1. JSON数组: ["Server1", "Server2", "Server3"]
     * 2. JSON对象: {"server_names": ["Server1", "Server2"], "version": "1.0"}
     * 3. 新格式: {"names": [{"name": "servername", "created_at": "2023-01-01"}, ...]}
     */
    private Set<String> parseServerNames(String json) {
        Set<String> serverNames = new HashSet<>();
        
        try {
            // 尝试解析为数组
            if (json.trim().startsWith("[")) {
                Set<String> arrayNames = gson.fromJson(json, new TypeToken<Set<String>>(){}.getType());
                if (arrayNames != null) {
                    serverNames.addAll(arrayNames);
                }
            } else {
                // 尝试解析为对象
                ServerNameListResponse response = gson.fromJson(json, ServerNameListResponse.class);
                if (response != null) {
                    // 优先检查新格式的 names 字段
                    if (response.names != null && !response.names.isEmpty()) {
                        Object firstItem = response.names.iterator().next();
                        if (firstItem instanceof String) {
                            // 字符串集合格式
                            for (Object obj : response.names) {
                                if (obj instanceof String) {
                                    serverNames.add((String) obj);
                                }
                            }
                        } else {
                            // 对象集合格式，需要提取 name 字段
                            for (Object obj : response.names) {
                                if (obj instanceof NameItem) {
                                    serverNames.add(((NameItem) obj).name);
                                }
                            }
                        }
                    }
                    // 如果 names 为空，检查旧格式的 serverNames 字段
                    else if (response.serverNames != null) {
                        serverNames.addAll(response.serverNames);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ServerNameProvider] 解析服务器名称列表失败: " + e.getMessage());
        }
        
        return serverNames;
    }

    /**
     * 验证服务器名称是否有效
     * @param serverName 要验证的服务器名称
     * @return 是否有效
     */
    public boolean isValidServerName(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return false;
        }
        return validServerNames.contains(serverName);
    }

    /**
     * 获取所有有效的服务器名称
     * @return 服务器名称集合
     */
    public Set<String> getValidServerNames() {
        return new HashSet<>(validServerNames);
    }

    /**
     * 手动刷新服务器名称列表（强制重新下载并验证）
     */
    public void refresh() {
        forceValidation = true;
        loadServerNames();
    }

    /**
     * 关闭服务器名称提供者
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * 服务器名称列表响应结构
     */
    private static class ServerNameListResponse {
        Set<Object> names;
        Set<String> serverNames;
        String version;
    }
    
    /**
     * 名称项结构
     */
    private static class NameItem {
        String name;
        String created_at;
    }
}