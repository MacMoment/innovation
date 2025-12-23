package com.staffsystem.plugin.commands;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;
import com.staffsystem.plugin.utils.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HistoryCommand implements CommandExecutor {

    private final StaffSystemPlugin plugin;

    public HistoryCommand(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission("staffsystem.history")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("history.usage")));
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        plugin.getDatabaseManager().getPunishmentHistory(target.getUniqueId())
            .thenAccept(punishments -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    displayHistory(staff, target, punishments);
                });
            });

        return true;
    }

    private void displayHistory(Player staff, OfflinePlayer target, List<Punishment> punishments) {
        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        
        // Send header
        staff.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("history.header")
                .replace("{player}", targetName)));

        if (punishments.isEmpty()) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("history.no-history")));
        } else {
            for (Punishment punishment : punishments) {
                String typeName = punishment.getType().name().replace("_", " ");
                String status = punishment.isActive() ? "&aActive" : "&cInactive";
                if (punishment.isExpired()) {
                    status = "&7Expired";
                }

                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("history.entry")
                        .replace("{id}", String.valueOf(punishment.getId()))
                        .replace("{type}", typeName)
                        .replace("{reason}", punishment.getReason())));
                
                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("history.entry-date")
                        .replace("{date}", TimeUtil.formatDate(punishment.getTimestamp()))));
                
                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("history.entry-staff")
                        .replace("{staff}", punishment.getStaffName())));
                
                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("history.entry-duration")
                        .replace("{duration}", TimeUtil.formatDuration(punishment.getDuration()))));
                
                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("history.entry-active")
                        .replace("{status}", status)));
            }
        }

        // Send footer
        staff.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("history.footer")));
    }
}
