package com.staffsystem.plugin.listeners;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final StaffSystemPlugin plugin;

    public PlayerJoinListener(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        
        // Check for active ban (synchronously check database)
        plugin.getPunishmentManager().getActiveBan(player.getUniqueId())
            .thenAccept(ban -> {
                if (ban != null && !ban.isExpired()) {
                    String kickMessage = formatBanMessage(ban);
                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMessage);
                }
            }).join(); // Block until check completes
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load mute cache for this player
        plugin.getPunishmentManager().loadMuteCache(player.getUniqueId());
        
        // Check for active mute and notify
        plugin.getPunishmentManager().getActiveMute(player.getUniqueId())
            .thenAccept(mute -> {
                if (mute != null && !mute.isExpired()) {
                    player.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("mute.player-notify")));
                    player.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("mute.player-reason")
                            .replace("{reason}", mute.getReason())));
                    if (!mute.isPermanent()) {
                        player.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("mute.player-duration")
                                .replace("{duration}", formatRemainingTime(mute.getRemainingTime()))));
                    }
                }
            });
    }

    private String formatBanMessage(Punishment ban) {
        String template = plugin.getMessageUtil().getMessage("ban.screen");
        String appealUrl = plugin.getConfig().getString("punishments.appeals.url", "N/A");
        
        return plugin.getMessageUtil().color(template
            .replace("{reason}", ban.getReason())
            .replace("{staff}", ban.getStaffName())
            .replace("{duration}", ban.isPermanent() ? "Permanent" : formatRemainingTime(ban.getRemainingTime()))
            .replace("{expires}", ban.isPermanent() ? "Never" : formatExpiration(ban.getExpiration()))
            .replace("{appeal_url}", appealUrl));
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

    private String formatExpiration(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }
}
