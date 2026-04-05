package com.chatbridge.util;

import com.chatbridge.ChatBridgePlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 更新检查器
 * 检查 GitHub Release 上的最新版本
 */
public class UpdateChecker {

    private final ChatBridgePlugin plugin;
    private final String currentVersion;
    private final String repoOwner;
    private final String repoName;
    private String latestVersion;
    private String downloadUrl;
    private String releaseNotes;
    private boolean updateAvailable;
    private boolean checked;

    public UpdateChecker(ChatBridgePlugin plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.updateAvailable = false;
        this.checked = false;
    }

    /**
     * 检查更新
     * @return 是否有新版本可用
     */
    public boolean checkUpdate() {
        if (checked) {
            return updateAvailable;
        }

        try {
            String apiUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "ChatBridge-UpdateChecker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("[UpdateChecker] 获取更新信息失败，HTTP响应码: " + responseCode);
                checked = true;
                return false;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
            
            if (jsonObject.has("tag_name")) {
                latestVersion = jsonObject.get("tag_name").getAsString();
                // 移除 'v' 前缀
                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }
                
                if (jsonObject.has("html_url")) {
                    downloadUrl = jsonObject.get("html_url").getAsString();
                }
                
                if (jsonObject.has("body")) {
                    releaseNotes = jsonObject.get("body").getAsString();
                }
                
                // 比较版本号
                updateAvailable = compareVersions(currentVersion, latestVersion) < 0;
                
                if (updateAvailable) {
                    plugin.getLogger().info("§a[UpdateChecker] 发现新版本: " + latestVersion + " (当前版本: " + currentVersion + ")");
                    plugin.getLogger().info("§7[UpdateChecker] 下载地址: " + downloadUrl);
                } else {
                    plugin.getLogger().info("§a[UpdateChecker] 当前已是最新版本: " + currentVersion);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[UpdateChecker] 检查更新失败: " + e.getMessage());
        }
        
        checked = true;
        return updateAvailable;
    }

    /**
     * 比较两个版本号
     * @param v1 版本1
     * @param v2 版本2
     * @return 负数表示 v1 < v2，0 表示相等，正数表示 v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        
        return 0;
    }

    /**
     * 解析版本号部分
     * @param part 版本号部分
     * @return 数字值
     */
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 是否有新版本可用
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * 获取当前版本
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * 获取最新版本
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * 获取下载地址
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * 获取更新说明
     */
    public String getReleaseNotes() {
        return releaseNotes;
    }

    /**
     * 是否已检查过更新
     */
    public boolean isChecked() {
        return checked;
    }
}
