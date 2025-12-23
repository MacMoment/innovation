package com.staffsystem.plugin;

import com.staffsystem.plugin.api.WebApiManager;
import com.staffsystem.plugin.commands.*;
import com.staffsystem.plugin.listeners.*;
import com.staffsystem.plugin.managers.*;
import com.staffsystem.plugin.utils.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class StaffSystemPlugin extends JavaPlugin {

    private static StaffSystemPlugin instance;
    
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private FreezeManager freezeManager;
    private StaffModeManager staffModeManager;
    private GuiManager guiManager;
    private WebApiManager webApiManager;
    private MessageUtil messageUtil;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Initialize message utility
        messageUtil = new MessageUtil(this);
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Log startup message
        getLogger().info("StaffSystem has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
        
        // Check for web integration
        if (getConfig().getBoolean("web-integration.enabled", false)) {
            getLogger().info("Web integration is enabled. API URL: " + 
                getConfig().getString("web-integration.api-url"));
        }
        
        // Check for Discord integration
        if (getConfig().getBoolean("discord.enabled", false)) {
            getLogger().info("Discord integration is enabled.");
        }
    }
    
    @Override
    public void onDisable() {
        // Save all data
        if (freezeManager != null) {
            freezeManager.unfreezeAll();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("StaffSystem has been disabled!");
    }
    
    private void initializeManagers() {
        // Database manager
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Punishment manager
        punishmentManager = new PunishmentManager(this);
        
        // Freeze manager
        freezeManager = new FreezeManager(this);
        
        // Staff mode manager
        staffModeManager = new StaffModeManager(this);
        
        // GUI manager
        guiManager = new GuiManager(this);
        
        // Web API manager
        if (getConfig().getBoolean("web-integration.enabled", false)) {
            webApiManager = new WebApiManager(this);
        }
    }
    
    private void registerCommands() {
        // Staff GUI command
        Objects.requireNonNull(getCommand("staff")).setExecutor(new StaffCommand(this));
        
        // Punishment commands
        BanCommand banCommand = new BanCommand(this);
        Objects.requireNonNull(getCommand("ban")).setExecutor(banCommand);
        Objects.requireNonNull(getCommand("tempban")).setExecutor(banCommand);
        Objects.requireNonNull(getCommand("unban")).setExecutor(banCommand);
        
        MuteCommand muteCommand = new MuteCommand(this);
        Objects.requireNonNull(getCommand("mute")).setExecutor(muteCommand);
        Objects.requireNonNull(getCommand("tempmute")).setExecutor(muteCommand);
        Objects.requireNonNull(getCommand("unmute")).setExecutor(muteCommand);
        
        Objects.requireNonNull(getCommand("kick")).setExecutor(new KickCommand(this));
        Objects.requireNonNull(getCommand("warn")).setExecutor(new WarnCommand(this));
        Objects.requireNonNull(getCommand("freeze")).setExecutor(new FreezeCommand(this));
        Objects.requireNonNull(getCommand("staffchat")).setExecutor(new StaffChatCommand(this));
        Objects.requireNonNull(getCommand("history")).setExecutor(new HistoryCommand(this));
        Objects.requireNonNull(getCommand("staffsystem")).setExecutor(new StaffSystemCommand(this));
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerCommandListener(this), this);
    }
    
    public void reload() {
        reloadConfig();
        messageUtil.reload();
        getLogger().info("Configuration reloaded!");
    }
    
    // Getters
    public static StaffSystemPlugin getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
    
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    public WebApiManager getWebApiManager() {
        return webApiManager;
    }
    
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
}
