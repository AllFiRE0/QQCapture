package com.qqcapture.models;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamData {
    private final String teamId;
    private final List<UUID> members;
    private final String teamHash;
    private int totalContribution;
    private boolean active;
    
    public TeamData(String teamId, String teamHash) {
        this.teamId = teamId;
        this.teamHash = teamHash;
        this.members = new ArrayList<>();
        this.totalContribution = 0;
        this.active = true;
    }
    
    public void addMember(Player player) {
        if (!members.contains(player.getUniqueId())) {
            members.add(player.getUniqueId());
        }
    }
    
    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
        if (members.isEmpty()) {
            active = false;
        }
    }
    
    public void addContribution(int amount) {
        this.totalContribution += amount;
    }
    
    public boolean containsPlayer(Player player) {
        return members.contains(player.getUniqueId());
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    // Getters
    public String getTeamId() { return teamId; }
    public List<UUID> getMembers() { return members; }
    public String getTeamHash() { return teamHash; }
    public int getTotalContribution() { return totalContribution; }
    public boolean isActive() { return active; }
}
