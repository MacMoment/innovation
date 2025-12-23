package com.staffsystem.plugin.managers;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.StaffMember;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StaffModeManager {

    private final StaffSystemPlugin plugin;
    private final Map<UUID, StaffMember> staffMembers;
    private final Map<UUID, ItemStack[]> storedInventories;
    private final Map<UUID, ItemStack[]> storedArmor;
    private final Map<UUID, GameMode> storedGameModes;

    public StaffModeManager(StaffSystemPlugin plugin) {
        this.plugin = plugin;
        this.staffMembers = new HashMap<>();
        this.storedInventories = new HashMap<>();
        this.storedArmor = new HashMap<>();
        this.storedGameModes = new HashMap<>();
    }

    public StaffMember getStaffMember(UUID uuid) {
        return staffMembers.get(uuid);
    }

    public StaffMember getOrCreateStaffMember(Player player) {
        return staffMembers.computeIfAbsent(player.getUniqueId(), 
            uuid -> new StaffMember(uuid, player.getName()));
    }

    public boolean isInStaffMode(UUID uuid) {
        StaffMember member = staffMembers.get(uuid);
        return member != null && member.isStaffMode();
    }

    public boolean toggleStaffMode(Player player) {
        StaffMember member = getOrCreateStaffMember(player);
        
        if (member.isStaffMode()) {
            return disableStaffMode(player);
        } else {
            return enableStaffMode(player);
        }
    }

    public boolean enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        StaffMember member = getOrCreateStaffMember(player);
        
        if (member.isStaffMode()) {
            return false;
        }

        // Store inventory
        storedInventories.put(uuid, player.getInventory().getContents().clone());
        storedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        storedGameModes.put(uuid, player.getGameMode());

        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Give staff items
        giveStaffItems(player);

        // Set gamemode
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);

        member.setStaffMode(true);

        // Notify player
        player.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("staffmode.enabled")));
        player.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("staffmode.inventory-stored")));

        return true;
    }

    public boolean disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        StaffMember member = getOrCreateStaffMember(player);
        
        if (!member.isStaffMode()) {
            return false;
        }

        // Clear staff items
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Restore inventory
        if (storedInventories.containsKey(uuid)) {
            player.getInventory().setContents(storedInventories.remove(uuid));
        }
        if (storedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(storedArmor.remove(uuid));
        }
        if (storedGameModes.containsKey(uuid)) {
            player.setGameMode(storedGameModes.remove(uuid));
        }

        member.setStaffMode(false);
        
        // Reset fly if not in creative
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }

        // Notify player
        player.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("staffmode.disabled")));
        player.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage("staffmode.inventory-restored")));

        return true;
    }

    private void giveStaffItems(Player player) {
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("staff.items");
        if (itemsSection == null) {
            // Default items
            giveDefaultStaffItems(player);
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            String materialName = itemSection.getString("material", "STONE");
            int slot = itemSection.getInt("slot", 0);
            String name = itemSection.getString("name", "Item");
            String action = itemSection.getString("action", "NONE");

            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.STONE;
            }

            ItemStack item = createStaffItem(material, name, action);
            player.getInventory().setItem(slot, item);
        }
    }

    private void giveDefaultStaffItems(Player player) {
        player.getInventory().setItem(0, createStaffItem(Material.COMPASS, "&6Teleport Tool", "TELEPORT"));
        player.getInventory().setItem(1, createStaffItem(Material.BOOK, "&ePlayer Info", "PLAYER_INFO"));
        player.getInventory().setItem(2, createStaffItem(Material.ICE, "&bFreeze Player", "FREEZE"));
        player.getInventory().setItem(3, createStaffItem(Material.PAPER, "&cReport Menu", "REPORTS"));
        player.getInventory().setItem(4, createStaffItem(Material.CHEST, "&aOnline Players", "ONLINE_PLAYERS"));
        player.getInventory().setItem(8, createStaffItem(Material.BARRIER, "&4Exit Staff Mode", "EXIT_STAFF_MODE"));
    }

    private ItemStack createStaffItem(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessageUtil().color(name));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMessageUtil().color("&7Click to use"));
            lore.add(plugin.getMessageUtil().color("&8Action: " + action));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleStaffItemClick(Player player, ItemStack item, String action) {
        switch (action.toUpperCase()) {
            case "TELEPORT":
                // Open player selector for teleport
                plugin.getGuiManager().openOnlinePlayersGui(player, "TELEPORT");
                break;
            case "PLAYER_INFO":
                // Open player selector for info
                plugin.getGuiManager().openOnlinePlayersGui(player, "PLAYER_INFO");
                break;
            case "FREEZE":
                // Open player selector for freeze
                plugin.getGuiManager().openOnlinePlayersGui(player, "FREEZE");
                break;
            case "REPORTS":
                // Open reports menu
                player.sendMessage(plugin.getMessageUtil().color("&7Reports feature coming soon!"));
                break;
            case "ONLINE_PLAYERS":
                // Open online players menu
                plugin.getGuiManager().openOnlinePlayersGui(player, "VIEW");
                break;
            case "EXIT_STAFF_MODE":
                disableStaffMode(player);
                break;
            default:
                break;
        }
    }

    public boolean isStaffChatEnabled(UUID uuid) {
        StaffMember member = staffMembers.get(uuid);
        return member != null && member.isStaffChatEnabled();
    }

    public void toggleStaffChat(Player player) {
        StaffMember member = getOrCreateStaffMember(player);
        boolean newState = !member.isStaffChatEnabled();
        member.setStaffChatEnabled(newState);

        String messageKey = newState ? "staffchat.toggle-on" : "staffchat.toggle-off";
        player.sendMessage(plugin.getMessageUtil().color(
            plugin.getMessageUtil().getMessage(messageKey)));
    }

    public void sendStaffChat(Player sender, String message) {
        String format = plugin.getMessageUtil().getMessage("staffchat.format")
            .replace("{rank}", getStaffRank(sender))
            .replace("{player}", sender.getName())
            .replace("{message}", message);

        String coloredMessage = plugin.getMessageUtil().color(format);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("staffsystem.staffchat")) {
                player.sendMessage(coloredMessage);
            }
        }

        // Also log to console
        plugin.getLogger().info("[StaffChat] " + sender.getName() + ": " + message);
    }

    private String getStaffRank(Player player) {
        StaffMember member = staffMembers.get(player.getUniqueId());
        if (member != null) {
            return member.getRank();
        }
        return "Staff";
    }

    public void cleanupPlayer(UUID uuid) {
        staffMembers.remove(uuid);
        storedInventories.remove(uuid);
        storedArmor.remove(uuid);
        storedGameModes.remove(uuid);
    }
}
