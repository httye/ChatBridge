package com.chatbridge.model;

/**
 * 玩家退出消息数据模型
 */
public class PlayerQuitMessage {

    private String serverName;
    private String serverDisplayName;
    private String playerName;
    private String playerDisplayName;
    private String reason;
    private long timestamp;

    public PlayerQuitMessage() {
        // 用于JSON反序列化
    }

    public PlayerQuitMessage(String serverName, String serverDisplayName,
                             String playerName, String playerDisplayName,
                             String reason) {
        this.serverName = serverName;
        this.serverDisplayName = serverDisplayName;
        this.playerName = playerName;
        this.playerDisplayName = playerDisplayName;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getServerName() {
        return serverName;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerDisplayName() {
        return playerDisplayName;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setServerDisplayName(String serverDisplayName) {
        this.serverDisplayName = serverDisplayName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerDisplayName(String playerDisplayName) {
        this.playerDisplayName = playerDisplayName;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}