package com.staffsystem.plugin.models;

import java.util.UUID;

public class StaffMember {
    
    private UUID uuid;
    private String name;
    private String rank;
    private int tier;
    private boolean staffMode;
    private boolean vanished;
    private boolean staffChatEnabled;
    
    public StaffMember(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.rank = "Staff";
        this.tier = 1;
        this.staffMode = false;
        this.vanished = false;
        this.staffChatEnabled = false;
    }
    
    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRank() {
        return rank;
    }
    
    public void setRank(String rank) {
        this.rank = rank;
    }
    
    public int getTier() {
        return tier;
    }
    
    public void setTier(int tier) {
        this.tier = tier;
    }
    
    public boolean isStaffMode() {
        return staffMode;
    }
    
    public void setStaffMode(boolean staffMode) {
        this.staffMode = staffMode;
    }
    
    public boolean isVanished() {
        return vanished;
    }
    
    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }
    
    public boolean isStaffChatEnabled() {
        return staffChatEnabled;
    }
    
    public void setStaffChatEnabled(boolean staffChatEnabled) {
        this.staffChatEnabled = staffChatEnabled;
    }
}
