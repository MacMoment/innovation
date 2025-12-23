package com.staffsystem.plugin.commands;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FreezeCommand implements CommandExecutor {

    private final StaffSystemPlugin plugin;

    public FreezeCommand(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission("staffsystem.freeze")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("freeze.usage")));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("player-offline")));
            return true;
        }

        // Check if target has bypass permission
        if (target.hasPermission("staffsystem.bypass.freeze")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-staff")));
            return true;
        }

        // Check self-freeze
        if (target.getUniqueId().equals(staff.getUniqueId())) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("cannot-punish-self")));
            return true;
        }

        boolean wasFrozen = plugin.getFreezeManager().isFrozen(target.getUniqueId());
        plugin.getFreezeManager().toggleFreeze(target, staff);

        if (wasFrozen) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("freeze.unfrozen")
                    .replace("{player}", targetName)));
        } else {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("freeze.frozen")
                    .replace("{player}", targetName)));
        }

        return true;
    }
}
