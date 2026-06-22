package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import com.qqcapture.models.Template;
import com.qqcapture.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManager {
    private final QQCapture plugin;
    private final Pattern checkPattern;
    private final Pattern delayPattern;
    private final Pattern randomPattern;
    private final Pattern soundPattern;
    private final Random random = new Random();
    
    public CommandManager(QQCapture plugin) {
        this.plugin = plugin;
        this.checkPattern = Pattern.compile("^check:(.+?)! (.+)$");
        this.delayPattern = Pattern.compile("^delay! (\\d+)$");
        this.randomPattern = Pattern.compile("^random:(\\d+)! (.+)$");
        this.soundPattern = Pattern.compile("^sound! (.+?) (\\d+\\.?\\d*) (\\d+\\.?\\d*)$");
    }
    
    public void executeStartCommands(CaptureSession session, List<Player> players) {
        List<String> commands = session.getTemplate().getStartCommands();
        if (commands == null || commands.isEmpty()) return;
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Executing " + commands.size() + " start commands");
        }
        processCommandsWithDelay(session, players, commands, 0);
    }
    
    public void executeTickCommands(CaptureSession session, List<Player> players) {
        List<String> commands = session.getTemplate().getTickCommands();
        if (commands == null || commands.isEmpty()) return;
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Executing " + commands.size() + " tick commands");
        }
        processCommandsWithDelay(session, players, commands, 0);
    }
    
    public void executeEndCommands(CaptureSession session, List<Player> players) {
        List<String> commands = session.getTemplate().getEndCommands();
        if (commands == null || commands.isEmpty()) return;
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Executing " + commands.size() + " end commands");
        }
        processCommandsWithDelay(session, players, commands, 0);
    }
    
    private void processCommandsWithDelay(CaptureSession session, List<Player> players, List<String> commands, int index) {
        if (index >= commands.size()) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("All commands processed");
            }
            return;
        }
        
        String command = commands.get(index);
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Processing command [" + index + "/" + commands.size() + "]: " + command);
        }
        
        if (command.startsWith("delay:")) {
            handleDelayAction(session, players, command, commands, index);
            return;
        }
        
        Matcher delayMatcher = delayPattern.matcher(command);
        if (delayMatcher.matches()) {
            int delay = Integer.parseInt(delayMatcher.group(1));
            Bukkit.getScheduler().runTaskLater(plugin, 
                () -> processCommandsWithDelay(session, players, commands, index + 1), 
                delay * 20L);
            return;
        }
        
        executeCommand(session, players, command);
        Bukkit.getScheduler().runTask(plugin, 
            () -> processCommandsWithDelay(session, players, commands, index + 1));
    }
    
    private void handleDelayAction(CaptureSession session, List<Player> players, String command, List<String> commands, int index) {
        try {
            String[] parts = command.substring(6).split("!", 2);
            String delayStr = parts[0].trim();
            int delayTicks = Integer.parseInt(delayStr);
            
            if (parts.length > 1) {
                String delayedAction = parts[1].trim();
                Bukkit.getScheduler().runTaskLater(plugin, 
                    () -> {
                        executeCommand(session, players, delayedAction);
                        processCommandsWithDelay(session, players, commands, index + 1);
                    }, 
                    delayTicks);
            } else {
                processCommandsWithDelay(session, players, commands, index + 1);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid delay format: " + command);
            processCommandsWithDelay(session, players, commands, index + 1);
        }
    }
    
    private void executeCommand(CaptureSession session, List<Player> players, String command) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Executing command: " + command);
        }
        
        // random:
        Matcher randomMatcher = randomPattern.matcher(command);
        if (randomMatcher.matches()) {
            int chance = Integer.parseInt(randomMatcher.group(1));
            if (random.nextInt(100) >= chance) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Random chance failed: " + chance + "%");
                }
                return;
            }
            command = randomMatcher.group(2);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Random chance passed: " + chance + "%");
            }
        }
        
        // Если команда начинается с check: - обрабатываем отдельно
        if (command.startsWith("check:")) {
            handleCheckCommand(session, players, command);
            return;
        }
        
        // Обычная команда
        executeSingleCommand(session, players, command, null);
    }
    
    private void handleCheckCommand(CaptureSession session, List<Player> players, String command) {
        List<String> conditions = new ArrayList<>();
        String remaining = command;
        String actualCommand = "";
        
        // Парсим все check: условия
        while (remaining.startsWith("check:")) {
            int nextCheck = remaining.indexOf(" check:", 1);
            int firstExclamation = remaining.indexOf("! ");
            
            if (nextCheck > 0 && (firstExclamation == -1 || nextCheck < firstExclamation)) {
                String condition = remaining.substring(6, nextCheck).trim();
                conditions.add(condition);
                remaining = remaining.substring(nextCheck);
            } else if (firstExclamation > 0) {
                String condition = remaining.substring(6, firstExclamation).trim();
                conditions.add(condition);
                actualCommand = remaining.substring(firstExclamation + 2).trim();
                break;
            } else {
                plugin.getLogger().warning("Invalid check format: " + command);
                return;
            }
        }
        
        if (conditions.isEmpty() || actualCommand.isEmpty()) {
            plugin.getLogger().warning("Empty conditions or command in check: " + command);
            return;
        }
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Check conditions: " + conditions.size() + ", command: " + actualCommand);
        }
        
        for (Player player : players) {
            boolean allConditionsMet = true;
            for (String condition : conditions) {
                if (!checkCondition(player, condition)) {
                    allConditionsMet = false;
                    break;
                }
            }
            if (allConditionsMet) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("All conditions met for player " + player.getName());
                }
                // Выполняем команду (она может содержать другие !)
                executeSingleCommand(session, players, actualCommand, player);
            }
        }
    }
    
    private boolean checkCondition(Player player, String condition) {
        return plugin.getConditionManager().evaluateCondition(player, condition);
    }
    
    private void executeSingleCommand(CaptureSession session, List<Player> players, String command, Player targetPlayer) {
        // Ищем "! " или просто "!"
        int exclamationIndex = command.indexOf("! ");
        if (exclamationIndex == -1) {
            exclamationIndex = command.indexOf("!");
        }
        if (exclamationIndex == -1) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("Invalid command format (missing '!'): " + command);
            }
            return;
        }
        
        String prefix = command.substring(0, exclamationIndex).trim();
        String content = command.substring(exclamationIndex + 1).trim();
        
        // Заменяем плейсхолдеры
        content = plugin.getPlaceholderManager().parsePlaceholders(targetPlayer, content);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Executing - prefix: " + prefix + ", content: " + content);
        }
        
        // Проверка префиксов с параметрами
        if (prefix.startsWith("title:")) {
            handleTitleCommandWithParams(targetPlayer != null ? targetPlayer : players.get(0), content, prefix.substring(6));
            return;
        }
        if (prefix.startsWith("actionbar:")) {
            handleActionbarCommandWithParams(targetPlayer != null ? targetPlayer : players.get(0), content, prefix.substring(10));
            return;
        }
        
        switch (prefix) {
            case "asConsole":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content);
                break;
                
            case "asPlayer":
                if (targetPlayer != null) {
                    targetPlayer.performCommand(content);
                } else {
                    for (Player player : players) {
                        player.performCommand(content);
                    }
                }
                break;
                
            case "message":
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ColorUtils.colorize(content));
                } else {
                    for (Player player : players) {
                        player.sendMessage(ColorUtils.colorize(content));
                    }
                }
                break;
                
            case "gMessage":
                Bukkit.broadcastMessage(ColorUtils.colorize(content));
                break;
                
            case "sound":
                executeSound(targetPlayer != null ? targetPlayer : players.get(0), content);
                break;
                
            case "gSound":
                executeSoundAll(content);
                break;
                
            case "actionbar":
                handleActionbarCommand(targetPlayer != null ? targetPlayer : players.get(0), content);
                break;
                
            case "gActionbar":
                handleActionbarAllCommand(content);
                break;
                
            case "title":
                handleTitleCommand(targetPlayer != null ? targetPlayer : players.get(0), content);
                break;
                
            default:
                plugin.getLogger().warning("Unknown command prefix: " + prefix);
        }
    }
    
    // ===== ACTIONBAR С ПАРАМЕТРАМИ =====
    
    private void handleActionbarCommandWithParams(Player player, String content, String params) {
        int duration = 60;
        try {
            duration = Integer.parseInt(params);
        } catch (NumberFormatException ignored) {}
        sendActionBar(player, content, duration);
    }
    
    private void handleActionbarCommand(Player player, String content) {
        int duration = 60;
        String message = content;
        
        if (content.contains(":")) {
            String[] parts = content.split(":", 2);
            try {
                duration = Integer.parseInt(parts[0]);
                message = parts[1];
            } catch (NumberFormatException ignored) {}
        }
        
        sendActionBar(player, ColorUtils.colorize(message), duration);
    }
    
    private void handleActionbarAllCommand(String content) {
        int duration = 60;
        String message = content;
        
        if (content.contains(":")) {
            String[] parts = content.split(":", 2);
            try {
                duration = Integer.parseInt(parts[0]);
                message = parts[1];
            } catch (NumberFormatException ignored) {}
        }
        
        sendActionBarAll(ColorUtils.colorize(message), duration);
    }
    
    // ===== TITLE С ПАРАМЕТРАМИ =====
    
    private void handleTitleCommandWithParams(Player player, String content, String params) {
        String[] times = params.split(":");
        if (times.length == 3) {
            try {
                int fadeIn = Integer.parseInt(times[0]);
                int stay = Integer.parseInt(times[1]);
                int fadeOut = Integer.parseInt(times[2]);
                String[] titleParts = content.split("\n", 2);
                sendTitle(player, 
                    titleParts[0],
                    titleParts.length > 1 ? titleParts[1] : "",
                    fadeIn, stay, fadeOut);
                return;
            } catch (NumberFormatException ignored) {}
        }
        
        // Fallback
        String[] titleParts = content.split("\n", 2);
        sendTitle(player, titleParts[0], titleParts.length > 1 ? titleParts[1] : "", 20, 40, 20);
    }
    
    // ===== TITLE =====
    
    private void handleTitleCommand(Player player, String content) {
        int fadeIn = 20;
        int stay = 40;
        int fadeOut = 20;
        String title = "";
        String subtitle = "";
        
        if (content.matches("\\d+:\\d+:\\d+! .+")) {
            String[] parts3 = content.split("! ", 2);
            String[] times = parts3[0].split(":");
            try {
                fadeIn = Integer.parseInt(times[0]);
                stay = Integer.parseInt(times[1]);
                fadeOut = Integer.parseInt(times[2]);
                String[] titleContent = parts3[1].split("\n", 2);
                title = titleContent[0];
                subtitle = titleContent.length > 1 ? titleContent[1] : "";
                sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
                return;
            } catch (NumberFormatException ignored) {}
        }
        
        String[] titleParts = content.split("\n", 2);
        title = titleParts[0];
        subtitle = titleParts.length > 1 ? titleParts[1] : "";
        sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }
    
    // ===== ВСПОМОГАТЕЛЬНЫЕ =====
    
    private void executeSound(Player player, String soundString) {
        try {
            String[] parts = soundString.split(" ");
            if (parts.length < 3) {
                plugin.getLogger().warning("Invalid sound format: " + soundString);
                return;
            }
            Sound sound = Sound.valueOf(parts[0]);
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound: " + soundString);
        }
    }
    
    private void executeSoundAll(String soundString) {
        try {
            String[] parts = soundString.split(" ");
            if (parts.length < 3) {
                plugin.getLogger().warning("Invalid sound format: " + soundString);
                return;
            }
            Sound sound = Sound.valueOf(parts[0]);
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound: " + soundString);
        }
    }
    
    private void sendActionBar(Player player, String message, int ticks) {
        player.sendActionBar(message);
        
        int refreshTicks = 20;
        if (ticks > refreshTicks) {
            int repeats = ticks / refreshTicks;
            for (int i = 1; i <= repeats; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, 
                    () -> player.sendActionBar(message), 
                    index * refreshTicks);
            }
        }
    }
    
    private void sendActionBarAll(String message, int ticks) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(message);
        }
        
        int refreshTicks = 20;
        if (ticks > refreshTicks) {
            int repeats = ticks / refreshTicks;
            for (int i = 1; i <= repeats; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, 
                    () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendActionBar(message);
                        }
                    }, 
                    index * refreshTicks);
            }
        }
    }
    
    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(ColorUtils.colorize(title), ColorUtils.colorize(subtitle), fadeIn, stay, fadeOut);
    }
}
