package com.qqcapture.models;

import com.qqcapture.QQCapture;
import com.qqcapture.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaptureSession {
    private final String sessionId;
    private final Template template;
    private final int targetPoints;
    private final boolean silent;
    private final Player starter;
    private final long startTime;
    private final Map<UUID, PlayerData> players;
    private int currentPoints;
    private boolean stopped;
    private boolean complete;
    private int tickCounter;
    private long lastCaptureTick;
    private BukkitRunnable bossBarTask;
    private BukkitRunnable captureTask;
    
    public CaptureSession(String sessionId, Template template, int targetPoints, boolean silent, Player starter) {
        this.sessionId = sessionId;
        this.template = template;
        this.targetPoints = targetPoints;
        this.silent = silent;
        this.starter = starter;
        this.startTime = System.currentTimeMillis();
        this.players = new ConcurrentHashMap<>();
        this.currentPoints = 0;
        this.stopped = false;
        this.complete = false;
        this.tickCounter = 0;
        this.lastCaptureTick = 0;
        
        // Start boss bar task
        startBossBarTask();
        
        // Start capture task
        startCaptureTask();
        
        // Initialize region if needed
        if (!template.getRegionName().isEmpty()) {
            QQCapture.getInstance().getRegionManager().setupRegion(this);
        }
    }
    
    private void startBossBarTask() {
        bossBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) {
                    this.cancel();
                    return;
                }
                updateBossBars();
            }
        };
        bossBarTask.runTaskTimer(QQCapture.getInstance(), 0L, template.getUpdateTicks());
    }
    
    private void startCaptureTask() {
        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped || complete) {
                    this.cancel();
                    return;
                }
                
                tickCounter++;
                if (tickCounter >= template.getTickCapture()) {
                    tickCounter = 0;
                    processCaptureTick();
                }
            }
        };
        captureTask.runTaskTimer(QQCapture.getInstance(), 0L, 1L);
    }
    
    private void processCaptureTick() {
        // Get players in zone
        List<Player> playersInZone = getPlayersInZone();
        
        // Check minimum players
        if (playersInZone.size() < template.getMinPlayers()) {
            return;
        }
        
        // Check maximum players
        int maxPlayers = template.getMaxPlayers();
        List<Player> activePlayers = playersInZone;
        if (maxPlayers > 0 && playersInZone.size() > maxPlayers) {
            activePlayers = new ArrayList<>(playersInZone.subList(0, maxPlayers));
        }
        
        // Calculate capture points
        int pointsToAdd = 0;
        for (Player player : playersInZone) {
            // Check individual conditions
            if (!QQCapture.getInstance().getConditionManager().checkPlayerConditions(player, template)) {
                continue;
            }
            
            // Check all players conditions
            if (!QQCapture.getInstance().getConditionManager().checkAllPlayersConditions(playersInZone, template)) {
                continue;
            }
            
            // Calculate points with multiplier
            double playerMultiplier = template.getMultiplier();
            if (playerMultiplier > 0) {
                pointsToAdd += (int) (template.getNeedAmount() * playerMultiplier);
            }
            
            // Add player to session data
            PlayerData data = players.computeIfAbsent(player.getUniqueId(), 
                k -> new PlayerData(player));
            data.addContribution((int) (template.getNeedAmount() * template.getMultiplier()));
        }
        
        // Apply team multiplier
        if (template.getTeamMultiplier() > 0 && !playersInZone.isEmpty()) {
            double teamMultiplier = template.getTeamMultiplier();
            if ("индивидуально".equalsIgnoreCase(template.getTeamMultiplierType())) {
                // Individual team multiplier
                for (Player player : playersInZone) {
                    PlayerData data = players.get(player.getUniqueId());
                    if (data != null) {
                        int bonus = (int) (data.getContribution() * teamMultiplier);
                        data.addContribution(bonus);
                        pointsToAdd += bonus;
                    }
                }
            } else {
                // Shared team multiplier
                int bonus = (int) (pointsToAdd * teamMultiplier);
                for (Player player : playersInZone) {
                    PlayerData data = players.get(player.getUniqueId());
                    if (data != null) {
                        data.addContribution(bonus / playersInZone.size());
                    }
                }
                pointsToAdd += bonus;
            }
        }
        
        // Update current points
        currentPoints = Math.min(currentPoints + pointsToAdd, targetPoints);
        
        // Execute commands if tick-command is true
        if (template.isTickCommand()) {
            QQCapture.getInstance().getCommandManager().executeCommands(this, playersInZone);
        }
        
        // Check if completed
        if (currentPoints >= targetPoints) {
            complete = true;
            onComplete(playersInZone);
        }
    }
    
    private List<Player> getPlayersInZone() {
        List<Player> result = new ArrayList<>();
        Location pos1 = template.getPos1();
        Location pos2 = template.getPos2();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            if (isInZone(loc, pos1, pos2)) {
                result.add(player);
            }
        }
        return result;
    }
    
    private boolean isInZone(Location loc, Location pos1, Location pos2) {
        // Check WorldGuard region first if defined
        if (!template.getRegionName().isEmpty()) {
            return QQCapture.getInstance().getRegionManager().isPlayerInRegion(loc, template.getRegionName());
        }
        
        // Check cuboid area
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    
    private void updateBossBars() {
        // Update boss bar for all online players
        QQCapture.getInstance().getBossBarManager().updateBossBar(this);
    }
    
    private void onComplete(List<Player> playersInZone) {
        // Execute completion commands
        if (!template.isTickCommand()) {
            QQCapture.getInstance().getCommandManager().executeCommands(this, playersInZone);
        }
        
        // Show completion message
        if (!silent) {
            String message = QQCapture.getInstance().getLanguageManager().getMessage("session-ended")
                .replace("%template%", template.getName());
            Bukkit.broadcastMessage(ColorUtils.colorize(message));
        }
        
        // Stop session after delay
        Bukkit.getScheduler().runTaskLater(QQCapture.getInstance(), 
            () -> QQCapture.getInstance().getSessionManager().stopSession(sessionId), 
            template.getEndDelay() * 20L);
    }
    
    public void addPlayer(Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            players.put(player.getUniqueId(), new PlayerData(player));
            QQCapture.getInstance().getBossBarManager().showBossBar(player, this);
        }
    }
    
    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        QQCapture.getInstance().getBossBarManager().hideBossBar(player, this);
    }
    
    public void stop() {
        stopped = true;
        if (bossBarTask != null) {
            bossBarTask.cancel();
        }
        if (captureTask != null) {
            captureTask.cancel();
        }
    }
    
    public void update() {
        // Update player positions and conditions
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.containsKey(player.getUniqueId())) {
                Location loc = player.getLocation();
                boolean inZone = isInZone(loc, template.getPos1(), template.getPos2());
                PlayerData data = players.get(player.getUniqueId());
                if (data != null) {
                    data.setInZone(inZone);
                }
            }
        }
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public Template getTemplate() { return template; }
    public int getTargetPoints() { return targetPoints; }
    public int getCurrentPoints() { return currentPoints; }
    public boolean isSilent() { return silent; }
    public Player getStarter() { return starter; }
    public long getStartTime() { return startTime; }
    public Map<UUID, PlayerData> getPlayers() { return players; }
    public boolean isStopped() { return stopped; }
    public boolean isComplete() { return complete; }
}
