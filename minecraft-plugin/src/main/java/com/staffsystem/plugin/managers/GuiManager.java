package com.staffsystem.plugin.managers;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;
import com.staffsystem.plugin.utils.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GuiManager {

    private final StaffSystemPlugin plugin;
    private final Map<UUID, String> playerGuiActions; // Stores what action GUI is for
    private final Map<UUID, OfflinePlayer> selectedTargets; // Stores selected target for punishment

    public GuiManager(StaffSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerGuiActions = new HashMap<>();
        this.selectedTargets = new HashMap<>();
    }

    public void openStaffGui(Player player) {
        String title = plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("gui.staff-panel"));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, filler);
        }

        // Staff Mode Toggle
        gui.setItem(10, createItem(Material.GOLDEN_SWORD, "&6&lStaff Mode",
            Arrays.asList("&7Click to toggle staff mode", "",
                "&7Current: " + (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId()) ? 
                    "&aEnabled" : "&cDisabled"))));

        // Online Players
        gui.setItem(12, createItem(Material.PLAYER_HEAD, "&a&lOnline Players",
            Arrays.asList("&7View and manage online players", "",
                "&7Online: &e" + Bukkit.getOnlinePlayers().size())));

        // Punishments
        gui.setItem(14, createItem(Material.BARRIER, "&c&lPunishments",
            Arrays.asList("&7Access punishment menu", "",
                "&7Ban, mute, kick, warn players")));

        // Frozen Players
        int frozenCount = plugin.getFreezeManager().getFrozenPlayers().size();
        gui.setItem(16, createItem(Material.ICE, "&b&lFrozen Players",
            Arrays.asList("&7View frozen players", "",
                "&7Currently frozen: &e" + frozenCount)));

        // Reports (placeholder)
        gui.setItem(28, createItem(Material.PAPER, "&e&lReports",
            Arrays.asList("&7View player reports", "",
                "&7Pending reports: &e0")));

        // Staff Chat Toggle
        boolean staffChatEnabled = plugin.getStaffModeManager().isStaffChatEnabled(player.getUniqueId());
        gui.setItem(30, createItem(Material.WRITABLE_BOOK, "&d&lStaff Chat",
            Arrays.asList("&7Toggle staff chat mode", "",
                "&7Current: " + (staffChatEnabled ? "&aEnabled" : "&cDisabled"))));

        // Statistics (placeholder)
        gui.setItem(32, createItem(Material.BOOK, "&9&lStatistics",
            Arrays.asList("&7View your staff statistics", "",
                "&7Coming soon...")));

        // Settings
        gui.setItem(34, createItem(Material.COMPARATOR, "&7&lSettings",
            Arrays.asList("&7Configure your preferences", "",
                "&7Coming soon...")));

        // Close button
        gui.setItem(49, createItem(Material.BARRIER, "&c&lClose",
            Collections.singletonList("&7Click to close this menu")));

        playerGuiActions.put(player.getUniqueId(), "STAFF_MAIN");
        player.openInventory(gui);
    }

    public void openOnlinePlayersGui(Player player, String action) {
        String title = plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("gui.online-players"));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, filler);
        }

        // Add player heads
        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 44) break;
            if (slot == 17 || slot == 18) slot = 19;
            if (slot == 26 || slot == 27) slot = 28;
            if (slot == 35 || slot == 36) slot = 37;

            ItemStack head = createPlayerHead(online);
            gui.setItem(slot, head);
            slot++;
        }

        // Back button
        gui.setItem(45, createItem(Material.ARROW, "&7&lBack",
            Collections.singletonList("&7Return to main menu")));

        // Close button
        gui.setItem(49, createItem(Material.BARRIER, "&c&lClose",
            Collections.singletonList("&7Click to close this menu")));

        playerGuiActions.put(player.getUniqueId(), "ONLINE_PLAYERS_" + action);
        player.openInventory(gui);
    }

    public void openPunishmentGui(Player staff, OfflinePlayer target) {
        String title = plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("gui.punishment-menu")
                .replace("{player}", target.getName() != null ? target.getName() : "Unknown"));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Store target
        selectedTargets.put(staff.getUniqueId(), target);

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, filler);
        }

        // Player info
        ItemStack playerHead = createPlayerHead(target);
        gui.setItem(4, playerHead);

        // Ban - Permanent
        gui.setItem(19, createItem(Material.BARRIER, "&4&lPermanent Ban",
            Arrays.asList("&7Permanently ban this player", "",
                "&cThis cannot be undone without /unban")));

        // Temp Ban options
        gui.setItem(20, createItem(Material.RED_CONCRETE, "&c&lBan 1 Hour",
            Arrays.asList("&7Ban for 1 hour", "")));
        gui.setItem(21, createItem(Material.RED_CONCRETE, "&c&lBan 1 Day",
            Arrays.asList("&7Ban for 1 day", "")));
        gui.setItem(22, createItem(Material.RED_CONCRETE, "&c&lBan 7 Days",
            Arrays.asList("&7Ban for 7 days", "")));
        gui.setItem(23, createItem(Material.RED_CONCRETE, "&c&lBan 30 Days",
            Arrays.asList("&7Ban for 30 days", "")));

        // Mute - Permanent
        gui.setItem(28, createItem(Material.PAPER, "&6&lPermanent Mute",
            Arrays.asList("&7Permanently mute this player", "")));

        // Temp Mute options
        gui.setItem(29, createItem(Material.ORANGE_CONCRETE, "&e&lMute 1 Hour",
            Arrays.asList("&7Mute for 1 hour", "")));
        gui.setItem(30, createItem(Material.ORANGE_CONCRETE, "&e&lMute 1 Day",
            Arrays.asList("&7Mute for 1 day", "")));
        gui.setItem(31, createItem(Material.ORANGE_CONCRETE, "&e&lMute 7 Days",
            Arrays.asList("&7Mute for 7 days", "")));

        // Kick
        gui.setItem(33, createItem(Material.LEATHER_BOOTS, "&c&lKick",
            Arrays.asList("&7Kick this player from the server", "")));

        // Warn
        gui.setItem(34, createItem(Material.BOOK, "&e&lWarn",
            Arrays.asList("&7Give this player a warning", "")));

        // Freeze (if online)
        if (target.isOnline()) {
            boolean isFrozen = plugin.getFreezeManager().isFrozen(target.getUniqueId());
            gui.setItem(37, createItem(Material.ICE, isFrozen ? "&a&lUnfreeze" : "&b&lFreeze",
                Arrays.asList(isFrozen ? "&7Unfreeze this player" : "&7Freeze this player", "")));
        }

        // View History
        gui.setItem(43, createItem(Material.WRITABLE_BOOK, "&9&lView History",
            Arrays.asList("&7View punishment history", "")));

        // Back button
        gui.setItem(45, createItem(Material.ARROW, "&7&lBack",
            Collections.singletonList("&7Return to previous menu")));

        // Close button
        gui.setItem(49, createItem(Material.BARRIER, "&c&lClose",
            Collections.singletonList("&7Click to close this menu")));

        playerGuiActions.put(staff.getUniqueId(), "PUNISHMENT_MENU");
        staff.openInventory(gui);
    }

    public void openHistoryGui(Player staff, OfflinePlayer target, List<Punishment> punishments) {
        String title = plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("gui.history-menu")
                .replace("{player}", target.getName() != null ? target.getName() : "Unknown"));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Store target
        selectedTargets.put(staff.getUniqueId(), target);

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, filler);
        }

        // Add punishments
        int slot = 10;
        for (Punishment punishment : punishments) {
            if (slot >= 44) break;
            if (slot == 17 || slot == 18) slot = 19;
            if (slot == 26 || slot == 27) slot = 28;
            if (slot == 35 || slot == 36) slot = 37;

            ItemStack item = createPunishmentItem(punishment);
            gui.setItem(slot, item);
            slot++;
        }

        if (punishments.isEmpty()) {
            gui.setItem(22, createItem(Material.EMERALD_BLOCK, "&a&lNo Punishments",
                Collections.singletonList("&7This player has no punishment history")));
        }

        // Back button
        gui.setItem(45, createItem(Material.ARROW, "&7&lBack",
            Collections.singletonList("&7Return to punishment menu")));

        // Close button
        gui.setItem(49, createItem(Material.BARRIER, "&c&lClose",
            Collections.singletonList("&7Click to close this menu")));

        playerGuiActions.put(staff.getUniqueId(), "HISTORY_MENU");
        staff.openInventory(gui);
    }

    private ItemStack createPunishmentItem(Punishment punishment) {
        Material material = switch (punishment.getType()) {
            case BAN, TEMP_BAN -> Material.RED_CONCRETE;
            case MUTE, TEMP_MUTE -> Material.ORANGE_CONCRETE;
            case KICK -> Material.YELLOW_CONCRETE;
            case WARN -> Material.LIME_CONCRETE;
        };

        String typeName = punishment.getType().name().replace("_", " ");
        String status = punishment.isActive() ? "&aActive" : "&cInactive";
        if (punishment.isExpired()) {
            status = "&7Expired";
        }

        List<String> lore = Arrays.asList(
            "&7Type: &f" + typeName,
            "&7Reason: &f" + punishment.getReason(),
            "&7By: &f" + punishment.getStaffName(),
            "&7Date: &f" + TimeUtil.formatDate(punishment.getTimestamp()),
            "&7Duration: &f" + TimeUtil.formatDuration(punishment.getDuration()),
            "&7Status: " + status
        );

        return createItem(material, "&e#" + punishment.getId() + " - " + typeName, lore);
    }

    private ItemStack createPlayerHead(OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(plugin.getMessageUtil().color("&e" + player.getName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMessageUtil().color("&7Click to select"));
            
            if (player.isOnline()) {
                Player online = player.getPlayer();
                if (online != null) {
                    lore.add("");
                    lore.add(plugin.getMessageUtil().color("&7Status: &aOnline"));
                    lore.add(plugin.getMessageUtil().color("&7Health: &c" + 
                        String.format("%.1f", online.getHealth()) + "/" + 
                        String.format("%.1f", online.getMaxHealth())));
                    lore.add(plugin.getMessageUtil().color("&7Gamemode: &f" + online.getGameMode().name()));
                    
                    if (plugin.getFreezeManager().isFrozen(online.getUniqueId())) {
                        lore.add(plugin.getMessageUtil().color("&b&lFROZEN"));
                    }
                }
            } else {
                lore.add("");
                lore.add(plugin.getMessageUtil().color("&7Status: &cOffline"));
            }
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessageUtil().color(name));
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(plugin.getMessageUtil().color(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getGuiAction(UUID uuid) {
        return playerGuiActions.get(uuid);
    }

    public void clearGuiAction(UUID uuid) {
        playerGuiActions.remove(uuid);
    }

    public OfflinePlayer getSelectedTarget(UUID uuid) {
        return selectedTargets.get(uuid);
    }

    public void clearSelectedTarget(UUID uuid) {
        selectedTargets.remove(uuid);
    }
}
