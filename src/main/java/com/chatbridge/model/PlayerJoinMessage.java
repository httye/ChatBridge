package com.chatbridge.model;

/**
 * 玩家加入消息数据模型
 */
public class PlayerJoinMessage {

    private String serverName;
    private String serverDisplayName;
    private String playerName;
    private String playerDisplayName;
    private long timestamp;

    public PlayerJoinMessage() {
        // 用于JSON反序列化
    }

    public PlayerJoinMessage(String serverName, String serverDisplayName,
                             String playerName, String playerDisplayName) {
        this.serverName = serverName;
        this.serverDisplayName = serverDisplayName;
        this.playerName = playerName;
        this.playerDisplayName = playerDisplayName;
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

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}