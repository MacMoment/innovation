package com.staffsystem.plugin.listeners;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final StaffSystemPlugin plugin;

    public PlayerQuitListener(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle frozen player logout
        plugin.getFreezeManager().handleLogout(player);
        
        // Cleanup staff mode if active
        if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId())) {
            plugin.getStaffModeManager().disableStaffMode(player);
        }
        
        // Clear GUI data
        plugin.getGuiManager().clearGuiAction(player.getUniqueId());
        plugin.getGuiManager().clearSelectedTarget(player.getUniqueId());
        
        // Clear mute cache
        plugin.getPunishmentManager().clearMuteCache(player.getUniqueId());
    }
}
