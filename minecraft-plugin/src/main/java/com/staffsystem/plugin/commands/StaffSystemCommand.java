package com.staffsystem.plugin.commands;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffSystemCommand implements CommandExecutor {

    private final StaffSystemPlugin plugin;

    public StaffSystemCommand(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("staffsystem.admin")) {
            sender.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("config-reloaded")));
            }
            case "info" -> sendInfo(sender);
            default -> {
                sender.sendMessage(plugin.getMessageUtil().color("&cUnknown subcommand. Use /ss reload or /ss info"));
            }
        }

        return true;
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(plugin.getMessageUtil().color("&8&m----------------------------------------"));
        sender.sendMessage(plugin.getMessageUtil().color("&6&lStaffSystem &7- Staff Management Plugin"));
        sender.sendMessage(plugin.getMessageUtil().color("&7Version: &e" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getMessageUtil().color("&7Author: &eStaffSystem Team"));
        sender.sendMessage(plugin.getMessageUtil().color(""));
        sender.sendMessage(plugin.getMessageUtil().color("&7Commands:"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/staff &7- Open staff GUI"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/ban &7- Ban a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/tempban &7- Temporarily ban a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/unban &7- Unban a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/mute &7- Mute a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/tempmute &7- Temporarily mute a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/unmute &7- Unmute a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/kick &7- Kick a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/warn &7- Warn a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/freeze &7- Freeze a player"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/history &7- View punishment history"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/staffchat &7- Toggle staff chat"));
        sender.sendMessage(plugin.getMessageUtil().color("&e/ss reload &7- Reload configuration"));
        sender.sendMessage(plugin.getMessageUtil().color("&8&m----------------------------------------"));
    }
}
