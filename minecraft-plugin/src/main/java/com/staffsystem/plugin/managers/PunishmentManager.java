package com.staffsystem.plugin.managers;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;
import com.staffsystem.plugin.models.Punishment.PunishmentType;
import com.staffsystem.plugin.utils.TimeUtil;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final StaffSystemPlugin plugin;
    // Cache for active mutes to avoid blocking database calls in chat events
    private final Map<UUID, Punishment> muteCache = new ConcurrentHashMap<>();

    public PunishmentManager(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get cached mute status for a player (non-blocking).
     * Returns null if player is not muted or mute has expired.
     */
    public Punishment getCachedMute(UUID playerUuid) {
        Punishment mute = muteCache.get(playerUuid);
        if (mute != null && mute.isExpired()) {
            muteCache.remove(playerUuid);
            return null;
        }
        return mute;
    }

    /**
     * Load mute status into cache when player joins.
     */
    public void loadMuteCache(UUID playerUuid) {
        getActiveMute(playerUuid).thenAccept(mute -> {
            if (mute != null && !mute.isExpired()) {
                muteCache.put(playerUuid, mute);
            } else {
                muteCache.remove(playerUuid);
            }
        });
    }

    /**
     * Clear mute cache when player leaves.
     */
    public void clearMuteCache(UUID playerUuid) {
        muteCache.remove(playerUuid);
    }

    public CompletableFuture<Boolean> ban(Player target, Player staff, String reason, long duration) {
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName();
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        PunishmentType type = duration == -1 ? PunishmentType.BAN : PunishmentType.TEMP_BAN;
        Punishment punishment = new Punishment(targetUuid, targetName, staffUuid, staffName, type, reason, duration);

        return plugin.getDatabaseManager().savePunishment(punishment).thenApply(saved -> {
            if (saved != null) {
                // Apply ban
                String banMessage = formatBanMessage(saved);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Date expiry = duration == -1 ? null : new Date(System.currentTimeMillis() + duration);
                    Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, expiry, staffName);
                    target.kickPlayer(banMessage);
                });

                // Broadcast
                broadcastPunishment(saved);
                
                // Send to Discord
                sendDiscordNotification(saved);
                
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> banOffline(OfflinePlayer target, Player staff, String reason, long duration) {
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString();
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        PunishmentType type = duration == -1 ? PunishmentType.BAN : PunishmentType.TEMP_BAN;
        Punishment punishment = new Punishment(targetUuid, targetName, staffUuid, staffName, type, reason, duration);

        return plugin.getDatabaseManager().savePunishment(punishment).thenApply(saved -> {
            if (saved != null) {
                // Apply ban
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Date expiry = duration == -1 ? null : new Date(System.currentTimeMillis() + duration);
                    Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, expiry, staffName);
                });

                // Broadcast
                broadcastPunishment(saved);
                
                // Send to Discord
                sendDiscordNotification(saved);
                
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> unban(String playerName, Player staff) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        return plugin.getDatabaseManager().unban(target.getUniqueId()).thenApply(success -> {
            if (success) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);
                });
            }
            return success;
        });
    }

    public CompletableFuture<Boolean> mute(Player target, Player staff, String reason, long duration) {
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName();
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        PunishmentType type = duration == -1 ? PunishmentType.MUTE : PunishmentType.TEMP_MUTE;
        Punishment punishment = new Punishment(targetUuid, targetName, staffUuid, staffName, type, reason, duration);

        return plugin.getDatabaseManager().savePunishment(punishment).thenApply(saved -> {
            if (saved != null) {
                // Update mute cache
                muteCache.put(targetUuid, saved);
                
                // Notify player
                Bukkit.getScheduler().runTask(plugin, () -> {
                    target.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("mute.player-notify")));
                    target.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("mute.player-reason")
                            .replace("{reason}", reason)));
                    target.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("mute.player-duration")
                            .replace("{duration}", TimeUtil.formatDuration(duration))));
                });

                // Broadcast
                broadcastPunishment(saved);
                
                // Send to Discord
                sendDiscordNotification(saved);
                
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> muteOffline(OfflinePlayer target, Player staff, String reason, long duration) {
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString();
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        PunishmentType type = duration == -1 ? PunishmentType.MUTE : PunishmentType.TEMP_MUTE;
        Punishment punishment = new Punishment(targetUuid, targetName, staffUuid, staffName, type, reason, duration);

        return plugin.getDatabaseManager().savePunishment(punishment).thenApply(saved -> {
            if (saved != null) {
                // Update mute cache if player comes online later
                muteCache.put(targetUuid, saved);
                
                // Broadcast
                broadcastPunishment(saved);
                
                // Send to Discord
                sendDiscordNotification(saved);
                
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> unmute(String playerName, Player staff) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID targetUuid = target.getUniqueId();
        
        // Clear mute cache
        muteCache.remove(targetUuid);
        
        return plugin.getDatabaseManager().unmute(targetUuid);
    }

    public CompletableFuture<Boolean> kick(Player target, Player staff, String reason) {
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName();
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        Punishment punishment = new Punishment(targetUuid, targetName, staffUuid, staffName, 
            PunishmentType.KICK, reason, 0);

        return plugin.getDatabaseManager().savePunishment(punishment).thenApply(saved -> {
            if (saved != null) {
                // Kick player
                String kickMessage = formatKickMessage(saved);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    target.kickPlayer(kickMessage);
                });

                // Broadcast
                broadcastPunishment(saved);
                
                // Send to Discord
                sendDiscordNotification(saved);
                
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> warn(Player target, Player staff, String reason) {
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName();
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        Punishment punishment = new Punishment(targetUuid, targetName, staffUuid, staffName, 
            PunishmentType.WARN, reason, -1);

        return plugin.getDatabaseManager().savePunishment(punishment).thenCompose(saved -> {
            if (saved != null) {
                return plugin.getDatabaseManager().getWarningCount(targetUuid).thenApply(count -> {
                    // Notify player
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        target.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("warn.player-notify")));
                        target.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("warn.player-reason")
                                .replace("{reason}", reason)));
                        target.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage("warn.player-count")
                                .replace("{count}", String.valueOf(count))));
                    });

                    // Broadcast
                    broadcastPunishment(saved);
                    
                    // Send to Discord
                    sendDiscordNotification(saved);
                    
                    // Check warning threshold
                    int threshold = plugin.getConfig().getInt("punishments.warnings.threshold", 3);
                    if (count >= threshold) {
                        handleWarningThreshold(target, staff, count);
                    }
                    
                    return true;
                });
            }
            return CompletableFuture.completedFuture(false);
        });
    }

    private void handleWarningThreshold(Player target, Player staff, int count) {
        String action = plugin.getConfig().getString("punishments.warnings.action", "TEMPBAN");
        String duration = plugin.getConfig().getString("punishments.warnings.duration", "1d");
        
        // Broadcast threshold reached
        Bukkit.getScheduler().runTask(plugin, () -> {
            String message = plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("warn.threshold-reached")
                    .replace("{player}", target.getName())
                    .replace("{threshold}", String.valueOf(count)));
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("staffsystem.staff")) {
                    player.sendMessage(message);
                }
            }
        });

        if (action.equalsIgnoreCase("TEMPBAN")) {
            long durationMs = TimeUtil.parseDuration(duration);
            ban(target, staff, "Too many warnings", durationMs);
        } else if (action.equalsIgnoreCase("KICK")) {
            kick(target, staff, "Too many warnings");
        }
    }

    public CompletableFuture<Boolean> isBanned(UUID playerUuid) {
        return plugin.getDatabaseManager().getActiveBan(playerUuid)
            .thenApply(punishment -> punishment != null);
    }

    public CompletableFuture<Boolean> isMuted(UUID playerUuid) {
        return plugin.getDatabaseManager().getActiveMute(playerUuid)
            .thenApply(punishment -> punishment != null);
    }

    public CompletableFuture<Punishment> getActiveBan(UUID playerUuid) {
        return plugin.getDatabaseManager().getActiveBan(playerUuid);
    }

    public CompletableFuture<Punishment> getActiveMute(UUID playerUuid) {
        return plugin.getDatabaseManager().getActiveMute(playerUuid);
    }

    private void broadcastPunishment(Punishment punishment) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String messageKey = switch (punishment.getType()) {
                case BAN, TEMP_BAN -> "ban.broadcast";
                case MUTE, TEMP_MUTE -> "mute.broadcast";
                case KICK -> "kick.broadcast";
                case WARN -> "warn.broadcast";
            };

            String message = plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage(messageKey)
                    .replace("{player}", punishment.getPlayerName())
                    .replace("{staff}", punishment.getStaffName()));

            String reasonMessage = plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage(messageKey.replace("broadcast", "broadcast-reason"))
                    .replace("{reason}", punishment.getReason()));

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("staffsystem.staff")) {
                    player.sendMessage(message);
                    player.sendMessage(reasonMessage);
                    
                    if (!punishment.isPermanent() && punishment.getDuration() > 0) {
                        String durationMessage = plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage(messageKey.replace("broadcast", "broadcast-duration"))
                                .replace("{duration}", TimeUtil.formatDuration(punishment.getDuration())));
                        player.sendMessage(durationMessage);
                    }
                }
            }
        });
    }

    private String formatBanMessage(Punishment punishment) {
        String template = plugin.getMessageUtil().getMessage("ban.screen");
        String appealUrl = plugin.getConfig().getString("punishments.appeals.url", "N/A");
        
        return plugin.getMessageUtil().color(template
            .replace("{reason}", punishment.getReason())
            .replace("{staff}", punishment.getStaffName())
            .replace("{duration}", TimeUtil.formatDuration(punishment.getDuration()))
            .replace("{expires}", punishment.isPermanent() ? "Never" : 
                TimeUtil.formatDate(punishment.getExpiration()))
            .replace("{appeal_url}", appealUrl));
    }

    private String formatKickMessage(Punishment punishment) {
        String template = plugin.getMessageUtil().getMessage("kick.screen");
        
        return plugin.getMessageUtil().color(template
            .replace("{reason}", punishment.getReason())
            .replace("{staff}", punishment.getStaffName()));
    }

    private void sendDiscordNotification(Punishment punishment) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) {
            return;
        }

        String notificationKey = switch (punishment.getType()) {
            case BAN, TEMP_BAN -> "discord.notifications.bans";
            case MUTE, TEMP_MUTE -> "discord.notifications.mutes";
            case KICK -> "discord.notifications.kicks";
            case WARN -> "discord.notifications.warns";
        };

        if (!plugin.getConfig().getBoolean(notificationKey, false)) {
            return;
        }

        // Send to web API if enabled
        if (plugin.getWebApiManager() != null) {
            plugin.getWebApiManager().sendPunishmentNotification(punishment);
        }
    }
}
