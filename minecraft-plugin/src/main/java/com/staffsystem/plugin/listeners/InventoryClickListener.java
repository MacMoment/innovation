package com.staffsystem.plugin.listeners;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;
import com.staffsystem.plugin.utils.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class InventoryClickListener implements Listener {

    private final StaffSystemPlugin plugin;

    public InventoryClickListener(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String guiAction = plugin.getGuiManager().getGuiAction(player.getUniqueId());
        if (guiAction == null) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Handle filler glass panes
        if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        // Handle close button
        if (clickedItem.getType() == Material.BARRIER && 
            clickedItem.hasItemMeta() && 
            clickedItem.getItemMeta().getDisplayName().contains("Close")) {
            player.closeInventory();
            return;
        }

        // Handle back button
        if (clickedItem.getType() == Material.ARROW && 
            clickedItem.hasItemMeta() && 
            clickedItem.getItemMeta().getDisplayName().contains("Back")) {
            handleBackButton(player, guiAction);
            return;
        }

        // Route to appropriate handler
        if (guiAction.equals("STAFF_MAIN")) {
            handleStaffMainClick(player, clickedItem);
        } else if (guiAction.startsWith("ONLINE_PLAYERS_")) {
            handleOnlinePlayersClick(player, clickedItem, guiAction);
        } else if (guiAction.equals("PUNISHMENT_MENU")) {
            handlePunishmentMenuClick(player, clickedItem);
        } else if (guiAction.equals("HISTORY_MENU")) {
            // History is view-only
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Clear GUI action on close
        plugin.getGuiManager().clearGuiAction(player.getUniqueId());
    }

    private void handleBackButton(Player player, String currentAction) {
        if (currentAction.startsWith("ONLINE_PLAYERS_")) {
            plugin.getGuiManager().openStaffGui(player);
        } else if (currentAction.equals("PUNISHMENT_MENU")) {
            plugin.getGuiManager().openOnlinePlayersGui(player, "PUNISH");
        } else if (currentAction.equals("HISTORY_MENU")) {
            OfflinePlayer target = plugin.getGuiManager().getSelectedTarget(player.getUniqueId());
            if (target != null) {
                plugin.getGuiManager().openPunishmentGui(player, target);
            } else {
                plugin.getGuiManager().openStaffGui(player);
            }
        } else {
            plugin.getGuiManager().openStaffGui(player);
        }
    }

    private void handleStaffMainClick(Player player, ItemStack clickedItem) {
        Material type = clickedItem.getType();
        
        switch (type) {
            case GOLDEN_SWORD -> {
                // Toggle staff mode
                plugin.getStaffModeManager().toggleStaffMode(player);
                plugin.getGuiManager().openStaffGui(player);
            }
            case PLAYER_HEAD -> {
                // Online players
                plugin.getGuiManager().openOnlinePlayersGui(player, "PUNISH");
            }
            case BARRIER -> {
                // Punishments - go to online players for punishment
                plugin.getGuiManager().openOnlinePlayersGui(player, "PUNISH");
            }
            case ICE -> {
                // Frozen players - go to online players with freeze action
                plugin.getGuiManager().openOnlinePlayersGui(player, "FREEZE");
            }
            case PAPER -> {
                // Reports (placeholder)
                player.sendMessage(plugin.getMessageUtil().color("&7Reports feature coming soon!"));
            }
            case WRITABLE_BOOK -> {
                // Staff chat toggle
                plugin.getStaffModeManager().toggleStaffChat(player);
                plugin.getGuiManager().openStaffGui(player);
            }
            case BOOK -> {
                // Statistics (placeholder)
                player.sendMessage(plugin.getMessageUtil().color("&7Statistics feature coming soon!"));
            }
            case COMPARATOR -> {
                // Settings (placeholder)
                player.sendMessage(plugin.getMessageUtil().color("&7Settings feature coming soon!"));
            }
        }
    }

    private void handleOnlinePlayersClick(Player player, ItemStack clickedItem, String guiAction) {
        if (clickedItem.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return;
        }

        OfflinePlayer target = skullMeta.getOwningPlayer();
        if (target == null) {
            return;
        }

        String action = guiAction.replace("ONLINE_PLAYERS_", "");

        switch (action) {
            case "PUNISH", "VIEW" -> {
                plugin.getGuiManager().openPunishmentGui(player, target);
            }
            case "FREEZE" -> {
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        boolean wasFrozen = plugin.getFreezeManager().isFrozen(target.getUniqueId());
                        plugin.getFreezeManager().toggleFreeze(targetPlayer, player);
                        
                        String messageKey = wasFrozen ? "freeze.unfrozen" : "freeze.frozen";
                        player.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage(messageKey)
                                .replace("{player}", target.getName())));
                        
                        // Refresh GUI
                        plugin.getGuiManager().openOnlinePlayersGui(player, "FREEZE");
                    }
                }
            }
            case "TELEPORT" -> {
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        player.teleport(targetPlayer.getLocation());
                        player.sendMessage(plugin.getMessageUtil().color(
                            "&aTeleported to &e" + target.getName()));
                        player.closeInventory();
                    }
                }
            }
            case "PLAYER_INFO" -> {
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        showPlayerInfo(player, targetPlayer);
                        player.closeInventory();
                    }
                }
            }
        }
    }

    private void handlePunishmentMenuClick(Player player, ItemStack clickedItem) {
        OfflinePlayer target = plugin.getGuiManager().getSelectedTarget(player.getUniqueId());
        if (target == null) {
            player.closeInventory();
            return;
        }

        Material type = clickedItem.getType();
        String displayName = clickedItem.hasItemMeta() ? clickedItem.getItemMeta().getDisplayName() : "";

        switch (type) {
            case BARRIER -> {
                // Permanent ban
                if (displayName.contains("Permanent Ban")) {
                    executePunishment(player, target, "ban", -1);
                }
            }
            case RED_CONCRETE -> {
                // Temp bans
                if (displayName.contains("1 Hour")) {
                    executePunishment(player, target, "ban", TimeUtil.parseDuration("1h"));
                } else if (displayName.contains("1 Day")) {
                    executePunishment(player, target, "ban", TimeUtil.parseDuration("1d"));
                } else if (displayName.contains("7 Days")) {
                    executePunishment(player, target, "ban", TimeUtil.parseDuration("7d"));
                } else if (displayName.contains("30 Days")) {
                    executePunishment(player, target, "ban", TimeUtil.parseDuration("30d"));
                }
            }
            case PAPER -> {
                // Permanent mute
                if (displayName.contains("Permanent Mute")) {
                    executePunishment(player, target, "mute", -1);
                }
            }
            case ORANGE_CONCRETE -> {
                // Temp mutes
                if (displayName.contains("1 Hour")) {
                    executePunishment(player, target, "mute", TimeUtil.parseDuration("1h"));
                } else if (displayName.contains("1 Day")) {
                    executePunishment(player, target, "mute", TimeUtil.parseDuration("1d"));
                } else if (displayName.contains("7 Days")) {
                    executePunishment(player, target, "mute", TimeUtil.parseDuration("7d"));
                }
            }
            case LEATHER_BOOTS -> {
                // Kick
                executePunishment(player, target, "kick", 0);
            }
            case BOOK -> {
                // Warn
                executePunishment(player, target, "warn", 0);
            }
            case ICE -> {
                // Freeze/Unfreeze
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        boolean wasFrozen = plugin.getFreezeManager().isFrozen(target.getUniqueId());
                        plugin.getFreezeManager().toggleFreeze(targetPlayer, player);
                        
                        String messageKey = wasFrozen ? "freeze.unfrozen" : "freeze.frozen";
                        player.sendMessage(plugin.getMessageUtil().color(
                            plugin.getMessageUtil().getMessage(messageKey)
                                .replace("{player}", target.getName())));
                        
                        // Refresh GUI
                        plugin.getGuiManager().openPunishmentGui(player, target);
                    }
                }
            }
            case WRITABLE_BOOK -> {
                // View history
                plugin.getDatabaseManager().getPunishmentHistory(target.getUniqueId())
                    .thenAccept(punishments -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getGuiManager().openHistoryGui(player, target, punishments);
                        });
                    });
            }
        }
    }

    private void executePunishment(Player staff, OfflinePlayer target, String type, long duration) {
        String reason = plugin.getConfig().getString("punishments.default-reasons." + type, "Punished by staff");
        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        switch (type) {
            case "ban" -> {
                if (target.isOnline()) {
                    plugin.getPunishmentManager().ban(target.getPlayer(), staff, reason, duration);
                } else {
                    plugin.getPunishmentManager().banOffline(target, staff, reason, duration);
                }
                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("ban.success")
                        .replace("{player}", targetName)));
            }
            case "mute" -> {
                if (target.isOnline()) {
                    plugin.getPunishmentManager().mute(target.getPlayer(), staff, reason, duration);
                } else {
                    plugin.getPunishmentManager().muteOffline(target, staff, reason, duration);
                }
                staff.sendMessage(plugin.getMessageUtil().color(
                    plugin.getMessageUtil().getMessage("mute.success")
                        .replace("{player}", targetName)));
            }
            case "kick" -> {
                if (target.isOnline()) {
                    plugin.getPunishmentManager().kick(target.getPlayer(), staff, reason);
                    staff.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("kick.success")
                            .replace("{player}", targetName)));
                } else {
                    staff.sendMessage(plugin.getMessageUtil().color("&cPlayer is not online!"));
                }
            }
            case "warn" -> {
                if (target.isOnline()) {
                    plugin.getPunishmentManager().warn(target.getPlayer(), staff, reason);
                    staff.sendMessage(plugin.getMessageUtil().color(
                        plugin.getMessageUtil().getMessage("warn.success")
                            .replace("{player}", targetName)));
                } else {
                    staff.sendMessage(plugin.getMessageUtil().color("&cPlayer is not online!"));
                }
            }
        }

        staff.closeInventory();
    }

    private void showPlayerInfo(Player viewer, Player target) {
        viewer.sendMessage(plugin.getMessageUtil().color("&8&m----------------------------------------"));
        viewer.sendMessage(plugin.getMessageUtil().color("&6&lPlayer Info: &e" + target.getName()));
        viewer.sendMessage(plugin.getMessageUtil().color(""));
        viewer.sendMessage(plugin.getMessageUtil().color("&7UUID: &f" + target.getUniqueId()));
        viewer.sendMessage(plugin.getMessageUtil().color("&7Health: &c" + String.format("%.1f", target.getHealth()) + 
            "/" + String.format("%.1f", target.getMaxHealth())));
        viewer.sendMessage(plugin.getMessageUtil().color("&7Food: &e" + target.getFoodLevel() + "/20"));
        viewer.sendMessage(plugin.getMessageUtil().color("&7Gamemode: &f" + target.getGameMode().name()));
        viewer.sendMessage(plugin.getMessageUtil().color("&7World: &f" + target.getWorld().getName()));
        viewer.sendMessage(plugin.getMessageUtil().color("&7Location: &f" + 
            String.format("%.0f, %.0f, %.0f", target.getLocation().getX(), 
                target.getLocation().getY(), target.getLocation().getZ())));
        viewer.sendMessage(plugin.getMessageUtil().color("&7Ping: &f" + target.getPing() + "ms"));
        viewer.sendMessage(plugin.getMessageUtil().color("&7IP: &f" + 
            (target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : "N/A")));
        
        if (plugin.getFreezeManager().isFrozen(target.getUniqueId())) {
            viewer.sendMessage(plugin.getMessageUtil().color("&7Status: &b&lFROZEN"));
        }
        
        viewer.sendMessage(plugin.getMessageUtil().color("&8&m----------------------------------------"));
    }
}
