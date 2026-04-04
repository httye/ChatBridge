package com.chatbridge.model;

/**
 * 服务器状态消息数据模型
 */
public class ServerStatusMessage {

    private String serverName;
    private String status; // "start" 或 "stop"
    private long timestamp;

    public ServerStatusMessage() {
        // 用于JSON反序列化
    }

    public ServerStatusMessage(String serverName, String status, long timestamp) {
        this.serverName = serverName;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters
    public String getServerName() {
        return serverName;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}