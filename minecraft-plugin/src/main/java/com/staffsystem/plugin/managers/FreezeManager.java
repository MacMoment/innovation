package com.staffsystem.plugin.managers;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.utils.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreezeManager {

    private final StaffSystemPlugin plugin;
    private final Map<UUID, Location> frozenPlayers;
    private final Map<UUID, UUID> frozenBy; // Player UUID -> Staff UUID
    private final Map<UUID, BukkitRunnable> reminderTasks;

    public FreezeManager(StaffSystemPlugin plugin) {
        this.plugin = plugin;
        this.frozenPlayers = new HashMap<>();
        this.frozenBy = new HashMap<>();
        this.reminderTasks = new HashMap<>();
    }

    public boolean freeze(Player target, Player staff) {
        UUID targetUuid = target.getUniqueId();
        
        if (isFrozen(targetUuid)) {
            return false;
        }

        // Store freeze location
        frozenPlayers.put(targetUuid, target.getLocation());
        frozenBy.put(targetUuid, staff.getUniqueId());

        // Notify player
        String message = plugin.getMessageUtil().getMessage("freeze.player-frozen");
        target.sendMessage(plugin.getMessageUtil().color(message));

        // Start reminder task
        startReminderTask(target);

        // Send to Discord
        if (plugin.getConfig().getBoolean("discord.notifications.freezes", false) && 
            plugin.getWebApiManager() != null) {
            plugin.getWebApiManager().sendFreezeNotification(target.getName(), staff.getName(), true);
        }

        return true;
    }

    public boolean unfreeze(Player target, Player staff) {
        UUID targetUuid = target.getUniqueId();
        
        if (!isFrozen(targetUuid)) {
            return false;
        }

        // Remove from frozen list
        frozenPlayers.remove(targetUuid);
        frozenBy.remove(targetUuid);

        // Stop reminder task
        stopReminderTask(targetUuid);

        // Notify player
        String message = plugin.getMessageUtil().getMessage("freeze.player-unfrozen");
        target.sendMessage(plugin.getMessageUtil().color(message));

        // Send to Discord
        if (plugin.getConfig().getBoolean("discord.notifications.freezes", false) && 
            plugin.getWebApiManager() != null) {
            plugin.getWebApiManager().sendFreezeNotification(target.getName(), staff.getName(), false);
        }

        return true;
    }

    public boolean toggleFreeze(Player target, Player staff) {
        if (isFrozen(target.getUniqueId())) {
            return unfreeze(target, staff);
        } else {
            return freeze(target, staff);
        }
    }

    public boolean isFrozen(UUID playerUuid) {
        return frozenPlayers.containsKey(playerUuid);
    }

    public Location getFreezeLocation(UUID playerUuid) {
        return frozenPlayers.get(playerUuid);
    }

    public UUID getFrozenByStaff(UUID playerUuid) {
        return frozenBy.get(playerUuid);
    }

    public void handleLogout(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        if (!isFrozen(playerUuid)) {
            return;
        }

        // Remove from frozen list
        frozenPlayers.remove(playerUuid);
        UUID staffUuid = frozenBy.remove(playerUuid);
        stopReminderTask(playerUuid);

        // Check if logout ban is enabled
        if (plugin.getConfig().getBoolean("freeze.logout-ban.enabled", true)) {
            String reason = plugin.getConfig().getString("freeze.logout-ban.reason", "Logged out while frozen");
            String durationStr = plugin.getConfig().getString("freeze.logout-ban.duration", "7d");
            long duration = TimeUtil.parseDuration(durationStr);

            // Get staff player
            Player staff = staffUuid != null ? Bukkit.getPlayer(staffUuid) : null;
            if (staff == null) {
                // Use console as staff
                staff = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("staffsystem.admin"))
                    .findFirst()
                    .orElse(null);
            }

            if (staff != null) {
                plugin.getPunishmentManager().banOffline(player, staff, reason, duration);
            }

            // Notify staff
            String message = plugin.getMessageUtil().color(
                plugin.getMessageUtil().getMessage("freeze.logout-ban")
                    .replace("{player}", player.getName()));
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("staffsystem.staff")) {
                    p.sendMessage(message);
                }
            }
        }
    }

    public void unfreezeAll() {
        for (UUID uuid : frozenPlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String message = plugin.getMessageUtil().getMessage("freeze.player-unfrozen");
                player.sendMessage(plugin.getMessageUtil().color(message));
            }
            stopReminderTask(uuid);
        }
        frozenPlayers.clear();
        frozenBy.clear();
    }

    public boolean canUseCommand(Player player, String command) {
        if (!isFrozen(player.getUniqueId())) {
            return true;
        }

        if (!plugin.getConfig().getBoolean("freeze.block-commands", true)) {
            return true;
        }

        // Check allowed commands
        for (String allowedCmd : plugin.getConfig().getStringList("freeze.allowed-commands")) {
            if (command.toLowerCase().startsWith(allowedCmd.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private void startReminderTask(Player target) {
        UUID targetUuid = target.getUniqueId();
        int interval = plugin.getConfig().getInt("freeze.message-interval", 5) * 20; // Convert to ticks

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(targetUuid);
                if (player == null || !isFrozen(targetUuid)) {
                    cancel();
                    return;
                }

                String message = plugin.getMessageUtil().getMessage("freeze.reminder");
                player.sendMessage(plugin.getMessageUtil().color(message));
            }
        };

        task.runTaskTimer(plugin, interval, interval);
        reminderTasks.put(targetUuid, task);
    }

    private void stopReminderTask(UUID playerUuid) {
        BukkitRunnable task = reminderTasks.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    public Map<UUID, Location> getFrozenPlayers() {
        return new HashMap<>(frozenPlayers);
    }
}
