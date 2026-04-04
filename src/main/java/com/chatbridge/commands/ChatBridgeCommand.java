package com.chatbridge.commands;

import com.chatbridge.ChatBridgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ChatBridge主命令处理器
 */
public class ChatBridgeCommand implements CommandExecutor, TabCompleter {

    private final ChatBridgePlugin plugin;

    public ChatBridgeCommand(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "status":
                return handleStatus(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(Component.text("未知的子命令! 使用 /chatbridge help 查看帮助")
                    .color(NamedTextColor.RED));
                return true;
        }
    }

    /**
     * 处理重载命令
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("chatbridge.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令!")
                .color(NamedTextColor.RED));
            return true;
        }

        plugin.reload();
        sender.sendMessage(Component.text("ChatBridge 配置已重新加载!")
            .color(NamedTextColor.GREEN));
        return true;
    }

    /**
     * 处理状态命令
     */
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("chatbridge.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令!")
                .color(NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("========== ChatBridge 状态 ==========")
            .color(NamedTextColor.GOLD));
        
        sender.sendMessage(Component.text("服务器名称: ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(plugin.getConfigManager().getServerName())
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("Redis连接: ")
            .color(NamedTextColor.YELLOW)
            .append(plugin.getRedisManager().isConnected() 
                ? Component.text("已连接").color(NamedTextColor.GREEN)
                : Component.text("未连接").color(NamedTextColor.RED)));
        
        sender.sendMessage(Component.text("聊天同步: ")
            .color(NamedTextColor.YELLOW)
            .append(plugin.getConfigManager().isChatEnabled()
                ? Component.text("已启用").color(NamedTextColor.GREEN)
                : Component.text("已禁用").color(NamedTextColor.RED)));
        
        sender.sendMessage(Component.text("加入/退出同步: ")
            .color(NamedTextColor.YELLOW)
            .append(plugin.getConfigManager().isSyncJoinQuit()
                ? Component.text("已启用").color(NamedTextColor.GREEN)
                : Component.text("已禁用").color(NamedTextColor.RED)));

        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== ChatBridge 帮助 ==========")
            .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/chatbridge reload")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重载配置文件").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/chatbridge status")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看插件状态").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/globalchat <消息>")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - 发送全局消息").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/togglechat")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - 切换全局聊天显示").color(NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "status", "help");
            List<String> completions = new ArrayList<>();
            
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(args[0].toLowerCase())) {
                    if (subCmd.equals("reload") || subCmd.equals("status")) {
                        if (sender.hasPermission("chatbridge.admin")) {
                            completions.add(subCmd);
                        }
                    } else {
                        completions.add(subCmd);
                    }
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}