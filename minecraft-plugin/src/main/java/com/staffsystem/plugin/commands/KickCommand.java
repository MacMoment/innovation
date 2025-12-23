package com.staffsystem.plugin.commands;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickCommand implements CommandExecutor {

    private final StaffSystemPlugin plugin;

    public KickCommand(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission("staffsystem.kick")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("kick.usage")));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("player-offline")));
            return true;
        }

        // Check if target is staff
        if (target.hasPermission("staffsystem.staff") && !staff.hasPermission("staffsystem.admin")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-staff")));
            return true;
        }

        // Check self-punishment
        if (target.getUniqueId().equals(staff.getUniqueId())) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-self")));
            return true;
        }

        // Build reason
        String reason = buildReason(args, 1, 
            plugin.getConfig().getString("punishments.default-reasons.kick", "Kicked from server"));

        plugin.getPunishmentManager().kick(target, staff, reason)
            .thenAccept(success -> {
                if (success) {
                    staff.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("kick.success")
                            .replace("{player}", targetName)));
                }
            });

        return true;
    }

    private String buildReason(String[] args, int startIndex, String defaultReason) {
        if (args.length <= startIndex) {
            return defaultReason;
        }
        
        StringBuilder reason = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (reason.length() > 0) {
                reason.append(" ");
            }
            reason.append(args[i]);
        }
        return reason.toString();
    }
}
