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
    private final QQCapture plugin;
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
    private BukkitRunnable durationTask;
    
    public CaptureSession(String sessionId, Template template, int targetPoints, boolean silent, Player starter) {
        this.plugin = QQCapture.getInstance();
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
        
        // Start duration task if maxDuration is set
        startDurationTask();
        
        // Execute start commands
        executeStartCommands();
        
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
    
    private void startDurationTask() {
        int maxDuration = template.getMaxDuration();
        if (maxDuration <= 0) {
            return;
        }
        
        durationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped || complete) {
                    this.cancel();
                    return;
                }
                
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                if (elapsedSeconds >= maxDuration) {
                    // Force complete session
                    complete = true;
                    List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    onComplete(allPlayers);
                }
            }
        };
        durationTask.runTaskTimer(QQCapture.getInstance(), 0L, 20L); // Проверяем каждую секунду
    }
    
    private void executeStartCommands() {
        List<String> commands = template.getStartCommands();
        if (commands != null && !commands.isEmpty()) {
            List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            QQCapture.getInstance().getCommandManager().executeCommands(this, allPlayers);
        }
    }
    
    private void processCaptureTick() {
        // Get players in zone
        List<Player> playersInZone = getPlayersInZone();

        // Debug
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("processCaptureTick: " + playersInZone.size() + " players in zone");
        }
    
        // Check minimum players
        if (playersInZone.size() < template.getMinPlayers()) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Not enough players: " + playersInZone.size() + " < " + template.getMinPlayers());
            }
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
        Map<UUID, Integer> playerContributions = new HashMap<>();
        
        for (Player player : activePlayers) {
            // Check individual conditions
            if (!QQCapture.getInstance().getConditionManager().checkPlayerConditions(player, template)) {
                continue;
            }
            
            // Check all players conditions
            if (!QQCapture.getInstance().getConditionManager().checkAllPlayersConditions(activePlayers, template)) {
                continue;
            }
            
            // Calculate points with multiplier
            double playerMultiplier = template.getMultiplier();
            int contribution = 0;
            if (playerMultiplier > 0) {
                contribution = (int) (template.getNeedAmount() * playerMultiplier);
                pointsToAdd += contribution;
            }
            
            // Add player to session data
            PlayerData data = players.computeIfAbsent(player.getUniqueId(), 
                k -> new PlayerData(player));
            data.addContribution(contribution);
            playerContributions.put(player.getUniqueId(), contribution);
        }
        
        // Apply team multiplier
        if (template.getTeamMultiplier() > 0 && !activePlayers.isEmpty()) {
            double teamMultiplier = template.getTeamMultiplier();
            String teamType = template.getTeamMultiplierType();
            
            if ("individual".equalsIgnoreCase(teamType)) {
                // Individual team multiplier
                for (Player player : activePlayers) {
                    PlayerData data = players.get(player.getUniqueId());
                    if (data != null) {
                        int contribution = playerContributions.getOrDefault(player.getUniqueId(), 0);
                        int bonus = (int) (contribution * teamMultiplier);
                        data.addContribution(bonus);
                        pointsToAdd += bonus;
                    }
                }
            } else if ("shared".equalsIgnoreCase(teamType)) {
                // Shared team multiplier
                int totalPoints = playerContributions.values().stream().mapToInt(Integer::intValue).sum();
                int bonus = (int) (totalPoints * teamMultiplier);
                int bonusPerPlayer = activePlayers.isEmpty() ? 0 : bonus / activePlayers.size();
                
                for (Player player : activePlayers) {
                    PlayerData data = players.get(player.getUniqueId());
                    if (data != null) {
                        data.addContribution(bonusPerPlayer);
                    }
                }
                pointsToAdd += bonus;
            }
            // Если "disabled" - ничего не делаем
        }
        
        // Update current points
        currentPoints = Math.min(currentPoints + pointsToAdd, targetPoints);
        
        // Execute tick commands
        List<String> tickCommands = template.getTickCommands();
        if (tickCommands != null && !tickCommands.isEmpty()) {
            QQCapture.getInstance().getCommandManager().executeCommands(this, activePlayers);
        }
        
        // Check if completed
        if (currentPoints >= targetPoints) {
            complete = true;
            onComplete(activePlayers);
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
        // Execute end commands
        List<String> endCommands = template.getEndCommands();
        if (endCommands != null && !endCommands.isEmpty()) {
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
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Adding player " + player.getName() + " to session data for " + sessionId);
            }
            players.put(player.getUniqueId(), new PlayerData(player));
            if (template.isBossBarEnabled()) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Showing boss bar for player " + player.getName());
                }
                QQCapture.getInstance().getBossBarManager().showBossBar(player, this);
            }
        } else {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + " already in session " + sessionId);
            }
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
        if (durationTask != null) {
            durationTask.cancel();
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
