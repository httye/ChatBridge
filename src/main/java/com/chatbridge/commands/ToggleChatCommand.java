package com.chatbridge.commands;

import com.chatbridge.ChatBridgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 切换聊天显示命令处理器
 */
public class ToggleChatCommand implements CommandExecutor {

    private final ChatBridgePlugin plugin;

    public ToggleChatCommand(ChatBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbridge.toggle")) {
            sender.sendMessage(Component.text("你没有权限执行此命令!")
                .color(NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行!")
                .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        boolean newState = plugin.toggleChat(player.getUniqueId());

        if (newState) {
            player.sendMessage(Component.text("全局聊天已启用!")
                .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("全局聊天已禁用!")
                .color(NamedTextColor.RED));
        }

        return true;
    }
}