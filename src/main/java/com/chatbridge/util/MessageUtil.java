package com.chatbridge.util;

import com.chatbridge.config.ConfigManager;
import com.chatbridge.model.ChatMessage;
import com.chatbridge.model.PlayerJoinMessage;
import com.chatbridge.model.PlayerQuitMessage;
import com.chatbridge.model.ServerStatusMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 消息工具类
 * 用于格式化各种消息
 */
public class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();

    /**
     * 格式化聊天消息
     */
    public static Component formatChatMessage(ChatMessage message, ConfigManager config) {
        String format = config.getChatFormat();
        
        // 替换变量
        format = format.replace("{server}", message.getServerDisplayName());
        format = format.replace("{player}", message.getPlayerDisplayName());
        format = format.replace("{message}", message.getMessage());
        format = format.replace("{display_name}", message.getPlayerDisplayName());
        format = format.replace("{prefix}", message.getPrefix());
        format = format.replace("{suffix}", message.getSuffix());
        
        // 处理服务器前缀
        String serverPrefix = "";
        if (message.isServerPrefixEnabled() && message.getServerPrefix() != null) {
            serverPrefix = message.getServerPrefix();
        }
        
        // 根据前缀位置处理
        if ("before_name".equals(message.getServerPrefixPosition())) {
            // 前缀在玩家名字前
            format = format.replace("{player}", serverPrefix + " " + message.getPlayerDisplayName());
        } else if ("before_message".equals(message.getServerPrefixPosition())) {
            // 前缀在消息前
            format = format.replace("{message}", serverPrefix + " " + message.getMessage());
        }
        
        // 添加服务器前缀变量
        format = format.replace("{server_prefix}", serverPrefix);
        
        return SERIALIZER.deserialize(format);
    }

    /**
     * 格式化玩家加入消息
     */
    public static Component formatJoinMessage(PlayerJoinMessage message, ConfigManager config) {
        String format = config.getJoinFormat();
        
        // 替换变量
        format = format.replace("{server}", message.getServerDisplayName());
        format = format.replace("{player}", message.getPlayerDisplayName());
        format = format.replace("{display_name}", message.getPlayerDisplayName());
        
        return SERIALIZER.deserialize(format);
    }

    /**
     * 格式化玩家退出消息
     */
    public static Component formatQuitMessage(PlayerQuitMessage message, ConfigManager config) {
        String format = config.getQuitFormat();
        
        // 替换变量
        format = format.replace("{server}", message.getServerDisplayName());
        format = format.replace("{player}", message.getPlayerDisplayName());
        format = format.replace("{display_name}", message.getPlayerDisplayName());
        format = format.replace("{reason}", message.getReason() != null ? message.getReason() : "");
        
        return SERIALIZER.deserialize(format);
    }

    /**
     * 格式化服务器状态消息
     */
    public static Component formatStatusMessage(ServerStatusMessage message, ConfigManager config) {
        String format;
        
        if ("start".equals(message.getStatus())) {
            format = config.getServerStartFormat();
        } else {
            format = config.getServerStopFormat();
        }
        
        // 替换变量
        format = format.replace("{server}", message.getServerName());
        
        return SERIALIZER.deserialize(format);
    }

    /**
     * 将颜色代码转换为Component
     */
    public static Component toComponent(String text) {
        return SERIALIZER.deserialize(text);
    }

    /**
     * 将Component转换为字符串
     */
    public static String toString(Component component) {
        return SERIALIZER.serialize(component);
    }
}