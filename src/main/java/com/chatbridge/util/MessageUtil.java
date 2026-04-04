package com.chatbridge.util;

import com.chatbridge.config.ConfigManager;
import com.chatbridge.model.ChatMessage;
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
        // 基础格式: 前缀 + 玩家名 + 消息
        String format = "&r{prefix}{player}&r: &f{message}";
        
        // 替换变量
        format = format.replace("{server}", message.getServerDisplayName());
        format = format.replace("{player}", message.getPlayerDisplayName());
        format = format.replace("{message}", message.getMessage());
        format = format.replace("{display_name}", message.getPlayerDisplayName());
        format = format.replace("{prefix}", message.getPrefix());
        format = format.replace("{suffix}", message.getSuffix());
        
        // 处理服务器前缀
        if (message.isServerPrefixEnabled() && message.getServerPrefix() != null && !message.getServerPrefix().isEmpty()) {
            String serverPrefix = message.getServerPrefix();
            String position = message.getServerPrefixPosition();
            
            if ("before_message".equals(position)) {
                // 前缀放在消息前
                format = serverPrefix + " " + format;
            } else {
                // 默认: before_name - 前缀放在玩家名前
                format = format.replace("{player}", serverPrefix + " " + message.getPlayerDisplayName());
            }
        }
        
        return SERIALIZER.deserialize(format);
    }

    /**
     * 格式化服务器状态消息
     */
    public static Component formatStatusMessage(ServerStatusMessage message, ConfigManager config) {
        // 使用服务器前缀格式
        String serverPrefix = config.getFormattedServerPrefix();
        String format;
        
        if ("start".equals(message.getStatus())) {
            format = serverPrefix + " &a服务器已启动";
        } else {
            format = serverPrefix + " &c服务器已关闭";
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
