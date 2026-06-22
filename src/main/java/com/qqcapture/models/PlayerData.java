package com.qqcapture.models;

import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerData {
    private final UUID playerId;
    private final String playerName;
    private int contribution;
    private boolean inZone;
    private long lastUpdate;
    private int ticksInZone;
    private long joinTime;
    
    public PlayerData(Player player) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.contribution = 0;
        this.inZone = false;
        this.lastUpdate = System.currentTimeMillis();
        this.ticksInZone = 0;
        this.joinTime = System.currentTimeMillis();
    }
    
    public void addContribution(int amount) {
        this.contribution += amount;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public void incrementTicksInZone() {
        this.ticksInZone++;
    }
    
    public void resetTicksInZone() {
        this.ticksInZone = 0;
    }
    
    // Getters and Setters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getContribution() { return contribution; }
    public void setContribution(int contribution) { this.contribution = contribution; }
    public boolean isInZone() { return inZone; }
    public void setInZone(boolean inZone) { this.inZone = inZone; }
    public long getLastUpdate() { return lastUpdate; }
    public int getTicksInZone() { return ticksInZone; }
    public long getJoinTime() { return joinTime; }
}
