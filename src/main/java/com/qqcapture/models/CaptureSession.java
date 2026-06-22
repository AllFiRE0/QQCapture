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
    private BukkitRunnable startDelayTask;
    private BukkitRunnable endDelayTask;
    
    // ===== КЭШ ЗАВЕРШЕННЫХ СЕССИЙ =====
    private static final Map<String, SessionSnapshot> completedSessions = new ConcurrentHashMap<>();
    private static final long SNAPSHOT_LIFETIME = 60000; // 60 секунд
    
    // ===== ВНУТРЕННИЙ КЛАСС ДЛЯ ХРАНЕНИЯ ДАННЫХ =====
    public static class SessionSnapshot {
        private final String templateName;
        private final int totalPoints;
        private final int targetPoints;
        private final Map<UUID, Integer> contributions;
        private final Map<UUID, String> playerNames;
        private final long endTime;
        private final String sessionId;
        
        public SessionSnapshot(CaptureSession session) {
            this.sessionId = session.sessionId;
            this.templateName = session.template.getName();
            this.totalPoints = session.currentPoints;
            this.targetPoints = session.targetPoints;
            this.endTime = System.currentTimeMillis();
            this.contributions = new HashMap<>();
            this.playerNames = new HashMap<>();
            
            for (Map.Entry<UUID, PlayerData> entry : session.players.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerData data = entry.getValue();
                this.contributions.put(uuid, data.getContribution());
                this.playerNames.put(uuid, data.getPlayerName());
            }
        }
        
        public boolean isValid() {
            return System.currentTimeMillis() - endTime < SNAPSHOT_LIFETIME;
        }
        
        public String getTemplateName() { return templateName; }
        public int getTotalPoints() { return totalPoints; }
        public int getTargetPoints() { return targetPoints; }
        public Map<UUID, Integer> getContributions() { return contributions; }
        public Map<UUID, String> getPlayerNames() { return playerNames; }
        public long getEndTime() { return endTime; }
        public String getSessionId() { return sessionId; }
    }
    
    // ===== МЕТОД ДЛЯ ПОЛУЧЕНИЯ СНЕПШОТА ПО ИМЕНИ ШАБЛОНА =====
    public static SessionSnapshot getCompletedSession(String templateName) {
        for (SessionSnapshot snapshot : completedSessions.values()) {
            if (snapshot.isValid() && snapshot.templateName.equalsIgnoreCase(templateName)) {
                return snapshot;
            }
        }
        return null;
    }
    
    public static List<SessionSnapshot> getAllCompletedSessions() {
        List<SessionSnapshot> valid = new ArrayList<>();
        for (SessionSnapshot snapshot : completedSessions.values()) {
            if (snapshot.isValid()) {
                valid.add(snapshot);
            }
        }
        return valid;
    }
    
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
        
        startBossBarWithDelay();
        startCaptureTask();
        startDurationTask();
        executeStartCommands();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInZone(player.getLocation(), template.getPos1(), template.getPos2())) {
                if (!players.containsKey(player.getUniqueId())) {
                    addPlayer(player);
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("Player " + player.getName() + " already in zone, added to session");
                    }
                }
            }
        }
        
        if (!template.getRegionName().isEmpty()) {
            QQCapture.getInstance().getRegionManager().setupRegion(this);
        }
    }
    
    private void startBossBarWithDelay() {
        int startDelay = template.getStartDelay();
        if (startDelay <= 0) {
            startBossBarTask();
            return;
        }
        
        startDelayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!stopped && !complete) {
                    startBossBarTask();
                }
            }
        };
        startDelayTask.runTaskLater(QQCapture.getInstance(), startDelay * 20L);
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
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Capture task tick: " + tickCounter + "/" + template.getTickCapture());
                }
            
                if (stopped || complete) {
                    this.cancel();
                    return;
                }
            
                tickCounter++;
                if (tickCounter >= template.getTickCapture()) {
                    tickCounter = 0;
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("Running processCaptureTick!");
                    }
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
                    complete = true;
                    List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    onComplete(allPlayers);
                }
            }
        };
        durationTask.runTaskTimer(QQCapture.getInstance(), 0L, 20L);
    }
    
    private void executeStartCommands() {
        List<String> commands = template.getStartCommands();
        if (commands != null && !commands.isEmpty()) {
            plugin.getLogger().info("Executing " + commands.size() + " start commands");
            for (String cmd : commands) {
                plugin.getLogger().info("  Start command: " + cmd);
            }
            List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            QQCapture.getInstance().getCommandManager().executeStartCommands(this, allPlayers);
        } else {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("No start commands found");
            }
        }
    }
    
    private void processCaptureTick() {
        List<Player> playersInZone = getPlayersInZone();

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("processCaptureTick: " + playersInZone.size() + " players in zone");
        }
    
        if (playersInZone.size() < template.getMinPlayers()) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Not enough players: " + playersInZone.size() + " < " + template.getMinPlayers());
            }
            return;
        }
        
        int maxPlayers = template.getMaxPlayers();
        List<Player> activePlayers = playersInZone;
        if (maxPlayers > 0 && playersInZone.size() > maxPlayers) {
            activePlayers = new ArrayList<>(playersInZone.subList(0, maxPlayers));
        }
        
        int pointsToAdd = 0;
        Map<UUID, Integer> playerContributions = new HashMap<>();
        
        for (Player player : activePlayers) {
            if (!QQCapture.getInstance().getConditionManager().checkPlayerConditions(player, template)) {
                continue;
            }
            
            if (!QQCapture.getInstance().getConditionManager().checkAllPlayersConditions(activePlayers, template)) {
                continue;
            }
            
            double playerMultiplier = template.getMultiplier();
            int contribution = 0;
            if (playerMultiplier > 0) {
                contribution = (int) (template.getNeedAmount() * playerMultiplier);
                pointsToAdd += contribution;
            }
            
            PlayerData data = players.computeIfAbsent(player.getUniqueId(), 
                k -> new PlayerData(player));
            data.addContribution(contribution);
            playerContributions.put(player.getUniqueId(), contribution);
        }
        
        if (template.getTeamMultiplier() > 0 && !activePlayers.isEmpty()) {
            double teamMultiplier = template.getTeamMultiplier();
            String teamType = template.getTeamMultiplierType();
            
            if ("individual".equalsIgnoreCase(teamType)) {
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
        }
        
        currentPoints = Math.min(currentPoints + pointsToAdd, targetPoints);
        
        List<String> tickCommands = template.getTickCommands();
        if (tickCommands != null && !tickCommands.isEmpty()) {
            plugin.getLogger().info("Executing " + tickCommands.size() + " tick commands");
            for (String cmd : tickCommands) {
                plugin.getLogger().info("  Tick command: " + cmd);
            }
            QQCapture.getInstance().getCommandManager().executeTickCommands(this, activePlayers);
        }
        
        if (currentPoints >= targetPoints) {
            plugin.getLogger().info("COMPLETED! Current: " + currentPoints + ", Target: " + targetPoints);
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
        if (!template.getRegionName().isEmpty()) {
            return QQCapture.getInstance().getRegionManager().isPlayerInRegion(loc, template.getRegionName());
        }
        
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
        QQCapture.getInstance().getBossBarManager().updateBossBar(this);
    }
    
    private void onComplete(List<Player> playersInZone) {
        // ===== СОХРАНЯЕМ СНЕПШОТ =====
        SessionSnapshot snapshot = new SessionSnapshot(this);
        completedSessions.put(sessionId, snapshot);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Session snapshot saved: " + sessionId + " (" + snapshot.getContributions().size() + " players)");
        }
        
        // Удаляем через 60 секунд
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            completedSessions.remove(sessionId);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Session snapshot removed: " + sessionId);
            }
        }, 1200L); // 60 секунд * 20 тиков
        
        // ===== ВЫПОЛНЯЕМ END КОМАНДЫ =====
        List<String> endCommands = template.getEndCommands();
        if (endCommands != null && !endCommands.isEmpty()) {
            QQCapture.getInstance().getCommandManager().executeEndCommands(this, playersInZone);
        }
        
        if (!silent) {
            String message = QQCapture.getInstance().getLanguageManager().getMessage("session-ended")
                .replace("%template%", template.getName());
            Bukkit.broadcastMessage(ColorUtils.colorize(message));
        }
        
        int endDelay = template.getEndDelay();
        if (endDelay > 0) {
            endDelayTask = new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        QQCapture.getInstance().getBossBarManager().hideBossBar(player, CaptureSession.this);
                    }
                    QQCapture.getInstance().getSessionManager().stopSession(sessionId);
                }
            };
            endDelayTask.runTaskLater(QQCapture.getInstance(), endDelay * 20L);
        } else {
            Bukkit.getScheduler().runTaskLater(QQCapture.getInstance(), 
                () -> QQCapture.getInstance().getSessionManager().stopSession(sessionId), 
                template.getEndDelay() * 20L);
        }
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
        if (startDelayTask != null) {
            startDelayTask.cancel();
        }
        if (endDelayTask != null) {
            endDelayTask.cancel();
        }
    }
    
    public void update() {
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
