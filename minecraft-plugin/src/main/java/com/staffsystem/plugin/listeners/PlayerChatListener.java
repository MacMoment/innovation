package com.staffsystem.plugin.listeners;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final StaffSystemPlugin plugin;

    public PlayerChatListener(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has staff chat enabled
        if (plugin.getStaffModeManager().isStaffChatEnabled(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getStaffModeManager().sendStaffChat(player, event.getMessage());
            return;
        }
        
        // Check for mute using cache (non-blocking)
        Punishment mute = plugin.getPunishmentManager().getCachedMute(player.getUniqueId());
        
        if (mute != null && !mute.isExpired()) {
            event.setCancelled(true);
            
            player.sendMessage(plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("mute.attempt-blocked")));
            
            if (!mute.isPermanent()) {
                player.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("mute.attempt-remaining")
                        .replace("{remaining}", formatRemainingTime(mute.getRemainingTime()))));
            }
        }
    }

    private String formatRemainingTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "Expired";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day(s)";
        } else if (hours > 0) {
            return hours + " hour(s)";
        } else if (minutes > 0) {
            return minutes + " minute(s)";
        } else {
            return seconds + " second(s)";
        }
    }
}
