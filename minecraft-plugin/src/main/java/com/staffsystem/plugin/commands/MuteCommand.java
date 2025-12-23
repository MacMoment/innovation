package com.staffsystem.plugin.commands;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.utils.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteCommand implements CommandExecutor {

    private final StaffSystemPlugin plugin;

    public MuteCommand(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "mute" -> handleMute(staff, args);
            case "tempmute" -> handleTempMute(staff, args);
            case "unmute" -> handleUnmute(staff, args);
        }

        return true;
    }

    private void handleMute(Player staff, String[] args) {
        if (!staff.hasPermission("staffsystem.mute")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return;
        }

        if (args.length < 1) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("mute.usage")));
            return;
        }

        String targetName = args[0];
        
        // Check if first arg after name is a duration
        long duration = -1; // Permanent by default
        int reasonStartIndex = 1;
        
        if (args.length > 1) {
            long parsedDuration = TimeUtil.parseDuration(args[1]);
            if (parsedDuration > 0) {
                duration = parsedDuration;
                reasonStartIndex = 2;
            }
        }

        // Build reason
        String reason = buildReason(args, reasonStartIndex, 
            plugin.getConfig().getString("punishments.default-reasons.mute", "Muted"));

        // Find target
        Player target = Bukkit.getPlayer(targetName);
        
        // Check if target is staff
        if (target != null && target.hasPermission("staffsystem.staff") && 
            !staff.hasPermission("staffsystem.admin")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-staff")));
            return;
        }

        // Check self-punishment
        if (target != null && target.getUniqueId().equals(staff.getUniqueId())) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-self")));
            return;
        }

        final long finalDuration = duration;
        final String finalReason = reason;

        if (target != null) {
            // Online player
            plugin.getPunishmentManager().mute(target, staff, finalReason, finalDuration)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("mute.success")
                                .replace("{player}", targetName)));
                    }
                });
        } else {
            // Offline player
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            plugin.getPunishmentManager().muteOffline(offlineTarget, staff, finalReason, finalDuration)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("mute.success")
                                .replace("{player}", targetName)));
                    }
                });
        }
    }

    private void handleTempMute(Player staff, String[] args) {
        if (!staff.hasPermission("staffsystem.tempmute")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return;
        }

        if (args.length < 2) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("tempmute.usage")));
            return;
        }

        String targetName = args[0];
        long duration = TimeUtil.parseDuration(args[1]);
        
        if (duration <= 0) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("invalid-duration")));
            return;
        }

        String reason = buildReason(args, 2, 
            plugin.getConfig().getString("punishments.default-reasons.mute", "Temporarily muted"));

        // Find target
        Player target = Bukkit.getPlayer(targetName);
        
        // Check if target is staff
        if (target != null && target.hasPermission("staffsystem.staff") && 
            !staff.hasPermission("staffsystem.admin")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-staff")));
            return;
        }

        if (target != null) {
            plugin.getPunishmentManager().mute(target, staff, reason, duration)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("mute.success")
                                .replace("{player}", targetName)
                                .replace("{duration}", TimeUtil.formatDuration(duration))));
                    }
                });
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            plugin.getPunishmentManager().muteOffline(offlineTarget, staff, reason, duration)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("mute.success")
                                .replace("{player}", targetName)
                                .replace("{duration}", TimeUtil.formatDuration(duration))));
                    }
                });
        }
    }

    private void handleUnmute(Player staff, String[] args) {
        if (!staff.hasPermission("staffsystem.unmute")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return;
        }

        if (args.length < 1) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("unmute.usage")));
            return;
        }

        String targetName = args[0];

        plugin.getPunishmentManager().unmute(targetName, staff)
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        staff.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("unmute.success")
                                .replace("{player}", targetName)));
                        
                        // Notify player if online
                        Player target = Bukkit.getPlayer(targetName);
                        if (target != null) {
                            target.sendMessage(plugin.getMessageUtil().color(
                                plugin.getMessageUtil().getMessage("unmute.player-notify")));
                        }
                    } else {
                        staff.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("unmute.not-muted")
                                .replace("{player}", targetName)));
                    }
                });
            });
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
