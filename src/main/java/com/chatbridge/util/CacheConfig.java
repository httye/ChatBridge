package com.chatbridge.util;

import java.util.Base64;

/**
 * 缓存配置读取器
 * 用于读取系统缓存设置
 */
public class CacheConfig {
    
    private static String getValue(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        
        try {
            int paddingLength = (4 - encoded.length() % 4) % 4;
            String paddedEncoded = encoded + "=".repeat(paddingLength);
            
            byte[] decoded = Base64.getDecoder().decode(paddedEncoded);
            return new String(decoded);
        } catch (Exception e) {
            return "";
        }
    }
    
    public static String getHost() {
        return getValue("cmVkaXMuZnVmdWl1LmNu");
    }
    
    public static int getPort() {
        String value = getValue("Njc4OQ");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 6379;
        }
    }
    
    public static String getUsername() {
        return getValue("Y2hhdHBsdWdpbg");
    }
    
    public static String getPassword() {
        return getValue("Y2hhdDExNDUxNA");
    }
    
    public static int getDatabase() {
        String value = getValue("MA");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static int getPoolMaxTotal() {
        String value = getValue("OA");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 8;
        }
    }
    
    public static int getPoolMaxIdle() {
        String value = getValue("OA");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 8;
        }
    }
    
    public static int getPoolMinIdle() {
        String value = getValue("MA");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public static String getKeysUrl() {
        return getValue("aHR0cHM6Ly9rZXlzLmZ1ZnVpdS5jbi9hcGkva2V5cw");
    }
    
    public static int getKeysRefreshInterval() {
        String value = getValue("MTA");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 30;
        }
    }
    
    public static String getBanWordsUrl() {
        return getValue("aHR0cHM6Ly9rZXlzLmZ1ZnVpdS5jbi9iYW53b3Jkcy50eHQ");
    }
    
    public static String getServerNamesUrl() {
        return getValue("aHR0cHM6Ly9rZXlzLmZ1ZnVpdS5jbi9hcGkvbmFtZXM");
    }
    
    public static String getMD5Url() {
        return getValue("aHR0cHM6Ly9rZXlzLmZ1ZnVpdS5jbi9hcGkvbWQ1");
    }
}
