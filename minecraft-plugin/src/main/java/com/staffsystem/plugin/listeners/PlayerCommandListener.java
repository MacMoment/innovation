package com.staffsystem.plugin.listeners;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerCommandListener implements Listener {

    private final StaffSystemPlugin plugin;

    public PlayerCommandListener(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].toLowerCase();
        
        // Check if player is frozen
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            if (!plugin.getFreezeManager().canUseCommand(player, command)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("freeze.command-blocked")));
            }
        }
    }
}
