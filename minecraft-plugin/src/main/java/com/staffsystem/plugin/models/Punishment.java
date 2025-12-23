package com.staffsystem.plugin.models;

import java.util.UUID;

public class Punishment {
    
    public enum PunishmentType {
        BAN,
        TEMP_BAN,
        MUTE,
        TEMP_MUTE,
        KICK,
        WARN
    }
    
    private int id;
    private UUID playerUuid;
    private String playerName;
    private UUID staffUuid;
    private String staffName;
    private PunishmentType type;
    private String reason;
    private long timestamp;
    private long duration; // -1 for permanent
    private long expiration; // -1 for permanent
    private boolean active;
    private String server;
    
    public Punishment() {
    }
    
    public Punishment(UUID playerUuid, String playerName, UUID staffUuid, String staffName,
                      PunishmentType type, String reason, long duration) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
        this.duration = duration;
        this.expiration = duration == -1 ? -1 : timestamp + duration;
        this.active = true;
        this.server = "main";
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public UUID getStaffUuid() {
        return staffUuid;
    }
    
    public void setStaffUuid(UUID staffUuid) {
        this.staffUuid = staffUuid;
    }
    
    public String getStaffName() {
        return staffName;
    }
    
    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }
    
    public PunishmentType getType() {
        return type;
    }
    
    public void setType(PunishmentType type) {
        this.type = type;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public long getExpiration() {
        return expiration;
    }
    
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getServer() {
        return server;
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public boolean isPermanent() {
        return duration == -1;
    }
    
    public boolean isExpired() {
        if (isPermanent()) {
            return false;
        }
        return System.currentTimeMillis() > expiration;
    }
    
    public long getRemainingTime() {
        if (isPermanent()) {
            return -1;
        }
        long remaining = expiration - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
