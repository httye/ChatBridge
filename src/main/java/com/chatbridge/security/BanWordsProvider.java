package com.chatbridge.security;

import com.chatbridge.ChatBridgePlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 违禁词提供者
 * 从远程URL获取违禁词列表
 */
public class BanWordsProvider {

    private final ChatBridgePlugin plugin;
    private final Set<String> banWords;
    private ScheduledExecutorService scheduler;
    
    // 远程违禁词列表URL
    private String banWordsUrl;
    // 刷新间隔（分钟）
    private int refreshInterval;
    
    // 本地缓存文件
    private File cacheFile;
    private File md5File;
    
    // 文件名（用于 MD5 校验）
    private static final String CACHE_FILENAME = "banwords.cache";
    
    // 强制验证标志
    private boolean forceValidation = true;

    public BanWordsProvider(ChatBridgePlugin plugin) {
        this.plugin = plugin;
        this.banWords = new HashSet<>();
        
        // 初始化本地缓存文件
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        cacheFile = new File(dataFolder, "banwords.cache");
        md5File = new File(dataFolder, "banwords.md5");
    }

    /**
     * 初始化违禁词提供者
     * @param url 违禁词列表URL
     * @param refreshIntervalMinutes 刷新间隔（分钟）
     */
    public void initialize(String url, int refreshIntervalMinutes) {
        this.banWordsUrl = url;
        this.refreshInterval = refreshIntervalMinutes;
        
        // 立即加载一次
        loadBanWords();
        
        // 设置定时刷新
        if (refreshInterval > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                this::loadBanWords,
                refreshInterval,
                refreshInterval,
                TimeUnit.MINUTES
            );
        }
    }

    /**
     * 从远程URL加载违禁词列表
     */
    private void loadBanWords() {
        if (banWordsUrl == null || banWordsUrl.isEmpty()) {
            plugin.getLogger().warning("[BanWords] 违禁词列表URL未配置");
            return;
        }
        
        try {
            URL url = new URL(banWordsUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("[BanWords] 获取违禁词列表失败，HTTP响应码: " + responseCode);
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
            String contentMd5 = calculateMD5(content);
            
            // 检查是否需要更新
            boolean shouldUpdate = needUpdate(contentMd5);
            
            // 如果是强制验证模式，总是重新验证
            if (forceValidation) {
                shouldUpdate = true;
                forceValidation = false;
            }
            
            if (shouldUpdate) {
                // 保存到本地文件
                saveToCache(content, contentMd5);
                
                // 解析违禁词列表
                Set<String> newWords = parseBanWords(content);
                
                if (!newWords.isEmpty()) {
                    banWords.clear();
                    banWords.addAll(newWords);
                    plugin.getLogger().info("[BanWords] 成功加载 " + banWords.size() + " 个违禁词");
                }
            } else {
                // 校验失败，清空违禁词列表
                banWords.clear();
                plugin.getLogger().severe("[BanWords] 违禁词列表校验失败，已禁用违禁词过滤功能！");
                plugin.getLogger().severe("[BanWords] 原因：数据完整性验证未通过，可能是数据被篡改或来源不合法");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[BanWords] 加载违禁词列表失败: " + e.getMessage());
            // 发生错误时也清空违禁词列表
            banWords.clear();
        }
    }

    /**
     * 检查是否需要更新
     * 使用远程 MD5 校验器验证本地缓存
     * @param contentMd5 计算的内容MD5值
     * @return 是否需要更新（true=使用新数据，false=拒绝使用）
     */
    private boolean needUpdate(String contentMd5) {
        try {
            // 如果本地没有文件，需要下载
            if (!md5File.exists() || !cacheFile.exists()) {
                // 强制验证模式下，新数据必须通过MD5校验
                if (forceValidation) {
                    MD5Validator md5Validator = plugin.getConfigManager().getMD5Validator();
                    if (md5Validator != null && md5Validator.hasMD5(CACHE_FILENAME)) {
                        boolean matches = md5Validator.validateMD5(CACHE_FILENAME, contentMd5);
                        if (!matches) {
                            plugin.getLogger().warning("[BanWords] 强制校验失败，拒绝使用新下载的违禁词列表");
                            return false;
                        }
                    }
                }
                return true;
            }
            
            // 使用远程 MD5 校验器验证
            MD5Validator md5Validator = plugin.getConfigManager().getMD5Validator();
            if (md5Validator != null && md5Validator.hasMD5(CACHE_FILENAME)) {
                // 使用远程 MD5 进行校验
                boolean matches = md5Validator.validateMD5(CACHE_FILENAME, contentMd5);
                if (!matches) {
                    plugin.getLogger().warning("[BanWords] 违禁词列表校验失败，数据完整性验证未通过");
                    return false;
                }
                // 校验通过，可以使用缓存数据
                return false;
            }
            
            // 如果没有远程 MD5，使用本地 MD5 文件
            String localMd5 = new String(Files.readAllBytes(md5File.toPath()));
            return !contentMd5.equals(localMd5);
        } catch (Exception e) {
            plugin.getLogger().severe("[BanWords] 校验过程发生错误: " + e.getMessage());
            // 发生错误时拒绝使用
            return false;
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
            plugin.getLogger().warning("[BanWords] 保存本地缓存失败: " + e.getMessage());
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
     * 解析违禁词列表
     * 支持分号分隔格式：违禁词1；违禁词2；违禁词3
     */
    private Set<String> parseBanWords(String content) {
        Set<String> words = new HashSet<>();
        
        try {
            // 按分号分隔
            String[] parts = content.split("；");
            for (String word : parts) {
                word = word.trim();
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BanWords] 解析违禁词列表失败: " + e.getMessage());
        }
        
        return words;
    }

    /**
     * 过滤消息中的违禁词
     * @param message 原始消息
     * @param replacement 替换字符
     * @return 过滤后的消息
     */
    public String filterMessage(String message, String replacement) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 如果违禁词列表为空或不可用，直接返回原消息
        if (banWords.isEmpty()) {
            return message;
        }

        String filtered = message;
        for (String word : banWords) {
            if (word != null && !word.isEmpty()) {
                filtered = filtered.replace(word, replacement);
            }
        }
        return filtered;
    }

    /**
     * 检查消息是否包含违禁词
     * @param message 消息
     * @return 是否包含违禁词
     */
    public boolean containsBanWord(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // 如果违禁词列表为空或不可用，返回false
        if (banWords.isEmpty()) {
            return false;
        }

        for (String word : banWords) {
            if (word != null && !word.isEmpty() && message.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有违禁词
     * @return 违禁词集合
     */
    public Set<String> getBanWords() {
        return new HashSet<>(banWords);
    }

    /**
     * 手动刷新违禁词列表（强制重新下载并验证）
     */
    public void refresh() {
        forceValidation = true;
        loadBanWords();
    }

    /**
     * 关闭违禁词提供者
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
