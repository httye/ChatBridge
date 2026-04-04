package com.chatbridge.model;

/**
 * 聊天消息数据模型
 */
public class ChatMessage {

    private String serverKey;
    private String serverName;
    private String serverDisplayName;
    private String serverPrefix;
    private boolean serverPrefixEnabled;
    private String serverPrefixPosition;
    private String playerName;
    private String playerDisplayName;
    private String message;
    private String channel;
    private long timestamp;
    private String prefix;
    private String suffix;

    public ChatMessage() {
        // 用于JSON反序列化
    }

    public ChatMessage(String serverKey, String serverName, String serverDisplayName,
                       String serverPrefix, boolean serverPrefixEnabled, String serverPrefixPosition,
                       String playerName, String playerDisplayName,
                       String message, String channel,
                       String prefix, String suffix) {
        this.serverKey = serverKey;
        this.serverName = serverName;
        this.serverDisplayName = serverDisplayName;
        this.serverPrefix = serverPrefix != null ? serverPrefix : "";
        this.serverPrefixEnabled = serverPrefixEnabled;
        this.serverPrefixPosition = serverPrefixPosition != null ? serverPrefixPosition : "before_name";
        this.playerName = playerName;
        this.playerDisplayName = playerDisplayName;
        this.message = message;
        this.channel = channel;
        this.timestamp = System.currentTimeMillis();
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
    }

    // Getters
    public String getServerKey() {
        return serverKey;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public String getServerPrefix() {
        return serverPrefix;
    }

    public boolean isServerPrefixEnabled() {
        return serverPrefixEnabled;
    }

    public String getServerPrefixPosition() {
        return serverPrefixPosition;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerDisplayName() {
        return playerDisplayName;
    }

    public String getMessage() {
        return message;
    }

    public String getChannel() {
        return channel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    // Setters
    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setServerDisplayName(String serverDisplayName) {
        this.serverDisplayName = serverDisplayName;
    }

    public void setServerPrefix(String serverPrefix) {
        this.serverPrefix = serverPrefix;
    }

    public void setServerPrefixEnabled(boolean serverPrefixEnabled) {
        this.serverPrefixEnabled = serverPrefixEnabled;
    }

    public void setServerPrefixPosition(String serverPrefixPosition) {
        this.serverPrefixPosition = serverPrefixPosition;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerDisplayName(String playerDisplayName) {
        this.playerDisplayName = playerDisplayName;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}