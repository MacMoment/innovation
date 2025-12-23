package com.staffsystem.plugin.utils;

import com.staffsystem.plugin.StaffSystemPlugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private final StaffSystemPlugin plugin;
    private FileConfiguration messagesConfig;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageUtil(StaffSystemPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "Message not found: " + path;
        }
        return getPrefix() + message;
    }

    public String getMessageNoPrefix(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "Message not found: " + path;
        }
        return message;
    }

    public String getPrefix() {
        return messagesConfig.getString("prefix", "&8[&6StaffSystem&8] ");
    }

    public String color(String message) {
        if (message == null) {
            return "";
        }
        
        // Handle hex colors (&#RRGGBB format)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + group).toString());
        }
        matcher.appendTail(buffer);
        
        // Handle standard color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String stripColor(String message) {
        return ChatColor.stripColor(color(message));
    }
}
