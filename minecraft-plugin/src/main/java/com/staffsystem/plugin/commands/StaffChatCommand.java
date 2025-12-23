package com.staffsystem.plugin.commands;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor {

    private final StaffSystemPlugin plugin;

    public StaffChatCommand(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission("staffsystem.staffchat")) {
            staff.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            // Toggle staff chat
            plugin.getStaffModeManager().toggleStaffChat(staff);
        } else {
            // Send message to staff chat
            StringBuilder message = new StringBuilder();
            for (String arg : args) {
                if (message.length() > 0) {
                    message.append(" ");
                }
                message.append(arg);
            }
            plugin.getStaffModeManager().sendStaffChat(staff, message.toString());
        }

        return true;
    }
}
