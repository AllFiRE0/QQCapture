package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
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
    
    public CommandManager(QQCapture plugin) {
        this.plugin = plugin;
        this.checkPattern = Pattern.compile("^check:(.+?)! (.+)$");
        this.delayPattern = Pattern.compile("^delay! (\\d+)$");
        this.randomPattern = Pattern.compile("^random:(\\d+)! (.+)$");
        this.soundPattern = Pattern.compile("^sound! (.+?) (\\d+\\.?\\d*) (\\d+\\.?\\d*)$");
    }
    
    public void executeCommands(CaptureSession session, List<Player> players) {
        List<String> commands = session.getTemplate().getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        // Process commands with delay
        processCommandsWithDelay(session, players, commands, 0);
    }
    
    private void processCommandsWithDelay(CaptureSession session, List<Player> players, List<String> commands, int index) {
        if (index >= commands.size()) {
            return;
        }
        
        String command = commands.get(index);
        
        // Check for delay
        Matcher delayMatcher = delayPattern.matcher(command);
        if (delayMatcher.matches()) {
            int delay = Integer.parseInt(delayMatcher.group(1));
            Bukkit.getScheduler().runTaskLater(plugin, 
                () -> processCommandsWithDelay(session, players, commands, index + 1), 
                delay * 20L);
            return;
        }
        
        // Execute command
        executeCommand(session, players, command);
        
        // Continue to next command
        Bukkit.getScheduler().runTask(plugin, 
            () -> processCommandsWithDelay(session, players, commands, index + 1));
    }
    
    private void executeCommand(CaptureSession session, List<Player> players, String command) {
        // Check for random
        Matcher randomMatcher = randomPattern.matcher(command);
        if (randomMatcher.matches()) {
            int chance = Integer.parseInt(randomMatcher.group(1));
            if (new Random().nextInt(100) >= chance) {
                return;
            }
            command = randomMatcher.group(2);
        }
        
        // Check for condition
        Matcher checkMatcher = checkPattern.matcher(command);
        if (checkMatcher.matches()) {
            String condition = checkMatcher.group(1);
            String actualCommand = checkMatcher.group(2);
            
            // Check condition
            for (Player player : players) {
                if (checkCondition(player, condition)) {
                    executeSingleCommand(session, players, actualCommand, player);
                }
            }
            return;
        }
        
        // Execute for all players
        executeSingleCommand(session, players, command, null);
    }
    
    private void executeSingleCommand(CaptureSession session, List<Player> players, String command, Player targetPlayer) {
        // Parse command prefix
        String[] parts = command.split("! ", 2);
        if (parts.length < 2) {
            return;
        }
        
        String prefix = parts[0];
        String content = parts[1];
        
        // Replace placeholders
        content = plugin.getPlaceholderManager().parsePlaceholders(targetPlayer, content);
        
        switch (prefix) {
            case "asConsole":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content);
                break;
                
            case "asPlayer":
                if (targetPlayer != null) {
                    Bukkit.dispatchCommand(targetPlayer, content);
                } else {
                    for (Player player : players) {
                        Bukkit.dispatchCommand(player, content);
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
                if (content.contains(":")) {
                    String[] actionParts = content.split(":", 2);
                    try {
                        int ticks = Integer.parseInt(actionParts[0]);
                        String message = ColorUtils.colorize(actionParts[1]);
                        sendActionBar(targetPlayer != null ? targetPlayer : players.get(0), message, ticks);
                    } catch (NumberFormatException e) {
                        sendActionBar(targetPlayer != null ? targetPlayer : players.get(0), content, 60);
                    }
                } else {
                    sendActionBar(targetPlayer != null ? targetPlayer : players.get(0), content, 60);
                }
                break;
                
            case "gActionbar":
                if (content.contains(":")) {
                    String[] actionParts = content.split(":", 2);
                    try {
                        int ticks = Integer.parseInt(actionParts[0]);
                        String message = ColorUtils.colorize(actionParts[1]);
                        sendActionBarAll(message, ticks);
                    } catch (NumberFormatException e) {
                        sendActionBarAll(content, 60);
                    }
                } else {
                    sendActionBarAll(content, 60);
                }
                break;
                
            case "title":
                String[] titleParts = content.split("\n", 2);
                String title = titleParts[0];
                String subtitle = titleParts.length > 1 ? titleParts[1] : "";
                
                if (content.matches("\\d+:\\d+:\\d+! .+")) {
                    String[] parts3 = content.split("! ", 2);
                    String[] times = parts3[0].split(":");
                    int fadeIn = Integer.parseInt(times[0]);
                    int stay = Integer.parseInt(times[1]);
                    int fadeOut = Integer.parseInt(times[2]);
                    String[] titleContent = parts3[1].split("\n", 2);
                    sendTitle(targetPlayer != null ? targetPlayer : players.get(0), 
                        titleContent[0], titleContent.length > 1 ? titleContent[1] : "", 
                        fadeIn, stay, fadeOut);
                } else {
                    sendTitle(targetPlayer != null ? targetPlayer : players.get(0), 
                        title, subtitle, 20, 40, 20);
                }
                break;
                
            default:
                plugin.getLogger().warning("Unknown command prefix: " + prefix);
        }
    }
    
    private boolean checkCondition(Player player, String condition) {
        Template tempTemplate = new Template.Builder("temp")
            .condition(condition)
            .build();
        return plugin.getConditionManager().checkPlayerConditions(player, tempTemplate);
    }
    
    private void executeSound(Player player, String soundString) {
        Matcher matcher = soundPattern.matcher(soundString);
        if (matcher.matches()) {
            try {
                String soundName = matcher.group(1);
                float volume = Float.parseFloat(matcher.group(2));
                float pitch = Float.parseFloat(matcher.group(3));
                
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound format: " + soundString);
            }
        }
    }
    
    private void executeSoundAll(String soundString) {
        Matcher matcher = soundPattern.matcher(soundString);
        if (matcher.matches()) {
            try {
                String soundName = matcher.group(1);
                float volume = Float.parseFloat(matcher.group(2));
                float pitch = Float.parseFloat(matcher.group(3));
                
                Sound sound = Sound.valueOf(soundName);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound format: " + soundString);
            }
        }
    }
    
    private void sendActionBar(Player player, String message, int ticks) {
        player.sendActionBar(ColorUtils.colorize(message));
    }
    
    private void sendActionBarAll(String message, int ticks) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(ColorUtils.colorize(message));
        }
    }
    
    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(ColorUtils.colorize(title), ColorUtils.colorize(subtitle), fadeIn, stay, fadeOut);
    }
}
